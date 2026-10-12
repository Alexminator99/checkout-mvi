# Checkout MVI POC Implementation Plan

**Goal:** Rebuild the checkout prototype as a runnable, fully tested, multi-module Android POC that shows Betsson how plain MVI keeps scaling.

**Architecture:** Each screen runs one `Store`, a single loop that reduces actions in order: a pure reducer returns a `Transition` (new state, effects to run, one-off events). An effect handler turns each effect into one use case call and reports the result back as an action. A projector turns state into render-ready UI state. The checkout feature is split into `domain` (pure Kotlin), `data` (demo backend) and `presentation` modules. Shared build configuration lives in convention plugins.

**Tech Stack:** Kotlin 2.4.21, AGP 9.4.1 (built-in Kotlin), Gradle 9.6.0, KSP 2.3.12, Hilt 2.60.1, AndroidX Hilt 1.4.0, Jetpack Compose (BOM 2026.09.00), Navigation Compose 2.10.2 (type-safe routes), kotlinx-coroutines 1.11.0. Tests: JUnit 4, kotlinx-coroutines-test, Turbine 1.2.1, hand-written fakes.

**Spec:** `docs/specs/2026-10-12-checkout-mvi-poc-design.md`

## Global Constraints

- **Versions:** Gradle 9.6.0 (wrapper as committed), AGP 9.4.1, Kotlin 2.4.21, KSP 2.3.12, Hilt 2.60.1, AndroidX Hilt 1.4.0, Compose BOM 2026.09.00, Navigation Compose 2.10.2, Lifecycle 2.11.0, Activity Compose 1.13.0, kotlinx-coroutines 1.11.0, Turbine 1.2.1, androidx.annotation 1.11.0.
- **SDK and bytecode:** compileSdk 37, targetSdk 36, minSdk 26, JVM bytecode 17.
- **Kotlin plugin:** AGP 9 compiles Kotlin itself. Never apply `org.jetbrains.kotlin.android`.
- **Package root:** `com.example`.
  - Namespaces: `com.example.core.ui`, `com.example.feature.checkout.data`, `com.example.feature.checkout.presentation`, `com.example.app`.
  - Application id: `com.example.app`.
- **One top-level type per file.** A sealed hierarchy lives in one file with its subtypes. Extension functions for one concern share a file.
- **Reducers are pure:** no I/O, coroutines, clock, randomness or Android types. Ids are created by the ViewModel, never by a reducer.
- **Vocabulary is fixed:** `CheckoutState`, `CheckoutIntent`, `CheckoutAction`, `CheckoutEffect`, `CheckoutEvent`, `CheckoutUiState`.
- **Hilt wiring:**
  - Presentation and data internals are `internal`, with `@Inject constructor` (`@Inject internal constructor` when the class itself is public).
  - The ViewModel injects concrete classes; there are no `@Binds` for reducers, the effect handler or projectors.
  - `@Binds` exist only to bind data implementations to domain repository interfaces.
- **No hard-coded user-facing strings in UI code.** Copy and content descriptions come from `strings.xml`; formatting goes through `UiText` and `MoneyFormatter`.
- **Fakes only:** no mocking libraries. Fakes for domain repository interfaces live in `feature/checkout/domain/src/testFixtures`, one per file.
- **Commits** use `type(scope): description` and are made only with the repository owner's approval.
- **JDK:** commands assume a JDK 17+ on `JAVA_HOME`. Android Studio's bundled JBR works: `export JAVA_HOME="/Users/alexminator/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

## Review Focus

1. **Tapping Pay twice while the order is being placed.** Exactly one order is sent. *Pinned by Task 9, `OrderReducerTest` "a second tap while the order is being placed is ignored".*
2. **Removing the last item from the cart.** The empty-cart message shows, shipping is cleared, Pay is disabled, and nothing crashes. *Pinned by Task 9, `ContextReducerTest` "emptying the cart clears shipping and blocks paying", and Task 11, `CartSectionProjectorTest` "an empty cart is flagged".*
3. **Losing connectivity in the middle of checkout.** The banner shows and Pay is disabled; reconnecting enables Pay again. *Pinned by Task 11, `PlaceOrderButtonProjectorTest` "paying is disabled while offline", and Task 12, `CheckoutViewModelTest` "going offline shows the banner and blocks paying until reconnected".*
4. **A promo that stops qualifying after a cart change** (BIG50 applied, then the order drops under €100). The discount disappears and an inline error explains why. *Pinned by Task 9, `PromoReducerTest` "a promo that no longer qualifies after a cart change stops discounting".*
5. **Editing quantities while the order is being placed.** Edits are ignored and the buttons are disabled. *Pinned by Task 9, `CartReducerTest` "quantities cannot change while the order is being placed", and Task 11, `CartSectionProjectorTest` "lines cannot be edited while the order is being placed".*

## File Map

```
build-logic/                                  convention plugins (Task 1)
gradle/libs.versions.toml, gradle.properties, settings.gradle.kts, build.gradle.kts   (Task 1)
core/mvi/          Store, Reducer, Transition, EffectHandler, KeyedEffect             (Task 1)
core/domain/       Money, Outcome                                                     (Task 2)
feature/checkout/domain/
    model/         13 models                                                          (Task 3)
    error/         CheckoutError, CheckoutOutcome                                     (Task 3)
    rule/          ShippingPrice, ShippingAvailability, ShippingPreference, CartLimits (Task 3)
    repository/    9 repository interfaces                                            (Task 4)
    usecase/       7 use cases                                                        (Task 4)
    testFixtures/  fixture/CheckoutFixtures, fake/9 fakes                             (Tasks 3-4)
feature/checkout/data/
    remote/        CheckoutRemoteDataSource, CheckoutApiException, HttpStatus, DemoCheckoutRemoteDataSource, dto/10 DTOs  (Task 5)
    demo/          DemoCheckoutData                                                   (Task 5)
    error/         SafeCall                                                           (Task 5)
    mapper/        7 remote mappers (Task 5), CartMapper (Task 6)
    repository/    5 remote-backed repositories (Task 5); CartRepositoryImpl, DemoCustomerRepository, DemoFeatureFlagRepository (Task 6)
    local/         CartDataSource, CartLineRecord, InMemoryCartDataSource             (Task 6)
    network/       ConnectivityNetworkMonitor                                         (Task 6)
    di/            StoreCurrency, DataSourceModule, RepositoryModule                  (Task 6)
core/ui/           UiText, UiTextResolution, ObserveAsEvents, MoneyFormatter, LocaleMoneyFormatter, theme/ShopTheme, di/CoreUiModule (Task 7)
feature/checkout/presentation/
    contract/      state, actions, intents, effects, events, selectors                (Task 8)
    reducer/       CheckoutReducer, 8 slice reducers, 4 cross-slice steps, CheckoutTransition (Task 9)
    effect/        CheckoutEffectHandler                                              (Task 10)
    projector/     CheckoutProjector, ReadyCheckout, 7 section projectors, error text (Task 11)
    ui/model/      12 UI models                                                       (Task 11)
    CheckoutViewModel.kt                                                              (Task 12)
    ui/            CheckoutScreen, component/, confirmation/, preview/                (Task 13)
    navigation/    destinations + NavGraphBuilder extensions                          (Task 13)
app/               ShopApp, MainActivity, ShopNavHost, manifest, resources            (Task 14)
README.md                                                                             (Task 14)
```

---

### Task 1: Build foundation and the MVI core

Replaces the prototype's build with convention plugins on the current toolchain, deletes the prototype modules (later tasks recreate them; git history keeps them), and rebuilds `:core:mvi` with tests that run on the test scheduler.

**Files:**
- Delete: `app/`, `core/`, `data/`, `domain/`, `feature/`
- Replace: `gradle/libs.versions.toml`, `settings.gradle.kts`, `build.gradle.kts`
- Create:
  - `gradle.properties`
  - `build-logic/settings.gradle.kts`
  - `build-logic/convention/build.gradle.kts`
  - `build-logic/convention/src/main/kotlin/com/example/buildlogic/{ProjectExtensions,KotlinConfiguration,TestDependencies,ComposeDependencies}.kt`
  - `build-logic/convention/src/main/kotlin/{JvmLibrary,AndroidLibrary,AndroidLibraryCompose,Hilt,AndroidFeature,AndroidApplication}ConventionPlugin.kt`
  - `core/mvi/build.gradle.kts`
  - `core/mvi/src/main/kotlin/com/example/core/mvi/{Reducer,Transition,EffectHandler,KeyedEffect,Store}.kt`
- Test: `core/mvi/src/test/kotlin/com/example/core/mvi/{TransitionTest,StoreTest}.kt`

**Interfaces:**
- Produces (used by every later module):
  - plugin aliases `libs.plugins.shop.jvm.library`, `shop.android.library`, `shop.android.library.compose`, `shop.android.feature`, `shop.android.application`, `shop.hilt`
  - `fun interface Reducer<State, Action, Effect, Event> { fun reduce(state: State, action: Action): Transition<State, Effect, Event> }`
  - `data class Transition<out State, out Effect, out Event>(val state: State, val effects: List<Effect> = emptyList(), val events: List<Event> = emptyList())`
  - extensions `State.only()`, `State.withEffects(vararg)`, `State.withEvent(event)`, `Transition.plus(next)`, `Transition.then(rule)`
  - `fun interface EffectHandler<Effect, Action> { fun handle(effect: Effect): Flow<Action> }`
  - `interface KeyedEffect { val cancelKey: Any }`
  - `class Store<State, Action, Effect, Event>(initialState, scope, reducer, effectHandler)` with `state: StateFlow<State>`, `events: Flow<Event>`, `dispatch(action)`

- [ ] **Step 1: Remove the prototype modules**

```bash
git rm -r -q app core data domain feature
```

- [ ] **Step 2: Replace the version catalog**

`gradle/libs.versions.toml`:

```toml
[versions]
agp = "9.4.1"
kotlin = "2.4.21"
ksp = "2.3.12"
hilt = "2.60.1"
androidxHilt = "1.4.0"
coroutines = "1.11.0"
composeBom = "2026.09.00"
activityCompose = "1.13.0"
lifecycle = "2.11.0"
navigation = "2.10.2"
androidxAnnotation = "1.11.0"
javaxInject = "1"
junit = "4.13.2"
turbine = "1.2.1"
compileSdk = "37"
targetSdk = "36"
minSdk = "26"

[libraries]
android-gradlePlugin = { module = "com.android.tools.build:gradle", version.ref = "agp" }
kotlin-gradlePlugin = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version.ref = "kotlin" }
compose-gradlePlugin = { module = "org.jetbrains.kotlin:compose-compiler-gradle-plugin", version.ref = "kotlin" }
ksp-gradlePlugin = { module = "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin", version.ref = "ksp" }
hilt-gradlePlugin = { module = "com.google.dagger:hilt-android-gradle-plugin", version.ref = "hilt" }

kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
javax-inject = { module = "javax.inject:javax.inject", version.ref = "javaxInject" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
androidx-hilt-lifecycle-viewmodel-compose = { module = "androidx.hilt:hilt-lifecycle-viewmodel-compose", version.ref = "androidxHilt" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel = { module = "androidx.lifecycle:lifecycle-viewmodel", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
androidx-annotation = { module = "androidx.annotation:annotation", version.ref = "androidxAnnotation" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
junit = { module = "junit:junit", version.ref = "junit" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
shop-jvm-library = { id = "shop.jvm.library" }
shop-android-library = { id = "shop.android.library" }
shop-android-library-compose = { id = "shop.android.library.compose" }
shop-android-feature = { id = "shop.android.feature" }
shop-android-application = { id = "shop.android.application" }
shop-hilt = { id = "shop.hilt" }
```

- [ ] **Step 3: Write the root build files**

`gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
android.useAndroidX=true
kotlin.code.style=official
```

`settings.gradle.kts`:

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "checkout-mvi"

// Dependency rule, enforced by the module graph:
//
//   :app ──► :feature:checkout:presentation ──► :feature:checkout:domain ──► :core:domain
//     │              │        │
//     │              │        └──► :core:mvi
//     │              └──► :core:ui ──► :core:domain
//     └──► :feature:checkout:data ──► :feature:checkout:domain
//
// Presentation never sees data. Only :app knows both, so only :app assembles the Hilt graph.

include(":core:mvi")
```

`build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

- [ ] **Step 4: Write the convention plugins**

`build-logic/settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
```

`build-logic/convention/build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.example.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = "shop.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "shop.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "shop.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "shop.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = "shop.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("hilt") {
            id = "shop.hilt"
            implementationClass = "HiltConventionPlugin"
        }
    }
}
```

`build-logic/convention/src/main/kotlin/com/example/buildlogic/ProjectExtensions.kt`:

```kotlin
package com.example.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.versionInt(alias: String): Int =
    findVersion(alias).get().requiredVersion.toInt()

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()
```

`build-logic/convention/src/main/kotlin/com/example/buildlogic/KotlinConfiguration.kt`:

```kotlin
package com.example.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal val JAVA_VERSION = JavaVersion.VERSION_17

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
    }
    configureKotlinCompilation()
}

internal fun Project.configureKotlinCompilation() {
    tasks.withType<KotlinCompile>().configureEach {
        val isTestCompilation = name.contains("Test")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            if (isTestCompilation) {
                optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
    }
}
```

`build-logic/convention/src/main/kotlin/com/example/buildlogic/TestDependencies.kt`:

```kotlin
package com.example.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.addUnitTestDependencies() {
    dependencies {
        "testImplementation"(libs.library("junit"))
        "testImplementation"(libs.library("kotlinx-coroutines-test"))
        "testImplementation"(libs.library("turbine"))
    }
}
```

`build-logic/convention/src/main/kotlin/com/example/buildlogic/ComposeDependencies.kt`:

```kotlin
package com.example.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.addComposeDependencies() {
    dependencies {
        val bom = libs.library("androidx-compose-bom")
        "implementation"(platform(bom))
        "implementation"(libs.library("androidx-compose-ui"))
        "implementation"(libs.library("androidx-compose-ui-tooling-preview"))
        "implementation"(libs.library("androidx-compose-material3"))
        "debugImplementation"(libs.library("androidx-compose-ui-tooling"))
    }
}
```

`build-logic/convention/src/main/kotlin/JvmLibraryConventionPlugin.kt`:

```kotlin
import com.example.buildlogic.addUnitTestDependencies
import com.example.buildlogic.configureKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        configureKotlinJvm()
        addUnitTestDependencies()
    }
}
```

`build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt`:

```kotlin
import com.android.build.api.dsl.LibraryExtension
import com.example.buildlogic.JAVA_VERSION
import com.example.buildlogic.addUnitTestDependencies
import com.example.buildlogic.configureKotlinCompilation
import com.example.buildlogic.libs
import com.example.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> {
            compileSdk = libs.versionInt("compileSdk")
            defaultConfig.minSdk = libs.versionInt("minSdk")
            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            testOptions.unitTests.isReturnDefaultValues = true
        }
        configureKotlinCompilation()
        addUnitTestDependencies()
    }
}
```

`build-logic/convention/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt`:

```kotlin
import com.android.build.api.dsl.LibraryExtension
import com.example.buildlogic.addComposeDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("shop.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        extensions.configure<LibraryExtension> {
            buildFeatures.compose = true
        }
        addComposeDependencies()
    }
}
```

`build-logic/convention/src/main/kotlin/HiltConventionPlugin.kt`:

```kotlin
import com.example.buildlogic.libs
import com.example.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")
        dependencies {
            "implementation"(libs.library("hilt-android"))
            "ksp"(libs.library("hilt-compiler"))
        }
    }
}
```

`build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt`:

```kotlin
import com.example.buildlogic.libs
import com.example.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/** A feature's presentation module: Compose, Hilt, type-safe navigation and the shared MVI and UI cores. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("shop.android.library.compose")
        pluginManager.apply("shop.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        dependencies {
            "implementation"(project(":core:mvi"))
            "implementation"(project(":core:ui"))
            "implementation"(libs.library("androidx-lifecycle-runtime-compose"))
            "implementation"(libs.library("androidx-lifecycle-viewmodel"))
            "implementation"(libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            "implementation"(libs.library("androidx-navigation-compose"))
        }
    }
}
```

`build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt`:

```kotlin
import com.android.build.api.dsl.ApplicationExtension
import com.example.buildlogic.JAVA_VERSION
import com.example.buildlogic.addComposeDependencies
import com.example.buildlogic.configureKotlinCompilation
import com.example.buildlogic.libs
import com.example.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        extensions.configure<ApplicationExtension> {
            compileSdk = libs.versionInt("compileSdk")
            defaultConfig {
                minSdk = libs.versionInt("minSdk")
                targetSdk = libs.versionInt("targetSdk")
            }
            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            buildFeatures.compose = true
        }
        configureKotlinCompilation()
        addComposeDependencies()
    }
}
```

`core/mvi/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.shop.jvm.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
```

- [ ] **Step 5: Write the failing tests**

`core/mvi/src/test/kotlin/com/example/core/mvi/TransitionTest.kt`:

```kotlin
package com.example.core.mvi

import org.junit.Assert.assertEquals
import org.junit.Test

class TransitionTest {

    @Test
    fun `only carries no effects and no events`() {
        assertEquals(Transition(5), 5.only())
    }

    @Test
    fun `then applies the rule to the latest state and keeps everything requested`() {
        val transition: Transition<Int, String, String> = 1.withEffects("load")
            .then { state -> (state + 1).withEvent("loaded") }

        assertEquals(Transition(2, effects = listOf("load"), events = listOf("loaded")), transition)
    }

    @Test
    fun `plus keeps the latter state and concatenates effects and events`() {
        val first: Transition<String, Int, Int> = Transition("a", listOf(1), listOf(10))
        val second: Transition<String, Int, Int> = Transition("b", listOf(2), listOf(20))

        assertEquals(Transition("b", listOf(1, 2), listOf(10, 20)), first + second)
    }
}
```

`core/mvi/src/test/kotlin/com/example/core/mvi/StoreTest.kt`:

```kotlin
package com.example.core.mvi

import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreTest {

    private sealed interface TestAction {
        data class Add(val amount: Int) : TestAction
        data class Fetch(val id: Int) : TestAction
        data class Fetched(val id: Int) : TestAction
        data object Announce : TestAction
    }

    private data class TestEffect(val id: Int) : KeyedEffect {
        override val cancelKey: Any get() = "fetch"
    }

    private data class TestState(val total: Int = 0, val fetched: List<Int> = emptyList())

    private val reducer = Reducer<TestState, TestAction, TestEffect, String> { state, action ->
        when (action) {
            is TestAction.Add -> state.copy(total = state.total + action.amount).only()
            is TestAction.Fetch -> state.withEffects(TestEffect(action.id))
            is TestAction.Fetched -> state.copy(fetched = state.fetched + action.id).only()
            TestAction.Announce -> state.withEvent("hello")
        }
    }

    // The store loop must run on the test scheduler. advanceUntilIdle() stops as soon as only
    // backgroundScope work is left, so a store launched in backgroundScope would never run.
    private fun TestScope.storeScope(): CoroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler))

    @Test
    fun `actions are reduced in dispatch order`() = runTest {
        val scope = storeScope()
        val store = Store(TestState(), scope, reducer) { emptyFlow() }

        repeat(100) { store.dispatch(TestAction.Add(1)) }
        advanceUntilIdle()

        assertEquals(100, store.state.value.total)
        scope.cancel()
    }

    @Test
    fun `effect results come back as actions`() = runTest {
        val scope = storeScope()
        val store = Store(TestState(), scope, reducer) { effect -> flowOf(TestAction.Fetched(effect.id)) }

        store.dispatch(TestAction.Fetch(7))
        advanceUntilIdle()

        assertEquals(listOf(7), store.state.value.fetched)
        scope.cancel()
    }

    @Test
    fun `a keyed effect cancels the running one with the same key`() = runTest {
        val gates = mapOf(1 to CompletableDeferred<Unit>(), 2 to CompletableDeferred<Unit>())
        val scope = storeScope()
        val store = Store(TestState(), scope, reducer) { effect ->
            flow {
                gates.getValue(effect.id).await()
                emit(TestAction.Fetched(effect.id))
            }
        }

        store.dispatch(TestAction.Fetch(1))
        advanceUntilIdle()
        store.dispatch(TestAction.Fetch(2))
        advanceUntilIdle()
        gates.values.forEach { it.complete(Unit) }
        advanceUntilIdle()

        assertEquals(listOf(2), store.state.value.fetched)
        scope.cancel()
    }

    @Test
    fun `events are delivered in order and never replayed`() = runTest {
        val scope = storeScope()
        val store = Store(TestState(), scope, reducer) { emptyFlow() }

        store.events.test {
            store.dispatch(TestAction.Announce)
            store.dispatch(TestAction.Announce)
            assertEquals("hello", awaitItem())
            assertEquals("hello", awaitItem())
        }
        store.events.test {
            expectNoEvents()
        }
        scope.cancel()
    }
}
```

- [ ] **Step 6: Run the tests to verify they fail**

Run: `./gradlew :core:mvi:test`
Expected: FAIL. Compilation error `Unresolved reference 'Store'` (and `Transition`, `Reducer`, `KeyedEffect`).

- [ ] **Step 7: Implement the MVI core**

`core/mvi/src/main/kotlin/com/example/core/mvi/Reducer.kt`:

```kotlin
package com.example.core.mvi

/**
 * Pure state transition: (State, Action) -> Transition.
 *
 * Contract: no I/O, no coroutines, no clocks, no randomness, no Android types. The same input
 * always yields the same output. Side effects are described as [Transition.effects] and run by an
 * [EffectHandler]; one-off UI signals are described as [Transition.events].
 */
fun interface Reducer<State, Action, Effect, Event> {
    fun reduce(state: State, action: Action): Transition<State, Effect, Event>
}
```

`core/mvi/src/main/kotlin/com/example/core/mvi/Transition.kt`:

```kotlin
package com.example.core.mvi

/**
 * The result of reducing one action.
 *
 * @param state the next state, committed before any effect starts
 * @param effects work to run after the state is committed (network, disk, ...)
 * @param events one-off signals for the UI (navigation, transient messages)
 */
data class Transition<out State, out Effect, out Event>(
    val state: State,
    val effects: List<Effect> = emptyList(),
    val events: List<Event> = emptyList(),
)

/** A new (or unchanged) state with no effects and no events. */
fun <State> State.only(): Transition<State, Nothing, Nothing> = Transition(this)

fun <State, Effect> State.withEffects(vararg effects: Effect): Transition<State, Effect, Nothing> =
    Transition(this, effects.toList())

fun <State, Event> State.withEvent(event: Event): Transition<State, Nothing, Event> =
    Transition(this, events = listOf(event))

/** Keeps the latter state and concatenates effects and events. */
operator fun <State, Effect, Event> Transition<State, Effect, Event>.plus(
    next: Transition<State, Effect, Event>,
): Transition<State, Effect, Event> = Transition(next.state, effects + next.effects, events + next.events)

/** Applies [rule] to the state produced so far, accumulating effects and events. */
inline fun <State, Effect, Event> Transition<State, Effect, Event>.then(
    rule: (State) -> Transition<State, Effect, Event>,
): Transition<State, Effect, Event> = this + rule(state)
```

`core/mvi/src/main/kotlin/com/example/core/mvi/EffectHandler.kt`:

```kotlin
package com.example.core.mvi

import kotlinx.coroutines.flow.Flow

/**
 * Runs an effect and reports its results back as actions.
 *
 * One-shot work emits one action, a subscription keeps emitting, fire-and-forget emits nothing.
 * Failures must be translated into actions: an exception escaping the flow is a bug.
 */
fun interface EffectHandler<Effect, Action> {
    fun handle(effect: Effect): Flow<Action>
}
```

`core/mvi/src/main/kotlin/com/example/core/mvi/KeyedEffect.kt`:

```kotlin
package com.example.core.mvi

/**
 * Latest-wins concurrency, declared on the effect so the reducer stays in charge: starting an
 * effect whose [cancelKey] matches a running one cancels the running one.
 */
interface KeyedEffect {
    val cancelKey: Any
}
```

`core/mvi/src/main/kotlin/com/example/core/mvi/Store.kt`:

```kotlin
package com.example.core.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * The MVI loop of one screen.
 *
 * Every action goes through one channel and is reduced by one coroutine, so actions are reduced
 * strictly in order and the reducer never runs concurrently with itself. Effects run in [scope] and
 * report their results back as actions. Events are buffered without limit, so emitting one never
 * blocks the loop, and each event reaches one collector once.
 *
 * Effect handlers own error translation: an exception escaping an effect is a bug and is not
 * caught here.
 */
class Store<State, Action, Effect, Event>(
    initialState: State,
    private val scope: CoroutineScope,
    private val reducer: Reducer<State, Action, Effect, Event>,
    private val effectHandler: EffectHandler<Effect, Action>,
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    private val actions = Channel<Action>(Channel.UNLIMITED)

    // Touched only by the loop coroutine, so it needs no locking.
    private val runningKeyedEffects = HashMap<Any, Job>()

    init {
        scope.launch {
            for (action in actions) reduce(action)
        }
    }

    fun dispatch(action: Action) {
        actions.trySend(action)
    }

    private fun reduce(action: Action) {
        val transition = reducer.reduce(_state.value, action)
        _state.value = transition.state
        transition.events.forEach { _events.trySend(it) }
        transition.effects.forEach(::start)
    }

    private fun start(effect: Effect) {
        val key = (effect as? KeyedEffect)?.cancelKey
        key?.let { runningKeyedEffects.remove(it)?.cancel() }
        val job = scope.launch {
            effectHandler.handle(effect).collect { result -> dispatch(result) }
        }
        key?.let { runningKeyedEffects[it] = job }
    }
}
```

- [ ] **Step 8: Run the tests to verify they pass**

Run: `./gradlew :core:mvi:test`
Expected: PASS. 7 tests (3 `TransitionTest`, 4 `StoreTest`).

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "build: add convention plugins and rebuild the MVI core on the current toolchain"
```

---

### Task 2: Core domain — `Money` and `Outcome`

**Files:**
- Modify: `settings.gradle.kts` (add `include(":core:domain")`)
- Create:
  - `core/domain/build.gradle.kts`
  - `core/domain/src/main/kotlin/com/example/core/domain/{Money,Outcome}.kt`
- Test: `core/domain/src/test/kotlin/com/example/core/domain/{MoneyTest,OutcomeTest}.kt`

**Interfaces:**
- Produces:
  - `class Money(amount: BigDecimal, val currency: Currency) : Comparable<Money>`
    - `val amount: BigDecimal` (normalised to the currency scale)
    - `val minorUnits: Long`, `val isZero: Boolean`
    - `plus`, `minus`, `times(Int)`, `coerceAtMost(Money)`
    - `Money.zero(Currency)`, `Money.of(amount: String, currencyCode: String)`, `Money.ofMinor(minor: Long, currencyCode: String)`
  - `sealed interface Outcome<out T, out E>` with `Success(value)` and `Failure(error)`
    - `Outcome.map(transform)`, `Outcome.fold(onSuccess, onFailure)`

- [ ] **Step 1: Register the module**

Append to `settings.gradle.kts`:

```kotlin
include(":core:domain")
```

`core/domain/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.shop.jvm.library)
}
```

- [ ] **Step 2: Write the failing tests**

`core/domain/src/test/kotlin/com/example/core/domain/MoneyTest.kt`:

```kotlin
package com.example.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Currency

class MoneyTest {

    private val eur = Currency.getInstance("EUR")

    @Test
    fun `amounts are normalised to the currency scale`() {
        assertEquals(Money.of("5.00", "EUR"), Money(BigDecimal("5.0"), eur))
        assertEquals("5.00", Money(BigDecimal("5"), eur).amount.toPlainString())
    }

    @Test
    fun `equal amounts have equal hash codes`() {
        assertEquals(Money.of("5", "EUR").hashCode(), Money.of("5.00", "EUR").hashCode())
    }

    @Test
    fun `arithmetic keeps the currency`() {
        assertEquals(Money.of("17.50", "EUR"), Money.of("10.00", "EUR") + Money.of("7.50", "EUR"))
        assertEquals(Money.of("2.50", "EUR"), Money.of("10.00", "EUR") - Money.of("7.50", "EUR"))
        assertEquals(Money.of("36.00", "EUR"), Money.of("12.00", "EUR") * 3)
    }

    @Test
    fun `mixing currencies fails fast`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money.of("1", "EUR") + Money.of("1", "USD")
        }
    }

    @Test
    fun `minor units convert both ways`() {
        val price = Money.ofMinor(1299, "EUR")

        assertEquals(Money.of("12.99", "EUR"), price)
        assertEquals(1299L, price.minorUnits)
    }

    @Test
    fun `coerceAtMost caps the amount`() {
        assertEquals(Money.of("10.00", "EUR"), Money.of("15.00", "EUR").coerceAtMost(Money.of("10.00", "EUR")))
        assertEquals(Money.of("5.00", "EUR"), Money.of("5.00", "EUR").coerceAtMost(Money.of("10.00", "EUR")))
    }

    @Test
    fun `zero is zero`() {
        assertTrue(Money.zero(eur).isZero)
        assertFalse(Money.of("0.01", "EUR").isZero)
    }
}
```

`core/domain/src/test/kotlin/com/example/core/domain/OutcomeTest.kt`:

```kotlin
package com.example.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OutcomeTest {

    @Test
    fun `map transforms a success`() {
        val outcome: Outcome<Int, String> = Outcome.Success(2)

        assertEquals(Outcome.Success(4), outcome.map { it * 2 })
    }

    @Test
    fun `map keeps a failure`() {
        val outcome: Outcome<Int, String> = Outcome.Failure("boom")

        assertEquals(Outcome.Failure("boom"), outcome.map { it * 2 })
    }

    @Test
    fun `fold picks the matching branch`() {
        val success: Outcome<Int, String> = Outcome.Success(2)
        val failure: Outcome<Int, String> = Outcome.Failure("boom")

        assertEquals("value 2", success.fold({ "value $it" }, { "error $it" }))
        assertEquals("error boom", failure.fold({ "value $it" }, { "error $it" }))
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :core:domain:test`
Expected: FAIL. `Unresolved reference 'Money'` and `'Outcome'`.

- [ ] **Step 4: Implement**

`core/domain/src/main/kotlin/com/example/core/domain/Money.kt`:

```kotlin
package com.example.core.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

/**
 * An amount in one currency.
 *
 * The amount is normalised to the currency's fraction digits on construction, so equality does not
 * depend on how the amount was written ("5.0" equals "5.00"). Arithmetic across currencies is a
 * programming error and fails fast.
 */
class Money(amount: BigDecimal, val currency: Currency) : Comparable<Money> {

    val amount: BigDecimal = amount.setScale(currency.defaultFractionDigits, RoundingMode.HALF_EVEN)

    val minorUnits: Long get() = amount.movePointRight(currency.defaultFractionDigits).longValueExact()

    val isZero: Boolean get() = amount.signum() == 0

    operator fun plus(other: Money): Money = sameCurrency(other) { Money(amount + other.amount, currency) }

    operator fun minus(other: Money): Money = sameCurrency(other) { Money(amount - other.amount, currency) }

    operator fun times(quantity: Int): Money = Money(amount * quantity.toBigDecimal(), currency)

    override fun compareTo(other: Money): Int = sameCurrency(other) { amount.compareTo(other.amount) }

    fun coerceAtMost(max: Money): Money = if (this > max) max else this

    override fun equals(other: Any?): Boolean =
        other is Money && amount == other.amount && currency == other.currency

    override fun hashCode(): Int = 31 * amount.hashCode() + currency.hashCode()

    override fun toString(): String = "$amount ${currency.currencyCode}"

    private inline fun <T> sameCurrency(other: Money, block: () -> T): T {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return block()
    }

    companion object {
        fun zero(currency: Currency): Money = Money(BigDecimal.ZERO, currency)

        fun of(amount: String, currencyCode: String): Money =
            Money(BigDecimal(amount), Currency.getInstance(currencyCode))

        fun ofMinor(minor: Long, currencyCode: String): Money {
            val currency = Currency.getInstance(currencyCode)
            return Money(BigDecimal.valueOf(minor, currency.defaultFractionDigits), currency)
        }
    }
}
```

`core/domain/src/main/kotlin/com/example/core/domain/Outcome.kt`:

```kotlin
package com.example.core.domain

/**
 * The result of an operation that can fail in an expected way. Each feature brings its own error
 * type, so a failure stays a typed, comparable value instead of an exception.
 */
sealed interface Outcome<out T, out E> {
    data class Success<out T>(val value: T) : Outcome<T, Nothing>
    data class Failure<out E>(val error: E) : Outcome<Nothing, E>
}

inline fun <T, E, R> Outcome<T, E>.map(transform: (T) -> R): Outcome<R, E> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T, E, R> Outcome<T, E>.fold(onSuccess: (T) -> R, onFailure: (E) -> R): R = when (this) {
    is Outcome.Success -> onSuccess(value)
    is Outcome.Failure -> onFailure(error)
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :core:domain:test`
Expected: PASS. 10 tests.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(core): add Money with scale-safe equality and a typed Outcome"
```

---

### Task 3: Checkout domain — models, errors and business rules

**Files:**
- Modify: `settings.gradle.kts` (add `include(":feature:checkout:domain")`)
- Create:
  - `feature/checkout/domain/build.gradle.kts`
  - `feature/checkout/domain/src/main/kotlin/com/example/feature/checkout/domain/model/{Address,AddressValidation,Cart,CartLine,CheckoutContext,Customer,FeatureFlag,Order,OrderRequest,OrderTotals,PaymentMethod,Promo,ShippingOption}.kt`
  - `feature/checkout/domain/src/main/kotlin/com/example/feature/checkout/domain/error/{CheckoutError,CheckoutOutcome}.kt`
  - `feature/checkout/domain/src/main/kotlin/com/example/feature/checkout/domain/rule/{ShippingPrice,ShippingAvailability,ShippingPreference,CartLimits}.kt`
  - `feature/checkout/domain/src/testFixtures/kotlin/com/example/feature/checkout/domain/fixture/CheckoutFixtures.kt`
- Test: `feature/checkout/domain/src/test/kotlin/com/example/feature/checkout/domain/rule/{ShippingPriceTest,ShippingAvailabilityTest,ShippingPreferenceTest}.kt`

**Interfaces:**
- Consumes: `Money`, `Outcome` (Task 2).
- Produces:
  - Models:
    - `Address(id, label, recipient, line1, city, postalCode, isDefault)`
    - `AddressValidation { Deliverable; Undeliverable(reason); enum Reason { OUTSIDE_SERVICE_AREA, INCOMPLETE, PO_BOX_NOT_ALLOWED } }`
    - `Cart(lines, currency)` with `isEmpty`, `itemCount`, `subtotal`
    - `CartLine(sku, name, unitPrice, quantity)` with `lineTotal`
    - `CheckoutContext(cart, addresses, paymentMethods, customer, isOnline, isExpressShippingEnabled)`
    - `Customer(id, displayName, isMember)`
    - `FeatureFlag { EXPRESS_SHIPPING }`
    - `Order(id)`
    - `OrderRequest(checkoutId, addressId, shippingOptionId, paymentMethodId, promoCode)`
    - `OrderTotals(subtotal, shipping, discount, total)`
    - `PaymentMethod(id, brand, last4, isDefault)`
    - `Promo(code, discount)`
    - `ShippingOption(id, label, price, estimatedDays, isExpress)`
  - Errors: `sealed interface CheckoutError` and `typealias CheckoutOutcome<T> = Outcome<T, CheckoutError>`
  - Rules:
    - `ShippingOption.priceFor(customer: Customer): Money`
    - `List<ShippingOption>.availableFor(isExpressEnabled: Boolean): List<ShippingOption>`
    - `List<ShippingOption>.preferredFor(currentId: String?, customer: Customer): ShippingOption?`
    - `const val MAX_QUANTITY_PER_LINE = 10`
  - Fixtures (package `...domain.fixture`): `EUR`, `eur()`, `HOME`, `OFFICE`, `VISA`, `MASTERCARD`, `STANDARD`, `PICKUP`, `EXPRESS`, `SAVE5`, `MUG`, `TEE`, `CUSTOMER`, `MEMBER`, `cartOf()`, `checkoutContext()`

- [ ] **Step 1: Register the module**

Append to `settings.gradle.kts`:

```kotlin
include(":feature:checkout:domain")
```

`feature/checkout/domain/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.shop.jvm.library)
    `java-test-fixtures`
}

dependencies {
    api(projects.core.domain)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
```

- [ ] **Step 2: Write the models and errors**

All files in `feature/checkout/domain/src/main/kotlin/com/example/feature/checkout/domain/`.

`model/Address.kt`:

```kotlin
package com.example.feature.checkout.domain.model

data class Address(
    val id: String,
    val label: String,
    val recipient: String,
    val line1: String,
    val city: String,
    val postalCode: String,
    val isDefault: Boolean,
)
```

`model/AddressValidation.kt`:

```kotlin
package com.example.feature.checkout.domain.model

sealed interface AddressValidation {
    data object Deliverable : AddressValidation
    data class Undeliverable(val reason: Reason) : AddressValidation

    enum class Reason { OUTSIDE_SERVICE_AREA, INCOMPLETE, PO_BOX_NOT_ALLOWED }
}
```

`model/CartLine.kt`:

```kotlin
package com.example.feature.checkout.domain.model

import com.example.core.domain.Money

data class CartLine(
    val sku: String,
    val name: String,
    val unitPrice: Money,
    val quantity: Int,
) {
    val lineTotal: Money get() = unitPrice * quantity
}
```

`model/Cart.kt`:

```kotlin
package com.example.feature.checkout.domain.model

import com.example.core.domain.Money
import java.util.Currency

data class Cart(val lines: List<CartLine>, val currency: Currency) {
    val isEmpty: Boolean get() = lines.isEmpty()
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val subtotal: Money get() = lines.fold(Money.zero(currency)) { total, line -> total + line.lineTotal }
}
```

`model/CheckoutContext.kt`:

```kotlin
package com.example.feature.checkout.domain.model

/** Everything the checkout observes from the outside world, as one read model. */
data class CheckoutContext(
    val cart: Cart,
    val addresses: List<Address>,
    val paymentMethods: List<PaymentMethod>,
    val customer: Customer,
    val isOnline: Boolean,
    val isExpressShippingEnabled: Boolean,
)
```

`model/Customer.kt`:

```kotlin
package com.example.feature.checkout.domain.model

data class Customer(val id: String, val displayName: String, val isMember: Boolean)
```

`model/FeatureFlag.kt`:

```kotlin
package com.example.feature.checkout.domain.model

enum class FeatureFlag { EXPRESS_SHIPPING }
```

`model/Order.kt`:

```kotlin
package com.example.feature.checkout.domain.model

data class Order(val id: String)
```

`model/OrderRequest.kt`:

```kotlin
package com.example.feature.checkout.domain.model

/**
 * One attempt to place an order.
 *
 * [checkoutId] identifies the checkout the request comes from. Retries within one checkout send the
 * same request, so the backend can recognise them, while an identical order placed in a later
 * checkout is a new order.
 */
data class OrderRequest(
    val checkoutId: String,
    val addressId: String,
    val shippingOptionId: String,
    val paymentMethodId: String,
    val promoCode: String?,
)
```

`model/OrderTotals.kt`:

```kotlin
package com.example.feature.checkout.domain.model

import com.example.core.domain.Money

data class OrderTotals(
    val subtotal: Money,
    val shipping: Money,
    val discount: Money,
    val total: Money,
)
```

`model/PaymentMethod.kt`:

```kotlin
package com.example.feature.checkout.domain.model

data class PaymentMethod(
    val id: String,
    val brand: String,
    val last4: String,
    val isDefault: Boolean,
)
```

`model/Promo.kt`:

```kotlin
package com.example.feature.checkout.domain.model

import com.example.core.domain.Money

data class Promo(val code: String, val discount: Money)
```

`model/ShippingOption.kt`:

```kotlin
package com.example.feature.checkout.domain.model

import com.example.core.domain.Money

data class ShippingOption(
    val id: String,
    val label: String,
    val price: Money,
    val estimatedDays: Int,
    val isExpress: Boolean,
)
```

`error/CheckoutError.kt`:

```kotlin
package com.example.feature.checkout.domain.error

/**
 * Failures the checkout understands. The data layer maps exceptions and status codes into these,
 * so nothing above it ever handles a Throwable and state stays comparable.
 */
sealed interface CheckoutError {
    data object Network : CheckoutError
    data object Unauthorized : CheckoutError
    data object NotFound : CheckoutError
    data class Unexpected(val diagnostic: String?) : CheckoutError

    sealed interface Promo : CheckoutError {
        data object Invalid : Promo
        data object Expired : Promo
        data object MinimumNotReached : Promo
    }

    sealed interface Payment : CheckoutError {
        data object Declined : Payment
        data object RequiresAction : Payment
    }

    data object OutOfStock : CheckoutError
    data object PriceChanged : CheckoutError
}
```

`error/CheckoutOutcome.kt`:

```kotlin
package com.example.feature.checkout.domain.error

import com.example.core.domain.Outcome

typealias CheckoutOutcome<T> = Outcome<T, CheckoutError>
```

- [ ] **Step 3: Write the shared test fixtures**

`feature/checkout/domain/src/testFixtures/kotlin/com/example/feature/checkout/domain/fixture/CheckoutFixtures.kt`:

```kotlin
package com.example.feature.checkout.domain.fixture

import com.example.core.domain.Money
import com.example.feature.checkout.domain.model.Address
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.CartLine
import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.model.PaymentMethod
import com.example.feature.checkout.domain.model.Promo
import com.example.feature.checkout.domain.model.ShippingOption
import java.util.Currency

val EUR: Currency = Currency.getInstance("EUR")

fun eur(amount: String): Money = Money.of(amount, "EUR")

val HOME = Address("home", "Home", "Alex Doe", "Calle Larios 1", "Málaga", "29005", isDefault = true)
val OFFICE = Address("office", "Office", "Alex Doe", "Av. de Andalucía 10", "Málaga", "29007", isDefault = false)

val VISA = PaymentMethod("visa", "Visa", "4242", isDefault = true)
val MASTERCARD = PaymentMethod("mastercard", "Mastercard", "0002", isDefault = false)

val STANDARD = ShippingOption("standard", "Standard", eur("4.99"), estimatedDays = 3, isExpress = false)
val PICKUP = ShippingOption("pickup", "Pickup point", eur("2.99"), estimatedDays = 4, isExpress = false)
val EXPRESS = ShippingOption("express", "Express", eur("12.99"), estimatedDays = 1, isExpress = true)

val SAVE5 = Promo("SAVE5", eur("5.00"))

val MUG = CartLine("mug", "Mug", eur("12.00"), quantity = 2)
val TEE = CartLine("tee", "T-shirt", eur("20.00"), quantity = 1)

val CUSTOMER = Customer("customer-1", "Alex", isMember = false)
val MEMBER = Customer("customer-1", "Alex", isMember = true)

fun cartOf(vararg lines: CartLine): Cart = Cart(lines.toList(), EUR)

fun checkoutContext(
    cart: Cart = cartOf(MUG),
    addresses: List<Address> = listOf(HOME, OFFICE),
    paymentMethods: List<PaymentMethod> = listOf(VISA, MASTERCARD),
    customer: Customer = CUSTOMER,
    isOnline: Boolean = true,
    isExpressShippingEnabled: Boolean = true,
): CheckoutContext = CheckoutContext(cart, addresses, paymentMethods, customer, isOnline, isExpressShippingEnabled)
```

- [ ] **Step 4: Write the failing rule tests**

`feature/checkout/domain/src/test/kotlin/com/example/feature/checkout/domain/rule/ShippingPriceTest.kt`:

```kotlin
package com.example.feature.checkout.domain.rule

import com.example.feature.checkout.domain.fixture.CUSTOMER
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.MEMBER
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.eur
import org.junit.Assert.assertEquals
import org.junit.Test

class ShippingPriceTest {

    @Test
    fun `members ship non-express options for free`() {
        assertEquals(eur("0.00"), STANDARD.priceFor(MEMBER))
        assertEquals(eur("0.00"), PICKUP.priceFor(MEMBER))
    }

    @Test
    fun `members still pay for express`() {
        assertEquals(eur("12.99"), EXPRESS.priceFor(MEMBER))
    }

    @Test
    fun `regular customers pay the listed price`() {
        assertEquals(eur("4.99"), STANDARD.priceFor(CUSTOMER))
    }
}
```

`feature/checkout/domain/src/test/kotlin/com/example/feature/checkout/domain/rule/ShippingAvailabilityTest.kt`:

```kotlin
package com.example.feature.checkout.domain.rule

import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import org.junit.Assert.assertEquals
import org.junit.Test

class ShippingAvailabilityTest {

    private val quotes = listOf(STANDARD, PICKUP, EXPRESS)

    @Test
    fun `express options are hidden while the flag is off`() {
        assertEquals(listOf(STANDARD, PICKUP), quotes.availableFor(isExpressEnabled = false))
    }

    @Test
    fun `every option is offered while the flag is on`() {
        assertEquals(quotes, quotes.availableFor(isExpressEnabled = true))
    }
}
```

`feature/checkout/domain/src/test/kotlin/com/example/feature/checkout/domain/rule/ShippingPreferenceTest.kt`:

```kotlin
package com.example.feature.checkout.domain.rule

import com.example.feature.checkout.domain.fixture.CUSTOMER
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.MEMBER
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.model.ShippingOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShippingPreferenceTest {

    private val options = listOf(STANDARD, PICKUP, EXPRESS)

    @Test
    fun `keeps the current choice while it is still offered`() {
        assertEquals(EXPRESS, options.preferredFor(EXPRESS.id, CUSTOMER))
    }

    @Test
    fun `picks the cheapest option for a regular customer`() {
        assertEquals(PICKUP, options.preferredFor(currentId = null, customer = CUSTOMER))
    }

    @Test
    fun `prefers the faster option when the prices tie`() {
        // Members pay nothing for Standard or Pickup, so the faster Standard wins.
        assertEquals(STANDARD, options.preferredFor(currentId = null, customer = MEMBER))
    }

    @Test
    fun `replaces a choice that is no longer offered`() {
        assertEquals(PICKUP, options.preferredFor(currentId = "gone", customer = CUSTOMER))
    }

    @Test
    fun `nothing to choose from yields no choice`() {
        assertNull(emptyList<ShippingOption>().preferredFor(currentId = null, customer = CUSTOMER))
    }
}
```

- [ ] **Step 5: Run the tests to verify they fail**

Run: `./gradlew :feature:checkout:domain:test`
Expected: FAIL. `Unresolved reference 'priceFor'`, `'availableFor'`, `'preferredFor'`.

- [ ] **Step 6: Implement the rules**

`rule/ShippingPrice.kt`:

```kotlin
package com.example.feature.checkout.domain.rule

import com.example.core.domain.Money
import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.model.ShippingOption

/** Members ship for free unless they choose express delivery. The only place this perk is defined. */
fun ShippingOption.priceFor(customer: Customer): Money =
    if (customer.isMember && !isExpress) Money.zero(price.currency) else price
```

`rule/ShippingAvailability.kt`:

```kotlin
package com.example.feature.checkout.domain.rule

import com.example.feature.checkout.domain.model.ShippingOption

/** Express options are offered only while the express-shipping flag is on. */
fun List<ShippingOption>.availableFor(isExpressEnabled: Boolean): List<ShippingOption> =
    filter { isExpressEnabled || !it.isExpress }
```

`rule/ShippingPreference.kt`:

```kotlin
package com.example.feature.checkout.domain.rule

import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.model.ShippingOption

/**
 * Keeps the current choice while it is still offered. Otherwise picks the option that costs this
 * customer the least, preferring the faster one when prices tie.
 */
fun List<ShippingOption>.preferredFor(currentId: String?, customer: Customer): ShippingOption? =
    firstOrNull { it.id == currentId }
        ?: minWithOrNull(compareBy<ShippingOption> { it.priceFor(customer) }.thenBy { it.estimatedDays })
```

`rule/CartLimits.kt`:

```kotlin
package com.example.feature.checkout.domain.rule

/** The most units of one product a single order may contain. */
const val MAX_QUANTITY_PER_LINE = 10
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew :feature:checkout:domain:test`
Expected: PASS. 10 tests.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat(checkout): add the checkout domain models, errors and shipping rules"
```

---

### Task 4: Checkout domain — repository interfaces, fakes and use cases

**Files:**
- Create:
  - `feature/checkout/domain/src/main/kotlin/com/example/feature/checkout/domain/repository/{CartRepository,AddressRepository,ShippingRepository,PromoRepository,PaymentMethodRepository,OrderRepository,CustomerRepository,NetworkMonitor,FeatureFlagRepository}.kt`
  - `feature/checkout/domain/src/main/kotlin/com/example/feature/checkout/domain/usecase/{ObserveCheckoutContextUseCase,UpdateCartQuantityUseCase,ValidateAddressUseCase,GetShippingQuotesUseCase,RedeemPromoUseCase,PlaceOrderUseCase,CalculateOrderTotalsUseCase}.kt`
  - `feature/checkout/domain/src/testFixtures/kotlin/com/example/feature/checkout/domain/fake/{FakeCartRepository,FakeAddressRepository,FakeShippingRepository,FakePromoRepository,FakePaymentMethodRepository,FakeOrderRepository,FakeCustomerRepository,FakeNetworkMonitor,FakeFeatureFlagRepository}.kt`
- Test: `feature/checkout/domain/src/test/kotlin/com/example/feature/checkout/domain/usecase/{ObserveCheckoutContextUseCaseTest,UpdateCartQuantityUseCaseTest,ValidateAddressUseCaseTest,GetShippingQuotesUseCaseTest,RedeemPromoUseCaseTest,PlaceOrderUseCaseTest,CalculateOrderTotalsUseCaseTest}.kt`

**Interfaces:**
- Consumes: Task 3 models, `CheckoutError`, `CheckoutOutcome`, rules, fixtures.
- Produces:
  - Repository interfaces:
    - `CartRepository { observeCart(): Flow<Cart>; suspend setQuantity(sku, quantity): CheckoutOutcome<Unit>; suspend removeLine(sku): CheckoutOutcome<Unit> }`
    - `AddressRepository { observeAddresses(): Flow<CheckoutOutcome<List<Address>>>; suspend validate(addressId): CheckoutOutcome<AddressValidation> }`
    - `ShippingRepository { suspend quote(addressId, cart): CheckoutOutcome<List<ShippingOption>> }`
    - `PromoRepository { suspend redeem(code, cart): CheckoutOutcome<Promo> }`
    - `PaymentMethodRepository { observePaymentMethods(): Flow<CheckoutOutcome<List<PaymentMethod>>> }`
    - `OrderRepository { suspend place(request): CheckoutOutcome<Order> }`
    - `CustomerRepository { observeCustomer(): Flow<Customer> }`
    - `NetworkMonitor { val isOnline: Flow<Boolean> }`
    - `FeatureFlagRepository { observe(flag): Flow<Boolean> }`
  - Use cases (all `@Inject constructor`, `operator fun invoke`):
    - `ObserveCheckoutContextUseCase(cart, addresses, paymentMethods, customer, network, flags)(): Flow<CheckoutOutcome<CheckoutContext>>`
    - `UpdateCartQuantityUseCase(cart)(sku, quantity)`
    - `ValidateAddressUseCase(addresses)(addressId)`
    - `GetShippingQuotesUseCase(shipping)(addressId, cart)`
    - `RedeemPromoUseCase(promos)(rawCode, cart)`
    - `PlaceOrderUseCase(orders)(request)`
    - `CalculateOrderTotalsUseCase()(cart, shipping, promo, customer): OrderTotals`
  - Fakes (package `...domain.fake`), with constructor defaults that match `checkoutContext()`:
    - `FakeCartRepository(initial: Cart = cartOf(MUG))`: `.cart: MutableStateFlow<Cart>`, `.failure: CheckoutError?`
    - `FakeAddressRepository(addresses = Success(listOf(HOME, OFFICE)), validation = Success(Deliverable))`: `.validatedIds`
    - `FakeShippingRepository(quotes = Success(listOf(STANDARD, PICKUP, EXPRESS)))`: `.quotedAddressIds`
    - `FakePromoRepository(result = Success(SAVE5))`: `.redeemedCodes`
    - `FakePaymentMethodRepository(paymentMethods = Success(listOf(VISA, MASTERCARD)))`
    - `FakeOrderRepository(result = Success(Order("order-1")))`: `.placedRequests`
    - `FakeCustomerRepository(initial = CUSTOMER)`: `.customer`
    - `FakeNetworkMonitor(online = true)`: `.online`
    - `FakeFeatureFlagRepository(enabled = true)`: `.expressShipping`

- [ ] **Step 1: Write the repository interfaces**

All files in `feature/checkout/domain/src/main/kotlin/com/example/feature/checkout/domain/repository/`. The repository interfaces are owned by the domain and implemented by the data module.

`CartRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(): Flow<Cart>
    suspend fun setQuantity(sku: String, quantity: Int): CheckoutOutcome<Unit>
    suspend fun removeLine(sku: String): CheckoutOutcome<Unit>
}
```

`AddressRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Address
import com.example.feature.checkout.domain.model.AddressValidation
import kotlinx.coroutines.flow.Flow

interface AddressRepository {
    /** Emits an outcome, so a failed fetch is a value rather than an exception that ends the stream. */
    fun observeAddresses(): Flow<CheckoutOutcome<List<Address>>>
    suspend fun validate(addressId: String): CheckoutOutcome<AddressValidation>
}
```

`ShippingRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.ShippingOption

interface ShippingRepository {
    suspend fun quote(addressId: String, cart: Cart): CheckoutOutcome<List<ShippingOption>>
}
```

`PromoRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.Promo

interface PromoRepository {
    suspend fun redeem(code: String, cart: Cart): CheckoutOutcome<Promo>
}
```

`PaymentMethodRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface PaymentMethodRepository {
    fun observePaymentMethods(): Flow<CheckoutOutcome<List<PaymentMethod>>>
}
```

`OrderRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest

interface OrderRepository {
    suspend fun place(request: OrderRequest): CheckoutOutcome<Order>
}
```

`CustomerRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.model.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun observeCustomer(): Flow<Customer>
}
```

`NetworkMonitor.kt`:

```kotlin
package com.example.feature.checkout.domain.repository

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
```

`FeatureFlagRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.model.FeatureFlag
import kotlinx.coroutines.flow.Flow

interface FeatureFlagRepository {
    fun observe(flag: FeatureFlag): Flow<Boolean>
}
```

- [ ] **Step 2: Write the fakes**

All files in `feature/checkout/domain/src/testFixtures/kotlin/com/example/feature/checkout/domain/fake/`.

`FakeCartRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.fake

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeCartRepository(initial: Cart = cartOf(MUG)) : CartRepository {

    val cart = MutableStateFlow(initial)

    /** When set, every write fails with this error and leaves the cart untouched. */
    var failure: CheckoutError? = null

    override fun observeCart(): Flow<Cart> = cart

    override suspend fun setQuantity(sku: String, quantity: Int): CheckoutOutcome<Unit> = write {
        cart.update { current ->
            current.copy(lines = current.lines.map { if (it.sku == sku) it.copy(quantity = quantity) else it })
        }
    }

    override suspend fun removeLine(sku: String): CheckoutOutcome<Unit> = write {
        cart.update { current -> current.copy(lines = current.lines.filterNot { it.sku == sku }) }
    }

    private inline fun write(change: () -> Unit): CheckoutOutcome<Unit> {
        failure?.let { return Outcome.Failure(it) }
        change()
        return Outcome.Success(Unit)
    }
}
```

`FakeAddressRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.fake

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.OFFICE
import com.example.feature.checkout.domain.model.Address
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.repository.AddressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAddressRepository(
    var addresses: CheckoutOutcome<List<Address>> = Outcome.Success(listOf(HOME, OFFICE)),
    var validation: CheckoutOutcome<AddressValidation> = Outcome.Success(AddressValidation.Deliverable),
) : AddressRepository {

    val validatedIds = mutableListOf<String>()

    // Like a remote fetch: each subscription reads the configured result once.
    override fun observeAddresses(): Flow<CheckoutOutcome<List<Address>>> = flow { emit(addresses) }

    override suspend fun validate(addressId: String): CheckoutOutcome<AddressValidation> {
        validatedIds += addressId
        return validation
    }
}
```

`FakeShippingRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.fake

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.repository.ShippingRepository

class FakeShippingRepository(
    var quotes: CheckoutOutcome<List<ShippingOption>> = Outcome.Success(listOf(STANDARD, PICKUP, EXPRESS)),
) : ShippingRepository {

    val quotedAddressIds = mutableListOf<String>()

    override suspend fun quote(addressId: String, cart: Cart): CheckoutOutcome<List<ShippingOption>> {
        quotedAddressIds += addressId
        return quotes
    }
}
```

`FakePromoRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.fake

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.Promo
import com.example.feature.checkout.domain.repository.PromoRepository

class FakePromoRepository(var result: CheckoutOutcome<Promo> = Outcome.Success(SAVE5)) : PromoRepository {

    val redeemedCodes = mutableListOf<String>()

    override suspend fun redeem(code: String, cart: Cart): CheckoutOutcome<Promo> {
        redeemedCodes += code
        return result
    }
}
```

`FakePaymentMethodRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.fake

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.fixture.MASTERCARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.model.PaymentMethod
import com.example.feature.checkout.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakePaymentMethodRepository(
    var paymentMethods: CheckoutOutcome<List<PaymentMethod>> = Outcome.Success(listOf(VISA, MASTERCARD)),
) : PaymentMethodRepository {

    override fun observePaymentMethods(): Flow<CheckoutOutcome<List<PaymentMethod>>> = flow { emit(paymentMethods) }
}
```

`FakeOrderRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.fake

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.domain.repository.OrderRepository

class FakeOrderRepository(var result: CheckoutOutcome<Order> = Outcome.Success(Order("order-1"))) : OrderRepository {

    val placedRequests = mutableListOf<OrderRequest>()

    override suspend fun place(request: OrderRequest): CheckoutOutcome<Order> {
        placedRequests += request
        return result
    }
}
```

`FakeCustomerRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.fake

import com.example.feature.checkout.domain.fixture.CUSTOMER
import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCustomerRepository(initial: Customer = CUSTOMER) : CustomerRepository {

    val customer = MutableStateFlow(initial)

    override fun observeCustomer(): Flow<Customer> = customer
}
```

`FakeNetworkMonitor.kt`:

```kotlin
package com.example.feature.checkout.domain.fake

import com.example.feature.checkout.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {

    val online = MutableStateFlow(online)

    override val isOnline: Flow<Boolean> get() = online
}
```

`FakeFeatureFlagRepository.kt`:

```kotlin
package com.example.feature.checkout.domain.fake

import com.example.feature.checkout.domain.model.FeatureFlag
import com.example.feature.checkout.domain.repository.FeatureFlagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFeatureFlagRepository(enabled: Boolean = true) : FeatureFlagRepository {

    val expressShipping = MutableStateFlow(enabled)

    override fun observe(flag: FeatureFlag): Flow<Boolean> = expressShipping
}
```

- [ ] **Step 3: Write the failing use case tests**

All files in `feature/checkout/domain/src/test/kotlin/com/example/feature/checkout/domain/usecase/`.

`ObserveCheckoutContextUseCaseTest.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import app.cash.turbine.test
import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fake.FakeAddressRepository
import com.example.feature.checkout.domain.fake.FakeCartRepository
import com.example.feature.checkout.domain.fake.FakeCustomerRepository
import com.example.feature.checkout.domain.fake.FakeFeatureFlagRepository
import com.example.feature.checkout.domain.fake.FakeNetworkMonitor
import com.example.feature.checkout.domain.fake.FakePaymentMethodRepository
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.TEE
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveCheckoutContextUseCaseTest {

    private val cartRepository = FakeCartRepository()
    private val addressRepository = FakeAddressRepository()
    private val paymentMethodRepository = FakePaymentMethodRepository()
    private val customerRepository = FakeCustomerRepository()
    private val networkMonitor = FakeNetworkMonitor()
    private val featureFlagRepository = FakeFeatureFlagRepository()

    private val observeCheckoutContext = ObserveCheckoutContextUseCase(
        cartRepository,
        addressRepository,
        paymentMethodRepository,
        customerRepository,
        networkMonitor,
        featureFlagRepository,
    )

    @Test
    fun `combines every source into one context`() = runTest {
        assertEquals(Outcome.Success(checkoutContext()), observeCheckoutContext().first())
    }

    @Test
    fun `a failed address fetch fails the context`() = runTest {
        addressRepository.addresses = Outcome.Failure(CheckoutError.Network)

        assertEquals(Outcome.Failure(CheckoutError.Network), observeCheckoutContext().first())
    }

    @Test
    fun `a failed payment method fetch fails the context`() = runTest {
        paymentMethodRepository.paymentMethods = Outcome.Failure(CheckoutError.Unauthorized)

        assertEquals(Outcome.Failure(CheckoutError.Unauthorized), observeCheckoutContext().first())
    }

    @Test
    fun `a cart change emits an updated context`() = runTest {
        observeCheckoutContext().test {
            assertEquals(Outcome.Success(checkoutContext()), awaitItem())

            cartRepository.cart.value = cartOf(MUG, TEE)

            assertEquals(Outcome.Success(checkoutContext(cart = cartOf(MUG, TEE))), awaitItem())
        }
    }

    @Test
    fun `losing connectivity emits an offline context`() = runTest {
        observeCheckoutContext().test {
            awaitItem()

            networkMonitor.online.value = false

            assertEquals(Outcome.Success(checkoutContext(isOnline = false)), awaitItem())
        }
    }
}
```

`UpdateCartQuantityUseCaseTest.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fake.FakeCartRepository
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.TEE
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.rule.MAX_QUANTITY_PER_LINE
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateCartQuantityUseCaseTest {

    private val cartRepository = FakeCartRepository(cartOf(MUG, TEE))
    private val updateCartQuantity = UpdateCartQuantityUseCase(cartRepository)

    private fun quantityOf(sku: String) = cartRepository.cart.value.lines.first { it.sku == sku }.quantity

    @Test
    fun `sets the requested quantity`() = runTest {
        assertEquals(Outcome.Success(Unit), updateCartQuantity(MUG.sku, 3))
        assertEquals(3, quantityOf(MUG.sku))
    }

    @Test
    fun `zero removes the line`() = runTest {
        updateCartQuantity(MUG.sku, 0)

        assertEquals(listOf(TEE), cartRepository.cart.value.lines)
    }

    @Test
    fun `quantities above the maximum are capped`() = runTest {
        updateCartQuantity(MUG.sku, 99)

        assertEquals(MAX_QUANTITY_PER_LINE, quantityOf(MUG.sku))
    }

    @Test
    fun `a failed write is reported`() = runTest {
        cartRepository.failure = CheckoutError.Network

        assertEquals(Outcome.Failure(CheckoutError.Network), updateCartQuantity(MUG.sku, 3))
    }
}
```

`ValidateAddressUseCaseTest.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.fake.FakeAddressRepository
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.model.AddressValidation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateAddressUseCaseTest {

    @Test
    fun `asks the repository about the given address`() = runTest {
        val undeliverable = AddressValidation.Undeliverable(AddressValidation.Reason.PO_BOX_NOT_ALLOWED)
        val repository = FakeAddressRepository(validation = Outcome.Success(undeliverable))

        val result = ValidateAddressUseCase(repository)(HOME.id)

        assertEquals(listOf(HOME.id), repository.validatedIds)
        assertEquals(Outcome.Success(undeliverable), result)
    }
}
```

`GetShippingQuotesUseCaseTest.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.fake.FakeShippingRepository
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.cartOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetShippingQuotesUseCaseTest {

    @Test
    fun `quotes the given address`() = runTest {
        val repository = FakeShippingRepository()

        val result = GetShippingQuotesUseCase(repository)(HOME.id, cartOf(MUG))

        assertEquals(listOf(HOME.id), repository.quotedAddressIds)
        assertEquals(Outcome.Success(listOf(STANDARD, PICKUP, EXPRESS)), result)
    }
}
```

`RedeemPromoUseCaseTest.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fake.FakePromoRepository
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.cartOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RedeemPromoUseCaseTest {

    @Test
    fun `codes are trimmed and upper-cased before redeeming`() = runTest {
        val repository = FakePromoRepository()

        RedeemPromoUseCase(repository)("  save5 ", cartOf(MUG))

        assertEquals(listOf("SAVE5"), repository.redeemedCodes)
    }

    @Test
    fun `a rejection is reported`() = runTest {
        val repository = FakePromoRepository(result = Outcome.Failure(CheckoutError.Promo.Expired))

        assertEquals(Outcome.Failure(CheckoutError.Promo.Expired), RedeemPromoUseCase(repository)("OLD", cartOf(MUG)))
    }
}
```

`PlaceOrderUseCaseTest.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.fake.FakeOrderRepository
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceOrderUseCaseTest {

    @Test
    fun `places the given request`() = runTest {
        val repository = FakeOrderRepository()
        val request = OrderRequest("checkout-1", HOME.id, STANDARD.id, VISA.id, promoCode = null)

        assertEquals(Outcome.Success(Order("order-1")), PlaceOrderUseCase(repository)(request))
        assertEquals(listOf(request), repository.placedRequests)
    }
}
```

`CalculateOrderTotalsUseCaseTest.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.feature.checkout.domain.fixture.CUSTOMER
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.MEMBER
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.eur
import com.example.feature.checkout.domain.model.OrderTotals
import com.example.feature.checkout.domain.model.Promo
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateOrderTotalsUseCaseTest {

    private val calculateOrderTotals = CalculateOrderTotalsUseCase()
    private val cart = cartOf(MUG) // 2 x 12.00

    @Test
    fun `a regular customer pays for shipping`() {
        assertEquals(eur("28.99"), calculateOrderTotals(cart, STANDARD, promo = null, customer = CUSTOMER).total)
    }

    @Test
    fun `members get standard shipping free but pay for express`() {
        assertEquals(eur("24.00"), calculateOrderTotals(cart, STANDARD, promo = null, customer = MEMBER).total)
        assertEquals(eur("36.99"), calculateOrderTotals(cart, EXPRESS, promo = null, customer = MEMBER).total)
    }

    @Test
    fun `the discount never exceeds the subtotal`() {
        val huge = Promo("ALL", eur("500.00"))

        assertEquals(eur("4.99"), calculateOrderTotals(cart, STANDARD, huge, CUSTOMER).total)
    }

    @Test
    fun `shipping costs nothing until an option is chosen`() {
        assertEquals(
            OrderTotals(subtotal = eur("24.00"), shipping = eur("0"), discount = eur("0"), total = eur("24.00")),
            calculateOrderTotals(cart, shipping = null, promo = null, customer = CUSTOMER),
        )
    }
}
```

- [ ] **Step 4: Run the tests to verify they fail**

Run: `./gradlew :feature:checkout:domain:test`
Expected: FAIL. `Unresolved reference 'ObserveCheckoutContextUseCase'` and the other use cases.

- [ ] **Step 5: Implement the use cases**

All files in `feature/checkout/domain/src/main/kotlin/com/example/feature/checkout/domain/usecase/`.

`ObserveCheckoutContextUseCase.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Address
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.model.FeatureFlag
import com.example.feature.checkout.domain.model.PaymentMethod
import com.example.feature.checkout.domain.repository.AddressRepository
import com.example.feature.checkout.domain.repository.CartRepository
import com.example.feature.checkout.domain.repository.CustomerRepository
import com.example.feature.checkout.domain.repository.FeatureFlagRepository
import com.example.feature.checkout.domain.repository.NetworkMonitor
import com.example.feature.checkout.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** Everything the checkout depends on, combined into one read model that updates whenever a source does. */
class ObserveCheckoutContextUseCase @Inject constructor(
    private val cartRepository: CartRepository,
    private val addressRepository: AddressRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val customerRepository: CustomerRepository,
    private val networkMonitor: NetworkMonitor,
    private val featureFlagRepository: FeatureFlagRepository,
) {
    operator fun invoke(): Flow<CheckoutOutcome<CheckoutContext>> {
        val environment = combine(
            customerRepository.observeCustomer(),
            networkMonitor.isOnline,
            featureFlagRepository.observe(FeatureFlag.EXPRESS_SHIPPING),
            ::Environment,
        )
        return combine(
            cartRepository.observeCart(),
            addressRepository.observeAddresses(),
            paymentMethodRepository.observePaymentMethods(),
            environment,
            ::checkoutContext,
        ).distinctUntilChanged()
    }

    private fun checkoutContext(
        cart: Cart,
        addresses: CheckoutOutcome<List<Address>>,
        paymentMethods: CheckoutOutcome<List<PaymentMethod>>,
        environment: Environment,
    ): CheckoutOutcome<CheckoutContext> = when (addresses) {
        is Outcome.Failure -> addresses
        is Outcome.Success -> when (paymentMethods) {
            is Outcome.Failure -> paymentMethods
            is Outcome.Success -> Outcome.Success(
                CheckoutContext(
                    cart = cart,
                    addresses = addresses.value,
                    paymentMethods = paymentMethods.value,
                    customer = environment.customer,
                    isOnline = environment.isOnline,
                    isExpressShippingEnabled = environment.isExpressShippingEnabled,
                )
            )
        }
    }

    private data class Environment(
        val customer: Customer,
        val isOnline: Boolean,
        val isExpressShippingEnabled: Boolean,
    )
}
```

`UpdateCartQuantityUseCase.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.repository.CartRepository
import com.example.feature.checkout.domain.rule.MAX_QUANTITY_PER_LINE
import javax.inject.Inject

class UpdateCartQuantityUseCase @Inject constructor(private val cartRepository: CartRepository) {

    /** A quantity of zero or less removes the line; anything above the per-line maximum is capped. */
    suspend operator fun invoke(sku: String, quantity: Int): CheckoutOutcome<Unit> =
        if (quantity <= 0) cartRepository.removeLine(sku)
        else cartRepository.setQuantity(sku, quantity.coerceAtMost(MAX_QUANTITY_PER_LINE))
}
```

`ValidateAddressUseCase.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.repository.AddressRepository
import javax.inject.Inject

class ValidateAddressUseCase @Inject constructor(private val addressRepository: AddressRepository) {

    suspend operator fun invoke(addressId: String): CheckoutOutcome<AddressValidation> =
        addressRepository.validate(addressId)
}
```

`GetShippingQuotesUseCase.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.repository.ShippingRepository
import javax.inject.Inject

class GetShippingQuotesUseCase @Inject constructor(private val shippingRepository: ShippingRepository) {

    suspend operator fun invoke(addressId: String, cart: Cart): CheckoutOutcome<List<ShippingOption>> =
        shippingRepository.quote(addressId, cart)
}
```

`RedeemPromoUseCase.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.Promo
import com.example.feature.checkout.domain.repository.PromoRepository
import javax.inject.Inject

class RedeemPromoUseCase @Inject constructor(private val promoRepository: PromoRepository) {

    /** Promo codes are case-insensitive and ignore surrounding spaces. */
    suspend operator fun invoke(rawCode: String, cart: Cart): CheckoutOutcome<Promo> =
        promoRepository.redeem(rawCode.trim().uppercase(), cart)
}
```

`PlaceOrderUseCase.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.domain.repository.OrderRepository
import javax.inject.Inject

class PlaceOrderUseCase @Inject constructor(private val orderRepository: OrderRepository) {

    suspend operator fun invoke(request: OrderRequest): CheckoutOutcome<Order> = orderRepository.place(request)
}
```

`CalculateOrderTotalsUseCase.kt`:

```kotlin
package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Money
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.model.OrderTotals
import com.example.feature.checkout.domain.model.Promo
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.rule.priceFor
import javax.inject.Inject

/**
 * The pricing of an order, shared by every place that shows a total. Deciding the total belongs
 * here; formatting it belongs to presentation.
 */
class CalculateOrderTotalsUseCase @Inject constructor() {

    operator fun invoke(cart: Cart, shipping: ShippingOption?, promo: Promo?, customer: Customer): OrderTotals {
        val zero = Money.zero(cart.currency)
        val subtotal = cart.subtotal
        val shippingCost = shipping?.priceFor(customer) ?: zero
        val discount = promo?.discount?.coerceAtMost(subtotal) ?: zero
        return OrderTotals(subtotal, shippingCost, discount, subtotal + shippingCost - discount)
    }
}
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :feature:checkout:domain:test`
Expected: PASS. 28 tests (10 rule tests and 18 use case tests).

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(checkout): add repository interfaces, test fakes and one use case per file"
```

---

### Task 5: Checkout data — demo backend and remote-backed repositories

**Files:**
- Modify: `settings.gradle.kts` (add `include(":feature:checkout:data")`)
- Create:
  - `feature/checkout/data/build.gradle.kts`
  - Under `feature/checkout/data/src/main/kotlin/com/example/feature/checkout/data/`:
    - `remote/dto/{AddressDto,AddressValidationDto,QuoteLineDto,QuoteRequestDto,ShippingOptionDto,RedeemPromoRequestDto,PromoDto,PaymentMethodDto,PlaceOrderRequestDto,OrderDto}.kt`
    - `remote/{CheckoutRemoteDataSource,CheckoutApiException,HttpStatus,DemoCheckoutRemoteDataSource}.kt`
    - `demo/DemoCheckoutData.kt`
    - `error/SafeCall.kt`
    - `mapper/{AddressMapper,AddressValidationMapper,ShippingOptionMapper,PromoMapper,PaymentMethodMapper,QuoteLineMapper,OrderMapper}.kt`
    - `repository/{AddressRepositoryImpl,ShippingRepositoryImpl,PromoRepositoryImpl,PaymentMethodRepositoryImpl,OrderRepositoryImpl}.kt`
- Test: under `feature/checkout/data/src/test/kotlin/com/example/feature/checkout/data/`:
  - `error/SafeCallTest.kt`
  - `mapper/AddressValidationMapperTest.kt`
  - `repository/{AddressRepositoryImplTest,ShippingRepositoryImplTest,PromoRepositoryImplTest,PaymentMethodRepositoryImplTest,OrderRepositoryImplTest}.kt`

**Interfaces:**
- Consumes: domain repository interfaces, models, `CheckoutError`, `CheckoutOutcome` (Tasks 3–4); `Money.ofMinor`, `Money.minorUnits` (Task 2); fixtures.
- Produces (all `internal`, used by Task 6's DI):
  - `CheckoutRemoteDataSource`
  - `DemoCheckoutRemoteDataSource(latencyMillis: Long)`
  - `DemoCheckoutData` ids and lists
  - `safeCall(mapApiError, call)`
  - `AddressRepositoryImpl`, `ShippingRepositoryImpl`, `PromoRepositoryImpl`, `PaymentMethodRepositoryImpl`, `OrderRepositoryImpl`, each `@Inject constructor(remote: CheckoutRemoteDataSource)`

- [ ] **Step 1: Register the module**

Append to `settings.gradle.kts`:

```kotlin
include(":feature:checkout:data")
```

`feature/checkout/data/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.shop.android.library)
    alias(libs.plugins.shop.hilt)
}

android {
    namespace = "com.example.feature.checkout.data"
}

dependencies {
    implementation(projects.feature.checkout.domain)

    testImplementation(testFixtures(projects.feature.checkout.domain))
}
```

- [ ] **Step 2: Write the DTOs and the remote contract**

All files in `feature/checkout/data/src/main/kotlin/com/example/feature/checkout/data/`.

`remote/dto/AddressDto.kt`:

```kotlin
package com.example.feature.checkout.data.remote.dto

internal data class AddressDto(
    val id: String,
    val label: String,
    val recipient: String,
    val line1: String,
    val city: String,
    val postalCode: String,
    val isDefault: Boolean,
)
```

`remote/dto/AddressValidationDto.kt`:

```kotlin
package com.example.feature.checkout.data.remote.dto

internal data class AddressValidationDto(val deliverable: Boolean, val reasonCode: String?)
```

`remote/dto/QuoteLineDto.kt`:

```kotlin
package com.example.feature.checkout.data.remote.dto

internal data class QuoteLineDto(
    val sku: String,
    val quantity: Int,
    val unitPriceMinor: Long,
    val currency: String,
)
```

`remote/dto/QuoteRequestDto.kt`:

```kotlin
package com.example.feature.checkout.data.remote.dto

internal data class QuoteRequestDto(val addressId: String, val lines: List<QuoteLineDto>)
```

`remote/dto/ShippingOptionDto.kt`:

```kotlin
package com.example.feature.checkout.data.remote.dto

internal data class ShippingOptionDto(
    val id: String,
    val label: String,
    val priceMinor: Long,
    val currency: String,
    val etaDays: Int,
    val express: Boolean,
)
```

`remote/dto/RedeemPromoRequestDto.kt`:

```kotlin
package com.example.feature.checkout.data.remote.dto

internal data class RedeemPromoRequestDto(val code: String, val lines: List<QuoteLineDto>)
```

`remote/dto/PromoDto.kt`:

```kotlin
package com.example.feature.checkout.data.remote.dto

internal data class PromoDto(val code: String, val discountMinor: Long, val currency: String)
```

`remote/dto/PaymentMethodDto.kt`:

```kotlin
package com.example.feature.checkout.data.remote.dto

internal data class PaymentMethodDto(
    val id: String,
    val brand: String,
    val last4: String,
    val isDefault: Boolean,
)
```

`remote/dto/PlaceOrderRequestDto.kt`:

```kotlin
package com.example.feature.checkout.data.remote.dto

internal data class PlaceOrderRequestDto(
    val addressId: String,
    val shippingOptionId: String,
    val paymentMethodId: String,
    val promoCode: String?,
)
```

`remote/dto/OrderDto.kt`:

```kotlin
package com.example.feature.checkout.data.remote.dto

internal data class OrderDto(val id: String)
```

`remote/CheckoutRemoteDataSource.kt`:

```kotlin
package com.example.feature.checkout.data.remote

import com.example.feature.checkout.data.remote.dto.AddressDto
import com.example.feature.checkout.data.remote.dto.AddressValidationDto
import com.example.feature.checkout.data.remote.dto.OrderDto
import com.example.feature.checkout.data.remote.dto.PaymentMethodDto
import com.example.feature.checkout.data.remote.dto.PlaceOrderRequestDto
import com.example.feature.checkout.data.remote.dto.PromoDto
import com.example.feature.checkout.data.remote.dto.QuoteRequestDto
import com.example.feature.checkout.data.remote.dto.RedeemPromoRequestDto
import com.example.feature.checkout.data.remote.dto.ShippingOptionDto

/**
 * The checkout backend as the app sees it. Implementations report error responses as
 * [CheckoutApiException] and connectivity failures as [java.io.IOException].
 */
internal interface CheckoutRemoteDataSource {
    suspend fun addresses(): List<AddressDto>
    suspend fun validateAddress(addressId: String): AddressValidationDto
    suspend fun shippingQuotes(request: QuoteRequestDto): List<ShippingOptionDto>
    suspend fun redeemPromo(request: RedeemPromoRequestDto): PromoDto
    suspend fun paymentMethods(): List<PaymentMethodDto>
    suspend fun placeOrder(idempotencyKey: String, request: PlaceOrderRequestDto): OrderDto
}
```

`remote/CheckoutApiException.kt`:

```kotlin
package com.example.feature.checkout.data.remote

/** A failed backend call, carrying the status code the backend answered with. */
internal class CheckoutApiException(val code: Int) : Exception("HTTP $code")
```

`remote/HttpStatus.kt`:

```kotlin
package com.example.feature.checkout.data.remote

/** The status codes the checkout backend uses. */
internal object HttpStatus {
    const val UNAUTHORIZED = 401
    const val PAYMENT_REQUIRED = 402
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val CONFLICT = 409
    const val GONE = 410
    const val UNPROCESSABLE_ENTITY = 422
}
```

`demo/DemoCheckoutData.kt`:

```kotlin
package com.example.feature.checkout.data.demo

import com.example.feature.checkout.data.remote.dto.AddressDto
import com.example.feature.checkout.data.remote.dto.PaymentMethodDto
import com.example.feature.checkout.data.remote.dto.ShippingOptionDto

/** The demo backend's fixed data. Ids are stable so scripted scenarios can refer to them. */
internal object DemoCheckoutData {
    const val CURRENCY = "EUR"

    const val HOME_ID = "home"
    const val OFFICE_ID = "office"
    const val PO_BOX_ID = "po-box"
    const val ISLAND_ID = "island"

    const val VISA_ID = "visa"
    const val DECLINED_CARD_ID = "mastercard"
    const val CONFIRMATION_CARD_ID = "amex"

    val addresses: List<AddressDto> = listOf(
        AddressDto(HOME_ID, "Home", "Alex Doe", "Calle Larios 1", "Málaga", "29005", isDefault = true),
        AddressDto(OFFICE_ID, "Office", "Alex Doe", "Av. de Andalucía 10", "Málaga", "29007", isDefault = false),
        AddressDto(PO_BOX_ID, "PO Box", "Alex Doe", "PO Box 42", "Málaga", "29080", isDefault = false),
        AddressDto(ISLAND_ID, "Island cabin", "Alex Doe", "Isla de Tabarca 3", "Alicante", "03138", isDefault = false),
    )

    val shippingOptions: List<ShippingOptionDto> = listOf(
        ShippingOptionDto("standard", "Standard", priceMinor = 499, CURRENCY, etaDays = 3, express = false),
        ShippingOptionDto("pickup", "Pickup point", priceMinor = 299, CURRENCY, etaDays = 4, express = false),
        ShippingOptionDto("express", "Express", priceMinor = 1299, CURRENCY, etaDays = 1, express = true),
    )

    val paymentMethods: List<PaymentMethodDto> = listOf(
        PaymentMethodDto(VISA_ID, "Visa", "4242", isDefault = true),
        PaymentMethodDto(DECLINED_CARD_ID, "Mastercard", "0002", isDefault = false),
        PaymentMethodDto(CONFIRMATION_CARD_ID, "Amex", "3155", isDefault = false),
    )
}
```

`remote/DemoCheckoutRemoteDataSource.kt`:

```kotlin
package com.example.feature.checkout.data.remote

import com.example.feature.checkout.data.demo.DemoCheckoutData
import com.example.feature.checkout.data.remote.dto.AddressDto
import com.example.feature.checkout.data.remote.dto.AddressValidationDto
import com.example.feature.checkout.data.remote.dto.OrderDto
import com.example.feature.checkout.data.remote.dto.PaymentMethodDto
import com.example.feature.checkout.data.remote.dto.PlaceOrderRequestDto
import com.example.feature.checkout.data.remote.dto.PromoDto
import com.example.feature.checkout.data.remote.dto.QuoteRequestDto
import com.example.feature.checkout.data.remote.dto.RedeemPromoRequestDto
import com.example.feature.checkout.data.remote.dto.ShippingOptionDto
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * An in-memory backend with simulated latency and scripted outcomes, so the app runs end to end
 * without a server.
 *
 * - Promo codes: SAVE5 takes €5 off; BIG50 takes €50 off orders of at least €100; EXPIRED has
 *   expired; any other code is invalid.
 * - Payment: the Mastercard is declined and the Amex needs bank confirmation.
 * - Addresses: the PO box and the island cabin cannot be delivered to.
 * - Orders: placing twice with the same idempotency key returns the same order.
 */
internal class DemoCheckoutRemoteDataSource(private val latencyMillis: Long) : CheckoutRemoteDataSource {

    private val placedOrders = ConcurrentHashMap<String, OrderDto>()
    private val nextOrderNumber = AtomicInteger(FIRST_ORDER_NUMBER)

    override suspend fun addresses(): List<AddressDto> = respond { DemoCheckoutData.addresses }

    override suspend fun validateAddress(addressId: String): AddressValidationDto = respond {
        when (addressId) {
            DemoCheckoutData.HOME_ID, DemoCheckoutData.OFFICE_ID ->
                AddressValidationDto(deliverable = true, reasonCode = null)
            DemoCheckoutData.PO_BOX_ID -> AddressValidationDto(deliverable = false, reasonCode = "PO_BOX")
            DemoCheckoutData.ISLAND_ID -> AddressValidationDto(deliverable = false, reasonCode = "OUTSIDE_AREA")
            else -> throw CheckoutApiException(HttpStatus.NOT_FOUND)
        }
    }

    override suspend fun shippingQuotes(request: QuoteRequestDto): List<ShippingOptionDto> = respond {
        if (request.lines.isEmpty()) emptyList() else DemoCheckoutData.shippingOptions
    }

    override suspend fun redeemPromo(request: RedeemPromoRequestDto): PromoDto = respond {
        val subtotalMinor = request.lines.sumOf { it.unitPriceMinor * it.quantity }
        when (request.code) {
            "SAVE5" -> PromoDto(code = "SAVE5", discountMinor = 500, currency = DemoCheckoutData.CURRENCY)
            "BIG50" ->
                if (subtotalMinor >= BIG50_MINIMUM_MINOR) {
                    PromoDto(code = "BIG50", discountMinor = 5_000, currency = DemoCheckoutData.CURRENCY)
                } else {
                    throw CheckoutApiException(HttpStatus.UNPROCESSABLE_ENTITY)
                }
            "EXPIRED" -> throw CheckoutApiException(HttpStatus.GONE)
            else -> throw CheckoutApiException(HttpStatus.NOT_FOUND)
        }
    }

    override suspend fun paymentMethods(): List<PaymentMethodDto> = respond { DemoCheckoutData.paymentMethods }

    override suspend fun placeOrder(idempotencyKey: String, request: PlaceOrderRequestDto): OrderDto = respond {
        when (request.paymentMethodId) {
            DemoCheckoutData.DECLINED_CARD_ID -> throw CheckoutApiException(HttpStatus.PAYMENT_REQUIRED)
            DemoCheckoutData.CONFIRMATION_CARD_ID -> throw CheckoutApiException(HttpStatus.FORBIDDEN)
            else -> placedOrders.getOrPut(idempotencyKey) { OrderDto(id = "ORD-${nextOrderNumber.getAndIncrement()}") }
        }
    }

    private suspend fun <T> respond(response: () -> T): T {
        delay(latencyMillis)
        return response()
    }

    private companion object {
        const val FIRST_ORDER_NUMBER = 1001
        const val BIG50_MINIMUM_MINOR = 10_000L
    }
}
```

- [ ] **Step 3: Write the failing tests**

All files in `feature/checkout/data/src/test/kotlin/com/example/feature/checkout/data/`.

`error/SafeCallTest.kt`:

```kotlin
package com.example.feature.checkout.data.error

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.remote.CheckoutApiException
import com.example.feature.checkout.domain.error.CheckoutError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class SafeCallTest {

    @Test
    fun `a successful call is a success`() = runTest {
        assertEquals(Outcome.Success(42), safeCall { 42 })
    }

    @Test
    fun `an IO failure is a network error`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.Network), safeCall<Int> { throw IOException("offline") })
    }

    @Test
    fun `status codes without an endpoint meaning map to common errors`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.Unauthorized), safeCall<Int> { throw CheckoutApiException(401) })
        assertEquals(Outcome.Failure(CheckoutError.Unauthorized), safeCall<Int> { throw CheckoutApiException(403) })
        assertEquals(Outcome.Failure(CheckoutError.NotFound), safeCall<Int> { throw CheckoutApiException(404) })
        assertEquals(Outcome.Failure(CheckoutError.PriceChanged), safeCall<Int> { throw CheckoutApiException(409) })
        assertEquals(Outcome.Failure(CheckoutError.Unexpected("HTTP 500")), safeCall<Int> { throw CheckoutApiException(500) })
    }

    @Test
    fun `an endpoint meaning wins over the common one`() = runTest {
        val outcome = safeCall<Int>(mapApiError = { CheckoutError.Promo.Expired }) { throw CheckoutApiException(410) }

        assertEquals(Outcome.Failure(CheckoutError.Promo.Expired), outcome)
    }

    @Test
    fun `cancellation is never swallowed`() {
        assertThrows(CancellationException::class.java) {
            runBlocking { safeCall<Int> { throw CancellationException("stopped") } }
        }
    }
}
```

`mapper/AddressValidationMapperTest.kt`:

```kotlin
package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.remote.dto.AddressValidationDto
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.model.AddressValidation.Reason
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressValidationMapperTest {

    @Test
    fun `a deliverable answer maps to Deliverable`() {
        assertEquals(AddressValidation.Deliverable, AddressValidationDto(deliverable = true, reasonCode = null).toDomain())
    }

    @Test
    fun `reason codes map to reasons`() {
        assertEquals(undeliverable(Reason.PO_BOX_NOT_ALLOWED), refused("PO_BOX"))
        assertEquals(undeliverable(Reason.INCOMPLETE), refused("INCOMPLETE"))
        assertEquals(undeliverable(Reason.OUTSIDE_SERVICE_AREA), refused("OUTSIDE_AREA"))
        assertEquals(undeliverable(Reason.OUTSIDE_SERVICE_AREA), refused(null))
    }

    private fun refused(reasonCode: String?) = AddressValidationDto(deliverable = false, reasonCode = reasonCode).toDomain()

    private fun undeliverable(reason: Reason) = AddressValidation.Undeliverable(reason)
}
```

`repository/AddressRepositoryImplTest.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.demo.DemoCheckoutData
import com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.model.AddressValidation.Reason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressRepositoryImplTest {

    private val repository = AddressRepositoryImpl(DemoCheckoutRemoteDataSource(latencyMillis = 0))

    @Test
    fun `lists the demo addresses with Home as the default`() = runTest {
        val addresses = (repository.observeAddresses().first() as Outcome.Success).value

        assertEquals(listOf("Home", "Office", "PO Box", "Island cabin"), addresses.map { it.label })
        assertEquals(listOf(DemoCheckoutData.HOME_ID), addresses.filter { it.isDefault }.map { it.id })
    }

    @Test
    fun `home and office are deliverable`() = runTest {
        assertEquals(Outcome.Success(AddressValidation.Deliverable), repository.validate(DemoCheckoutData.HOME_ID))
        assertEquals(Outcome.Success(AddressValidation.Deliverable), repository.validate(DemoCheckoutData.OFFICE_ID))
    }

    @Test
    fun `the PO box is refused`() = runTest {
        assertEquals(
            Outcome.Success(AddressValidation.Undeliverable(Reason.PO_BOX_NOT_ALLOWED)),
            repository.validate(DemoCheckoutData.PO_BOX_ID),
        )
    }

    @Test
    fun `the island cabin is outside the service area`() = runTest {
        assertEquals(
            Outcome.Success(AddressValidation.Undeliverable(Reason.OUTSIDE_SERVICE_AREA)),
            repository.validate(DemoCheckoutData.ISLAND_ID),
        )
    }

    @Test
    fun `an unknown address is not found`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.NotFound), repository.validate("nowhere"))
    }
}
```

`repository/ShippingRepositoryImplTest.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.demo.DemoCheckoutData
import com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.model.ShippingOption
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ShippingRepositoryImplTest {

    private val repository = ShippingRepositoryImpl(DemoCheckoutRemoteDataSource(latencyMillis = 0))

    @Test
    fun `a cart with items gets three options`() = runTest {
        assertEquals(
            Outcome.Success(listOf(STANDARD, PICKUP, EXPRESS)),
            repository.quote(DemoCheckoutData.HOME_ID, cartOf(MUG)),
        )
    }

    @Test
    fun `an empty cart gets no options`() = runTest {
        assertEquals(Outcome.Success(emptyList<ShippingOption>()), repository.quote(DemoCheckoutData.HOME_ID, cartOf()))
    }
}
```

`repository/PromoRepositoryImplTest.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.eur
import com.example.feature.checkout.domain.model.Promo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PromoRepositoryImplTest {

    private val repository = PromoRepositoryImpl(DemoCheckoutRemoteDataSource(latencyMillis = 0))
    private val smallCart = cartOf(MUG) // 24.00
    private val bigCart = cartOf(MUG.copy(quantity = 10)) // 120.00

    @Test
    fun `SAVE5 takes five euros off`() = runTest {
        assertEquals(Outcome.Success(Promo("SAVE5", eur("5.00"))), repository.redeem("SAVE5", smallCart))
    }

    @Test
    fun `BIG50 needs an order of at least 100 euros`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.Promo.MinimumNotReached), repository.redeem("BIG50", smallCart))
        assertEquals(Outcome.Success(Promo("BIG50", eur("50.00"))), repository.redeem("BIG50", bigCart))
    }

    @Test
    fun `EXPIRED has expired`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.Promo.Expired), repository.redeem("EXPIRED", smallCart))
    }

    @Test
    fun `unknown codes are invalid`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.Promo.Invalid), repository.redeem("NOPE", smallCart))
    }
}
```

`repository/PaymentMethodRepositoryImplTest.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource
import com.example.feature.checkout.domain.fixture.MASTERCARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.model.PaymentMethod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentMethodRepositoryImplTest {

    @Test
    fun `lists the three demo cards with Visa as the default`() = runTest {
        val repository = PaymentMethodRepositoryImpl(DemoCheckoutRemoteDataSource(latencyMillis = 0))

        assertEquals(
            Outcome.Success(listOf(VISA, MASTERCARD, PaymentMethod("amex", "Amex", "3155", isDefault = false))),
            repository.observePaymentMethods().first(),
        )
    }
}
```

`repository/OrderRepositoryImplTest.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.demo.DemoCheckoutData
import com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OrderRepositoryImplTest {

    private val repository = OrderRepositoryImpl(DemoCheckoutRemoteDataSource(latencyMillis = 0))

    private fun request(checkoutId: String = "checkout-1", paymentMethodId: String = DemoCheckoutData.VISA_ID) =
        OrderRequest(checkoutId, DemoCheckoutData.HOME_ID, "standard", paymentMethodId, promoCode = null)

    @Test
    fun `a valid card places the order`() = runTest {
        assertEquals(Outcome.Success(Order("ORD-1001")), repository.place(request()))
    }

    @Test
    fun `retrying the same request returns the same order`() = runTest {
        val first = repository.place(request())

        assertEquals(first, repository.place(request()))
    }

    @Test
    fun `the same order in a new checkout is a new order`() = runTest {
        val first = repository.place(request(checkoutId = "checkout-1"))

        assertNotEquals(first, repository.place(request(checkoutId = "checkout-2")))
    }

    @Test
    fun `the Mastercard is declined`() = runTest {
        assertEquals(
            Outcome.Failure(CheckoutError.Payment.Declined),
            repository.place(request(paymentMethodId = DemoCheckoutData.DECLINED_CARD_ID)),
        )
    }

    @Test
    fun `the Amex needs bank confirmation`() = runTest {
        assertEquals(
            Outcome.Failure(CheckoutError.Payment.RequiresAction),
            repository.place(request(paymentMethodId = DemoCheckoutData.CONFIRMATION_CARD_ID)),
        )
    }
}
```

- [ ] **Step 4: Run the tests to verify they fail**

Run: `./gradlew :feature:checkout:data:testDebugUnitTest`
Expected: FAIL. `Unresolved reference 'safeCall'`, `'toDomain'`, `'AddressRepositoryImpl'` and the other repositories.

- [ ] **Step 5: Implement error mapping, mappers and repositories**

All files in `feature/checkout/data/src/main/kotlin/com/example/feature/checkout/data/`.

`error/SafeCall.kt`:

```kotlin
package com.example.feature.checkout.data.error

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.remote.CheckoutApiException
import com.example.feature.checkout.data.remote.HttpStatus
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * The single place where exceptions become [CheckoutError]s.
 *
 * @param mapApiError the meaning of a status code for one endpoint (for example 410 on redeem means
 * the promo expired). When it returns null, [toCheckoutError] decides.
 */
internal suspend inline fun <T> safeCall(
    noinline mapApiError: (CheckoutApiException) -> CheckoutError? = { null },
    crossinline call: suspend () -> T,
): CheckoutOutcome<T> = try {
    Outcome.Success(call())
} catch (e: CancellationException) {
    throw e
} catch (e: CheckoutApiException) {
    Outcome.Failure(mapApiError(e) ?: e.toCheckoutError())
} catch (e: IOException) {
    Outcome.Failure(CheckoutError.Network)
}

/** What a status code means when the endpoint gives it no specific meaning. */
internal fun CheckoutApiException.toCheckoutError(): CheckoutError = when (code) {
    HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> CheckoutError.Unauthorized
    HttpStatus.NOT_FOUND -> CheckoutError.NotFound
    HttpStatus.CONFLICT -> CheckoutError.PriceChanged
    else -> CheckoutError.Unexpected("HTTP $code")
}
```

`mapper/AddressMapper.kt`:

```kotlin
package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.remote.dto.AddressDto
import com.example.feature.checkout.domain.model.Address

internal fun AddressDto.toDomain(): Address = Address(id, label, recipient, line1, city, postalCode, isDefault)
```

`mapper/AddressValidationMapper.kt`:

```kotlin
package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.remote.dto.AddressValidationDto
import com.example.feature.checkout.domain.model.AddressValidation

internal fun AddressValidationDto.toDomain(): AddressValidation =
    if (deliverable) AddressValidation.Deliverable
    else AddressValidation.Undeliverable(reasonCode.toReason())

private fun String?.toReason(): AddressValidation.Reason = when (this) {
    "PO_BOX" -> AddressValidation.Reason.PO_BOX_NOT_ALLOWED
    "INCOMPLETE" -> AddressValidation.Reason.INCOMPLETE
    else -> AddressValidation.Reason.OUTSIDE_SERVICE_AREA
}
```

`mapper/ShippingOptionMapper.kt`:

```kotlin
package com.example.feature.checkout.data.mapper

import com.example.core.domain.Money
import com.example.feature.checkout.data.remote.dto.ShippingOptionDto
import com.example.feature.checkout.domain.model.ShippingOption

internal fun ShippingOptionDto.toDomain(): ShippingOption =
    ShippingOption(id, label, Money.ofMinor(priceMinor, currency), etaDays, express)
```

`mapper/PromoMapper.kt`:

```kotlin
package com.example.feature.checkout.data.mapper

import com.example.core.domain.Money
import com.example.feature.checkout.data.remote.dto.PromoDto
import com.example.feature.checkout.domain.model.Promo

internal fun PromoDto.toDomain(): Promo = Promo(code, Money.ofMinor(discountMinor, currency))
```

`mapper/PaymentMethodMapper.kt`:

```kotlin
package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.remote.dto.PaymentMethodDto
import com.example.feature.checkout.domain.model.PaymentMethod

internal fun PaymentMethodDto.toDomain(): PaymentMethod = PaymentMethod(id, brand, last4, isDefault)
```

`mapper/QuoteLineMapper.kt`:

```kotlin
package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.remote.dto.QuoteLineDto
import com.example.feature.checkout.domain.model.Cart

internal fun Cart.toQuoteLines(): List<QuoteLineDto> = lines.map { line ->
    QuoteLineDto(line.sku, line.quantity, line.unitPrice.minorUnits, line.unitPrice.currency.currencyCode)
}
```

`mapper/OrderMapper.kt`:

```kotlin
package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.remote.dto.OrderDto
import com.example.feature.checkout.data.remote.dto.PlaceOrderRequestDto
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest

internal fun OrderRequest.toDto(): PlaceOrderRequestDto =
    PlaceOrderRequestDto(addressId, shippingOptionId, paymentMethodId, promoCode)

internal fun OrderDto.toDomain(): Order = Order(id)
```

`repository/AddressRepositoryImpl.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.feature.checkout.data.error.safeCall
import com.example.feature.checkout.data.mapper.toDomain
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Address
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.repository.AddressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class AddressRepositoryImpl @Inject constructor(
    private val remote: CheckoutRemoteDataSource,
) : AddressRepository {

    override fun observeAddresses(): Flow<CheckoutOutcome<List<Address>>> = flow {
        emit(safeCall { remote.addresses().map { it.toDomain() } })
    }

    override suspend fun validate(addressId: String): CheckoutOutcome<AddressValidation> =
        safeCall { remote.validateAddress(addressId).toDomain() }
}
```

`repository/ShippingRepositoryImpl.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.feature.checkout.data.error.safeCall
import com.example.feature.checkout.data.mapper.toDomain
import com.example.feature.checkout.data.mapper.toQuoteLines
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.data.remote.dto.QuoteRequestDto
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.repository.ShippingRepository
import javax.inject.Inject

internal class ShippingRepositoryImpl @Inject constructor(
    private val remote: CheckoutRemoteDataSource,
) : ShippingRepository {

    override suspend fun quote(addressId: String, cart: Cart): CheckoutOutcome<List<ShippingOption>> = safeCall {
        remote.shippingQuotes(QuoteRequestDto(addressId, cart.toQuoteLines())).map { it.toDomain() }
    }
}
```

`repository/PromoRepositoryImpl.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.feature.checkout.data.error.safeCall
import com.example.feature.checkout.data.mapper.toDomain
import com.example.feature.checkout.data.mapper.toQuoteLines
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.data.remote.HttpStatus
import com.example.feature.checkout.data.remote.dto.RedeemPromoRequestDto
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.Promo
import com.example.feature.checkout.domain.repository.PromoRepository
import javax.inject.Inject

internal class PromoRepositoryImpl @Inject constructor(
    private val remote: CheckoutRemoteDataSource,
) : PromoRepository {

    override suspend fun redeem(code: String, cart: Cart): CheckoutOutcome<Promo> = safeCall(
        mapApiError = { error ->
            when (error.code) {
                HttpStatus.NOT_FOUND -> CheckoutError.Promo.Invalid
                HttpStatus.GONE -> CheckoutError.Promo.Expired
                HttpStatus.UNPROCESSABLE_ENTITY -> CheckoutError.Promo.MinimumNotReached
                else -> null
            }
        },
    ) {
        remote.redeemPromo(RedeemPromoRequestDto(code, cart.toQuoteLines())).toDomain()
    }
}
```

`repository/PaymentMethodRepositoryImpl.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.feature.checkout.data.error.safeCall
import com.example.feature.checkout.data.mapper.toDomain
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.PaymentMethod
import com.example.feature.checkout.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class PaymentMethodRepositoryImpl @Inject constructor(
    private val remote: CheckoutRemoteDataSource,
) : PaymentMethodRepository {

    override fun observePaymentMethods(): Flow<CheckoutOutcome<List<PaymentMethod>>> = flow {
        emit(safeCall { remote.paymentMethods().map { it.toDomain() } })
    }
}
```

`repository/OrderRepositoryImpl.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.feature.checkout.data.error.safeCall
import com.example.feature.checkout.data.mapper.toDomain
import com.example.feature.checkout.data.mapper.toDto
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.data.remote.HttpStatus
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.domain.repository.OrderRepository
import java.util.UUID
import javax.inject.Inject

internal class OrderRepositoryImpl @Inject constructor(
    private val remote: CheckoutRemoteDataSource,
) : OrderRepository {

    override suspend fun place(request: OrderRequest): CheckoutOutcome<Order> = safeCall(
        mapApiError = { error ->
            when (error.code) {
                HttpStatus.PAYMENT_REQUIRED -> CheckoutError.Payment.Declined
                HttpStatus.FORBIDDEN -> CheckoutError.Payment.RequiresAction
                HttpStatus.UNPROCESSABLE_ENTITY -> CheckoutError.OutOfStock
                else -> null
            }
        },
    ) {
        remote.placeOrder(request.idempotencyKey(), request.toDto()).toDomain()
    }

    /**
     * The same request within one checkout always yields the same key, so a retry after a timeout
     * cannot charge twice. A new checkout has a new checkout id, so a later identical order is a
     * new order.
     */
    private fun OrderRequest.idempotencyKey(): String {
        val fields = listOf(checkoutId, addressId, shippingOptionId, paymentMethodId, promoCode.orEmpty())
        return UUID.nameUUIDFromBytes(fields.joinToString("|").toByteArray()).toString()
    }
}
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :feature:checkout:data:testDebugUnitTest`
Expected: PASS. 24 tests.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(checkout): add the demo backend and remote-backed repositories"
```

---

### Task 6: Checkout data — local cart, platform adapters and DI

**Files:**
- Create:
  - `feature/checkout/data/src/main/AndroidManifest.xml`
  - Under `feature/checkout/data/src/main/kotlin/com/example/feature/checkout/data/`:
    - `local/{CartLineRecord,CartDataSource,InMemoryCartDataSource}.kt`
    - `mapper/CartMapper.kt`
    - `repository/{CartRepositoryImpl,DemoCustomerRepository,DemoFeatureFlagRepository}.kt`
    - `network/ConnectivityNetworkMonitor.kt`
    - `di/{StoreCurrency,DataSourceModule,RepositoryModule}.kt`
- Test: under `feature/checkout/data/src/test/kotlin/com/example/feature/checkout/data/`:
  - `local/InMemoryCartDataSourceTest.kt`
  - `mapper/CartMapperTest.kt`
  - `repository/{CartRepositoryImplTest,DemoCustomerRepositoryTest,DemoFeatureFlagRepositoryTest}.kt`

**Interfaces:**
- Consumes: Task 5's data sources and repositories, plus the domain repository interfaces.
- Produces: a complete Hilt binding of all nine domain repository interfaces, installed in `SingletonComponent`. Only `:app` sees it.

- [ ] **Step 1: Write the failing tests**

All files in `feature/checkout/data/src/test/kotlin/com/example/feature/checkout/data/`.

`local/InMemoryCartDataSourceTest.kt`:

```kotlin
package com.example.feature.checkout.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryCartDataSourceTest {

    private val source = InMemoryCartDataSource()

    @Test
    fun `starts with the demo cart`() = runTest {
        assertEquals(InMemoryCartDataSource.DEMO_LINES, source.observeLines().first())
    }

    @Test
    fun `updates a known line`() = runTest {
        assertTrue(source.updateQuantity("mug", 5))

        assertEquals(5, source.observeLines().first().first { it.sku == "mug" }.quantity)
    }

    @Test
    fun `deletes a known line`() = runTest {
        assertTrue(source.delete("mug"))

        assertEquals(listOf("tee"), source.observeLines().first().map { it.sku })
    }

    @Test
    fun `unknown lines are reported and nothing changes`() = runTest {
        assertFalse(source.updateQuantity("nope", 1))
        assertFalse(source.delete("nope"))
        assertEquals(InMemoryCartDataSource.DEMO_LINES, source.observeLines().first())
    }
}
```

`mapper/CartMapperTest.kt`:

```kotlin
package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.local.CartLineRecord
import com.example.feature.checkout.data.remote.dto.QuoteLineDto
import com.example.feature.checkout.domain.fixture.EUR
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.eur
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.CartLine
import org.junit.Assert.assertEquals
import org.junit.Test

class CartMapperTest {

    @Test
    fun `records become cart lines priced from minor units`() {
        val cart = listOf(CartLineRecord("mug", "Coffee mug", 1200, "EUR", 2)).toCart(EUR)

        assertEquals(cartOf(CartLine("mug", "Coffee mug", eur("12.00"), 2)), cart)
    }

    @Test
    fun `an empty cart uses the store currency`() {
        assertEquals(Cart(emptyList(), EUR), emptyList<CartLineRecord>().toCart(EUR))
    }

    @Test
    fun `cart lines become quote lines`() {
        assertEquals(listOf(QuoteLineDto("mug", 2, 1200, "EUR")), cartOf(MUG).toQuoteLines())
    }
}
```

`repository/CartRepositoryImplTest.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.local.InMemoryCartDataSource
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.EUR
import com.example.feature.checkout.domain.fixture.eur
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CartRepositoryImplTest {

    private val repository = CartRepositoryImpl(InMemoryCartDataSource(), EUR)

    @Test
    fun `the demo cart holds a mug and a T-shirt`() = runTest {
        val cart = repository.observeCart().first()

        assertEquals(listOf("mug", "tee"), cart.lines.map { it.sku })
        assertEquals(eur("44.00"), cart.subtotal)
    }

    @Test
    fun `setting a quantity updates the cart`() = runTest {
        assertEquals(Outcome.Success(Unit), repository.setQuantity("mug", 3))

        assertEquals(3, repository.observeCart().first().lines.first { it.sku == "mug" }.quantity)
    }

    @Test
    fun `removing a line drops it from the cart`() = runTest {
        assertEquals(Outcome.Success(Unit), repository.removeLine("mug"))

        assertEquals(listOf("tee"), repository.observeCart().first().lines.map { it.sku })
    }

    @Test
    fun `unknown lines are not found`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.NotFound), repository.setQuantity("nope", 1))
        assertEquals(Outcome.Failure(CheckoutError.NotFound), repository.removeLine("nope"))
    }
}
```

`repository/DemoCustomerRepositoryTest.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoCustomerRepositoryTest {

    @Test
    fun `the demo customer is a member`() = runTest {
        assertTrue(DemoCustomerRepository().observeCustomer().first().isMember)
    }
}
```

`repository/DemoFeatureFlagRepositoryTest.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.feature.checkout.domain.model.FeatureFlag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoFeatureFlagRepositoryTest {

    @Test
    fun `express shipping is switched on in the demo`() = runTest {
        assertTrue(DemoFeatureFlagRepository().observe(FeatureFlag.EXPRESS_SHIPPING).first())
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :feature:checkout:data:testDebugUnitTest`
Expected: FAIL. `Unresolved reference 'InMemoryCartDataSource'`, `'CartLineRecord'`, `'toCart'`, `'CartRepositoryImpl'`, `'DemoCustomerRepository'`, `'DemoFeatureFlagRepository'`.

- [ ] **Step 3: Implement the local cart and its repository**

All files in `feature/checkout/data/src/main/kotlin/com/example/feature/checkout/data/`.

`local/CartLineRecord.kt`:

```kotlin
package com.example.feature.checkout.data.local

internal data class CartLineRecord(
    val sku: String,
    val name: String,
    val unitPriceMinor: Long,
    val currency: String,
    val quantity: Int,
)
```

`local/CartDataSource.kt`:

```kotlin
package com.example.feature.checkout.data.local

import kotlinx.coroutines.flow.Flow

/** Where the cart is kept on the device. In a production app, Room would implement this. */
internal interface CartDataSource {
    fun observeLines(): Flow<List<CartLineRecord>>

    /** @return false when no line has [sku]. */
    suspend fun updateQuantity(sku: String, quantity: Int): Boolean

    /** @return false when no line has [sku]. */
    suspend fun delete(sku: String): Boolean
}
```

`local/InMemoryCartDataSource.kt`:

```kotlin
package com.example.feature.checkout.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The cart kept in memory, seeded with two demo lines. It starts over when the process restarts. */
internal class InMemoryCartDataSource(initialLines: List<CartLineRecord> = DEMO_LINES) : CartDataSource {

    private val lines = MutableStateFlow(initialLines)

    override fun observeLines(): Flow<List<CartLineRecord>> = lines.asStateFlow()

    override suspend fun updateQuantity(sku: String, quantity: Int): Boolean = edit(sku) { current ->
        current.map { if (it.sku == sku) it.copy(quantity = quantity) else it }
    }

    override suspend fun delete(sku: String): Boolean = edit(sku) { current ->
        current.filterNot { it.sku == sku }
    }

    private fun edit(sku: String, change: (List<CartLineRecord>) -> List<CartLineRecord>): Boolean {
        if (lines.value.none { it.sku == sku }) return false
        lines.update(change)
        return true
    }

    companion object {
        val DEMO_LINES: List<CartLineRecord> = listOf(
            CartLineRecord(sku = "mug", name = "Coffee mug", unitPriceMinor = 1200, currency = "EUR", quantity = 2),
            CartLineRecord(sku = "tee", name = "T-shirt", unitPriceMinor = 2000, currency = "EUR", quantity = 1),
        )
    }
}
```

`mapper/CartMapper.kt`:

```kotlin
package com.example.feature.checkout.data.mapper

import com.example.core.domain.Money
import com.example.feature.checkout.data.local.CartLineRecord
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.CartLine
import java.util.Currency

internal fun List<CartLineRecord>.toCart(fallbackCurrency: Currency): Cart = Cart(
    lines = map { CartLine(it.sku, it.name, Money.ofMinor(it.unitPriceMinor, it.currency), it.quantity) },
    currency = firstOrNull()?.let { Currency.getInstance(it.currency) } ?: fallbackCurrency,
)
```

`di/StoreCurrency.kt`:

```kotlin
package com.example.feature.checkout.data.di

import javax.inject.Qualifier

/** The currency the shop sells in, used whenever the cart is empty and carries no currency of its own. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class StoreCurrency
```

`repository/CartRepositoryImpl.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.di.StoreCurrency
import com.example.feature.checkout.data.local.CartDataSource
import com.example.feature.checkout.data.mapper.toCart
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Currency
import javax.inject.Inject

internal class CartRepositoryImpl @Inject constructor(
    private val cartDataSource: CartDataSource,
    @StoreCurrency private val storeCurrency: Currency,
) : CartRepository {

    override fun observeCart(): Flow<Cart> = cartDataSource.observeLines().map { it.toCart(storeCurrency) }

    override suspend fun setQuantity(sku: String, quantity: Int): CheckoutOutcome<Unit> =
        written(cartDataSource.updateQuantity(sku, quantity))

    override suspend fun removeLine(sku: String): CheckoutOutcome<Unit> = written(cartDataSource.delete(sku))

    private fun written(found: Boolean): CheckoutOutcome<Unit> =
        if (found) Outcome.Success(Unit) else Outcome.Failure(CheckoutError.NotFound)
}
```

`repository/DemoCustomerRepository.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** The signed-in customer of the demo: a member, so free standard shipping is visible. */
internal class DemoCustomerRepository @Inject constructor() : CustomerRepository {

    override fun observeCustomer(): Flow<Customer> = flowOf(DEMO_CUSTOMER)

    private companion object {
        val DEMO_CUSTOMER = Customer(id = "customer-1", displayName = "Alex", isMember = true)
    }
}
```

`repository/DemoFeatureFlagRepository.kt`:

```kotlin
package com.example.feature.checkout.data.repository

import com.example.feature.checkout.domain.model.FeatureFlag
import com.example.feature.checkout.domain.repository.FeatureFlagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Stands in for remote config: every flag of the demo is switched on. */
internal class DemoFeatureFlagRepository @Inject constructor() : FeatureFlagRepository {

    override fun observe(flag: FeatureFlag): Flow<Boolean> = flowOf(true)
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :feature:checkout:data:testDebugUnitTest`
Expected: PASS. 37 tests (24 from Task 5 and 13 new).

- [ ] **Step 5: Add the connectivity adapter and the Hilt modules**

`feature/checkout/data/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
</manifest>
```

`network/ConnectivityNetworkMonitor.kt`:

```kotlin
package com.example.feature.checkout.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.feature.checkout.domain.repository.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** The Android adapter behind [NetworkMonitor]: follows the device's default network. */
internal class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkMonitor {

    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        trySend(manager.hasInternet())
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.conflate().distinctUntilChanged()

    private fun ConnectivityManager.hasInternet(): Boolean =
        getNetworkCapabilities(activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
```

`di/DataSourceModule.kt`:

```kotlin
package com.example.feature.checkout.data.di

import com.example.feature.checkout.data.demo.DemoCheckoutData
import com.example.feature.checkout.data.local.CartDataSource
import com.example.feature.checkout.data.local.InMemoryCartDataSource
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Currency
import javax.inject.Singleton

/** Picks the data sources. Swapping the demo backend for a real API or Room is a change here only. */
@Module
@InstallIn(SingletonComponent::class)
internal object DataSourceModule {

    private const val DEMO_LATENCY_MILLIS = 600L

    @Provides
    @Singleton
    fun checkoutRemoteDataSource(): CheckoutRemoteDataSource = DemoCheckoutRemoteDataSource(DEMO_LATENCY_MILLIS)

    @Provides
    @Singleton
    fun cartDataSource(): CartDataSource = InMemoryCartDataSource()

    @Provides
    @StoreCurrency
    fun storeCurrency(): Currency = Currency.getInstance(DemoCheckoutData.CURRENCY)
}
```

`di/RepositoryModule.kt`:

```kotlin
package com.example.feature.checkout.data.di

import com.example.feature.checkout.data.network.ConnectivityNetworkMonitor
import com.example.feature.checkout.data.repository.AddressRepositoryImpl
import com.example.feature.checkout.data.repository.CartRepositoryImpl
import com.example.feature.checkout.data.repository.DemoCustomerRepository
import com.example.feature.checkout.data.repository.DemoFeatureFlagRepository
import com.example.feature.checkout.data.repository.OrderRepositoryImpl
import com.example.feature.checkout.data.repository.PaymentMethodRepositoryImpl
import com.example.feature.checkout.data.repository.PromoRepositoryImpl
import com.example.feature.checkout.data.repository.ShippingRepositoryImpl
import com.example.feature.checkout.domain.repository.AddressRepository
import com.example.feature.checkout.domain.repository.CartRepository
import com.example.feature.checkout.domain.repository.CustomerRepository
import com.example.feature.checkout.domain.repository.FeatureFlagRepository
import com.example.feature.checkout.domain.repository.NetworkMonitor
import com.example.feature.checkout.domain.repository.OrderRepository
import com.example.feature.checkout.domain.repository.PaymentMethodRepository
import com.example.feature.checkout.domain.repository.PromoRepository
import com.example.feature.checkout.domain.repository.ShippingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The only place that knows which implementation backs each domain repository interface. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    abstract fun cartRepository(impl: CartRepositoryImpl): CartRepository

    @Binds
    abstract fun addressRepository(impl: AddressRepositoryImpl): AddressRepository

    @Binds
    abstract fun shippingRepository(impl: ShippingRepositoryImpl): ShippingRepository

    @Binds
    abstract fun promoRepository(impl: PromoRepositoryImpl): PromoRepository

    @Binds
    abstract fun paymentMethodRepository(impl: PaymentMethodRepositoryImpl): PaymentMethodRepository

    @Binds
    abstract fun orderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    abstract fun customerRepository(impl: DemoCustomerRepository): CustomerRepository

    @Binds
    abstract fun networkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    @Binds
    abstract fun featureFlagRepository(impl: DemoFeatureFlagRepository): FeatureFlagRepository
}
```

- [ ] **Step 6: Verify tests and the Hilt modules compile**

Run: `./gradlew :feature:checkout:data:testDebugUnitTest :feature:checkout:data:assembleDebug`
Expected: BUILD SUCCESSFUL. 37 tests pass and KSP processes both modules.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(checkout): add the in-memory cart, connectivity monitor and data bindings"
```

---

### Task 7: Core UI — `UiText`, `ObserveAsEvents`, money formatting and theme

**Files:**
- Modify: `settings.gradle.kts` (add `include(":core:ui")`)
- Create:
  - `core/ui/build.gradle.kts`
  - Under `core/ui/src/main/kotlin/com/example/core/ui/`:
    - `{UiText,UiTextResolution,ObserveAsEvents,MoneyFormatter,LocaleMoneyFormatter}.kt`
    - `theme/ShopTheme.kt`
    - `di/CoreUiModule.kt`
- Test: `core/ui/src/test/kotlin/com/example/core/ui/{UiTextTest,LocaleMoneyFormatterTest}.kt`

**Interfaces:**
- Produces:
  - `sealed interface UiText { Raw(value); Res(@StringRes id, args: List<Any>); Plural(@PluralsRes id, count, args) }`, with `UiText.res(id, vararg args)` and `UiText.plural(id, count, vararg args)`
  - `@Composable UiText.asString(): String` and `UiText.asString(context: Context): String`
  - `@Composable fun <T> ObserveAsEvents(flow: Flow<T>, key1: Any? = null, key2: Any? = null, onEvent: (T) -> Unit)`
  - `fun interface MoneyFormatter { fun format(money: Money): String }` and `class LocaleMoneyFormatter(locale: () -> Locale = Locale::getDefault)`
  - `@Composable fun ShopTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`
  - `CoreUiModule` provides `MoneyFormatter` as a singleton

- [ ] **Step 1: Register the module**

Append to `settings.gradle.kts`:

```kotlin
include(":core:ui")
```

`core/ui/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.shop.android.library.compose)
    alias(libs.plugins.shop.hilt)
}

android {
    namespace = "com.example.core.ui"
}

dependencies {
    api(projects.core.domain)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.annotation)
}
```

- [ ] **Step 2: Write the failing tests**

`core/ui/src/test/kotlin/com/example/core/ui/UiTextTest.kt`:

```kotlin
package com.example.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UiTextTest {

    @Test
    fun `factory helpers build values that compare structurally`() {
        assertEquals(UiText.Res(1, listOf("a", 2)), UiText.res(1, "a", 2))
        assertEquals(UiText.Plural(3, 4, listOf(4)), UiText.plural(3, 4, 4))
    }
}
```

`core/ui/src/test/kotlin/com/example/core/ui/LocaleMoneyFormatterTest.kt`:

```kotlin
package com.example.core.ui

import com.example.core.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocaleMoneyFormatterTest {

    @Test
    fun `formats with the given locale and the money's own currency`() {
        val formatter = LocaleMoneyFormatter { Locale.US }

        assertEquals("€12.99", formatter.format(Money.of("12.99", "EUR")))
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :core:ui:testDebugUnitTest`
Expected: FAIL. `Unresolved reference 'UiText'` and `'LocaleMoneyFormatter'`.

- [ ] **Step 4: Implement**

All files in `core/ui/src/main/kotlin/com/example/core/ui/`.

`UiText.kt`:

```kotlin
package com.example.core.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Text resolved at render time, so the code that produces it stays free of Context and testable on
 * the JVM. Equality is structural.
 */
sealed interface UiText {
    data class Raw(val value: String) : UiText
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText

    companion object {
        fun res(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())

        fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): UiText = Plural(id, count, args.toList())
    }
}
```

`UiTextResolution.kt`:

```kotlin
package com.example.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

@Composable
@ReadOnlyComposable
fun UiText.asString(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Res -> stringResource(id, *args.toTypedArray())
    is UiText.Plural -> pluralStringResource(id, count, *args.toTypedArray())
}

/** For text needed outside composition, such as a snackbar shown from a coroutine. */
fun UiText.asString(context: Context): String = when (this) {
    is UiText.Raw -> value
    is UiText.Res -> context.getString(id, *args.toTypedArray())
    is UiText.Plural -> context.resources.getQuantityString(id, count, *args.toTypedArray())
}
```

`ObserveAsEvents.kt`:

```kotlin
package com.example.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Collects one-off events while the screen is at least STARTED.
 *
 * Collecting on [Dispatchers.Main.immediate] means no event is lost between lifecycle transitions,
 * and [rememberUpdatedState] means the latest [onEvent] handles it, even after recomposition.
 */
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = null,
    key2: Any? = null,
    onEvent: (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(flow, lifecycleOwner.lifecycle, key1, key2) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect { event -> currentOnEvent(event) }
            }
        }
    }
}
```

`MoneyFormatter.kt`:

```kotlin
package com.example.core.ui

import com.example.core.domain.Money

/** How amounts are shown. Projectors depend on this, never on Locale or NumberFormat directly. */
fun interface MoneyFormatter {
    fun format(money: Money): String
}
```

`LocaleMoneyFormatter.kt`:

```kotlin
package com.example.core.ui

import com.example.core.domain.Money
import java.text.NumberFormat
import java.util.Locale

class LocaleMoneyFormatter(private val locale: () -> Locale = Locale::getDefault) : MoneyFormatter {

    override fun format(money: Money): String =
        NumberFormat.getCurrencyInstance(locale())
            .apply { currency = money.currency }
            .format(money.amount)
}
```

`theme/ShopTheme.kt`:

```kotlin
package com.example.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun ShopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
```

`di/CoreUiModule.kt`:

```kotlin
package com.example.core.ui.di

import com.example.core.ui.LocaleMoneyFormatter
import com.example.core.ui.MoneyFormatter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreUiModule {

    @Provides
    @Singleton
    fun moneyFormatter(): MoneyFormatter = LocaleMoneyFormatter()
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :core:ui:testDebugUnitTest`
Expected: PASS. 2 tests.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(ui): add UiText, ObserveAsEvents, money formatting and the app theme"
```

---

### Task 8: Presentation contract — state, actions, effects, events and selectors

**Files:**
- Modify: `settings.gradle.kts` (add `include(":feature:checkout:presentation")`)
- Create:
  - `feature/checkout/presentation/build.gradle.kts`
  - `feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/contract/{Loadable,AddressSlice,ShippingSlice,PromoSlice,PromoStatus,Submission,CheckoutState,CheckoutAction,CheckoutIntent,LifecycleAction,ContextAction,CartAction,AddressAction,ShippingAction,PromoAction,PaymentAction,OrderAction,CheckoutEffect,CheckoutEvent,CheckoutSelectors}.kt`
- Test:
  - `feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/StateFixtures.kt`
  - `feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/contract/CheckoutSelectorsTest.kt`

**Interfaces:**
- Consumes: domain models, errors and rules (Tasks 3–4); `KeyedEffect` (Task 1).
- Produces:
  - Public: `CheckoutAction`, `CheckoutIntent`, `CheckoutEvent`, and the action groups `LifecycleAction`, `ContextAction`, `CartAction`, `AddressAction`, `ShippingAction`, `PromoAction`, `PaymentAction`, `OrderAction` (members listed in the code below).
  - Internal:
    - `CheckoutState(checkoutId, context, address, shipping, promo, selectedPaymentId, submission)`
    - `AddressSlice`, `ShippingSlice`, `PromoSlice`, `PromoStatus`, `Submission`, `Loadable`, `CheckoutEffect`
    - selectors `contextOrNull`, `availableShippingOptions`, `selectedShippingOption`, `appliedPromo`, `isAddressDeliverable`, `orderRequestOrNull()`
  - Test fixtures: `CHECKOUT_ID`, `readyState(context = checkoutContext())`. The ready state has HOME deliverable, quotes STANDARD/PICKUP/EXPRESS with STANDARD selected, and VISA selected.

- [ ] **Step 1: Register the module**

Append to `settings.gradle.kts`:

```kotlin
include(":feature:checkout:presentation")
```

`feature/checkout/presentation/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.shop.android.feature)
}

android {
    namespace = "com.example.feature.checkout.presentation"
}

dependencies {
    implementation(projects.feature.checkout.domain)

    testImplementation(testFixtures(projects.feature.checkout.domain))
}
```

- [ ] **Step 2: Write the state types**

All files in `feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/contract/`.

`Loadable.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError

/** Something the checkout asks the outside world for. */
internal sealed interface Loadable<out T> {
    data object Idle : Loadable<Nothing>
    data object Loading : Loadable<Nothing>
    data class Ready<out T>(val value: T) : Loadable<T>
    data class Failed(val error: CheckoutError) : Loadable<Nothing>
}
```

`AddressSlice.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.model.AddressValidation

internal data class AddressSlice(
    val selectedId: String? = null,
    val validation: Loadable<AddressValidation> = Loadable.Idle,
)
```

`ShippingSlice.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.model.ShippingOption

internal data class ShippingSlice(
    val quotes: Loadable<List<ShippingOption>> = Loadable.Idle,
    val selectedId: String? = null,
)
```

`PromoSlice.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

internal data class PromoSlice(
    val input: String = "",
    val status: PromoStatus = PromoStatus.None,
)
```

`PromoStatus.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.Promo

internal sealed interface PromoStatus {
    data object None : PromoStatus
    data class Redeeming(val code: String) : PromoStatus
    data class Applied(val promo: Promo) : PromoStatus
    data class Rejected(val error: CheckoutError) : PromoStatus
}
```

`Submission.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError

internal sealed interface Submission {
    data object Idle : Submission
    data object Submitting : Submission
    data class Failed(val error: CheckoutError) : Submission
}
```

`CheckoutState.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.model.CheckoutContext

/**
 * The single source of truth of the checkout screen.
 *
 * Shaped like the domain: no strings, no formatting, no "is the button enabled" flags. Selectors
 * derive business answers from it and projectors derive what the UI shows. It is organised in
 * slices, one per section of the screen, each owned by one slice reducer.
 */
internal data class CheckoutState(
    val checkoutId: String,
    val context: Loadable<CheckoutContext> = Loadable.Loading,
    val address: AddressSlice = AddressSlice(),
    val shipping: ShippingSlice = ShippingSlice(),
    val promo: PromoSlice = PromoSlice(),
    val selectedPaymentId: String? = null,
    val submission: Submission = Submission.Idle,
)
```

- [ ] **Step 3: Write the actions, effects and events**

`CheckoutAction.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

/**
 * Everything the reducer handles: [CheckoutIntent]s sent by the UI and the results effects report
 * back. Actions are grouped per section, one sealed interface per section.
 */
sealed interface CheckoutAction
```

`CheckoutIntent.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

/**
 * The subset of [CheckoutAction]s the UI may send. Effect results are not intents, so the type
 * system keeps the UI from faking them.
 */
sealed interface CheckoutIntent : CheckoutAction
```

`LifecycleAction.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

sealed interface LifecycleAction : CheckoutAction {

    /** Sent once by the ViewModel when the screen starts. Not an intent: the UI cannot restart the data stream. */
    data object Started : LifecycleAction

    data object RetryLoadClicked : LifecycleAction, CheckoutIntent
}
```

`ContextAction.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.CheckoutContext

sealed interface ContextAction : CheckoutAction {
    data class Updated(val context: CheckoutContext) : ContextAction
    data class Failed(val error: CheckoutError) : ContextAction
}
```

`CartAction.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError

sealed interface CartAction : CheckoutAction {
    data class QuantityChanged(val sku: String, val quantity: Int) : CartAction, CheckoutIntent
    data class UpdateFailed(val error: CheckoutError) : CartAction
}
```

`AddressAction.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.AddressValidation

sealed interface AddressAction : CheckoutAction {
    data class Selected(val addressId: String) : AddressAction, CheckoutIntent
    data class Validated(val addressId: String, val result: AddressValidation) : AddressAction
    data class ValidationFailed(val addressId: String, val error: CheckoutError) : AddressAction
}
```

`ShippingAction.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.ShippingOption

sealed interface ShippingAction : CheckoutAction {
    data class Selected(val optionId: String) : ShippingAction, CheckoutIntent
    data object RetryClicked : ShippingAction, CheckoutIntent
    data class QuotesLoaded(val addressId: String, val options: List<ShippingOption>) : ShippingAction
    data class QuotesFailed(val addressId: String, val error: CheckoutError) : ShippingAction
}
```

`PromoAction.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.Promo

sealed interface PromoAction : CheckoutAction {
    data class InputChanged(val text: String) : PromoAction, CheckoutIntent
    data object ApplyClicked : PromoAction, CheckoutIntent
    data object RemoveClicked : PromoAction, CheckoutIntent
    data class Redeemed(val promo: Promo) : PromoAction
    data class Rejected(val error: CheckoutError) : PromoAction
}
```

`PaymentAction.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

sealed interface PaymentAction : CheckoutAction {
    data class Selected(val paymentMethodId: String) : PaymentAction, CheckoutIntent
}
```

`OrderAction.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.Order

sealed interface OrderAction : CheckoutAction {
    data object PlaceClicked : OrderAction, CheckoutIntent
    data object ErrorDismissed : OrderAction, CheckoutIntent
    data class Placed(val order: Order) : OrderAction
    data class Failed(val error: CheckoutError) : OrderAction
}
```

`CheckoutEffect.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.core.mvi.KeyedEffect
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.OrderRequest

/**
 * Side effects a reducer asks for, as plain data so tests can assert on them. A [KeyedEffect] is
 * latest-wins: starting one cancels the running one with the same key.
 */
internal sealed interface CheckoutEffect {

    data object ObserveContext : CheckoutEffect, KeyedEffect {
        override val cancelKey: Any get() = "context"
    }

    data class UpdateQuantity(val sku: String, val quantity: Int) : CheckoutEffect

    data class ValidateAddress(val addressId: String) : CheckoutEffect, KeyedEffect {
        override val cancelKey: Any get() = "address-validation"
    }

    data class LoadShippingQuotes(val addressId: String, val cart: Cart) : CheckoutEffect, KeyedEffect {
        override val cancelKey: Any get() = "shipping-quotes"
    }

    data class RedeemPromo(val code: String, val cart: Cart) : CheckoutEffect, KeyedEffect {
        override val cancelKey: Any get() = "promo"
    }

    data class PlaceOrder(val request: OrderRequest) : CheckoutEffect
}
```

`CheckoutEvent.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError

/** One-off signals for the UI. State is what the screen shows; events are things that happen once. */
sealed interface CheckoutEvent {
    data class NavigateToConfirmation(val orderId: String) : CheckoutEvent
    data class ShowError(val error: CheckoutError) : CheckoutEvent
}
```

- [ ] **Step 4: Write the fixtures and the failing selector tests**

`feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/StateFixtures.kt`:

```kotlin
package com.example.feature.checkout.presentation

import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingSlice

internal const val CHECKOUT_ID = "checkout-1"

/** A state that meets every precondition for placing an order. */
internal fun readyState(context: CheckoutContext = checkoutContext()): CheckoutState = CheckoutState(
    checkoutId = CHECKOUT_ID,
    context = Loadable.Ready(context),
    address = AddressSlice(selectedId = HOME.id, validation = Loadable.Ready(AddressValidation.Deliverable)),
    shipping = ShippingSlice(quotes = Loadable.Ready(listOf(STANDARD, PICKUP, EXPRESS)), selectedId = STANDARD.id),
    selectedPaymentId = VISA.id,
)
```

`feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/contract/CheckoutSelectorsTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckoutSelectorsTest {

    @Test
    fun `a ready checkout produces exactly the order the customer sees`() {
        assertEquals(OrderRequest(CHECKOUT_ID, HOME.id, STANDARD.id, VISA.id, promoCode = null), readyState().orderRequestOrNull())
    }

    @Test
    fun `an applied promo is sent with the order`() {
        val state = readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Applied(SAVE5)))

        assertEquals("SAVE5", state.orderRequestOrNull()?.promoCode)
    }

    @Test
    fun `no order can be placed while offline`() {
        assertNull(readyState(checkoutContext(isOnline = false)).orderRequestOrNull())
    }

    @Test
    fun `no order can be placed with an empty cart`() {
        assertNull(readyState(checkoutContext(cart = cartOf())).orderRequestOrNull())
    }

    @Test
    fun `no order can be placed while one is being placed`() {
        assertNull(readyState().copy(submission = Submission.Submitting).orderRequestOrNull())
    }

    @Test
    fun `no order can be placed while a promo is being checked`() {
        assertNull(readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Redeeming("SAVE5"))).orderRequestOrNull())
    }

    @Test
    fun `the address must be confirmed deliverable`() {
        val checking = readyState().copy(address = AddressSlice(HOME.id, Loadable.Loading))
        val refused = readyState().copy(
            address = AddressSlice(
                HOME.id,
                Loadable.Ready(AddressValidation.Undeliverable(AddressValidation.Reason.OUTSIDE_SERVICE_AREA)),
            ),
        )

        assertNull(checking.orderRequestOrNull())
        assertNull(refused.orderRequestOrNull())
    }

    @Test
    fun `an express choice is not valid while express is switched off`() {
        val state = readyState(checkoutContext(isExpressShippingEnabled = false))
            .copy(shipping = ShippingSlice(Loadable.Ready(listOf(STANDARD, EXPRESS)), selectedId = EXPRESS.id))

        assertNull(state.orderRequestOrNull())
    }

    @Test
    fun `the chosen card must still be offered`() {
        assertNull(readyState().copy(selectedPaymentId = "gone").orderRequestOrNull())
    }

    @Test
    fun `available options hide express while it is switched off`() {
        assertEquals(listOf(STANDARD, PICKUP), readyState(checkoutContext(isExpressShippingEnabled = false)).availableShippingOptions)
    }
}
```

- [ ] **Step 5: Run the tests to verify they fail**

Run: `./gradlew :feature:checkout:presentation:testDebugUnitTest`
Expected: FAIL. `Unresolved reference 'orderRequestOrNull'` and `'availableShippingOptions'`.

- [ ] **Step 6: Implement the selectors**

`contract/CheckoutSelectors.kt`:

```kotlin
package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.domain.model.Promo
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.rule.availableFor

// Selectors answer business questions about the state. Reducers use them as guards and projectors
// as display rules, so a rule like "can the order be placed?" is written exactly once.

internal val CheckoutState.contextOrNull: CheckoutContext?
    get() = (context as? Loadable.Ready)?.value

/** The shipping options the customer may pick right now. */
internal val CheckoutState.availableShippingOptions: List<ShippingOption>
    get() {
        val quotes = (shipping.quotes as? Loadable.Ready)?.value.orEmpty()
        return quotes.availableFor(isExpressEnabled = contextOrNull?.isExpressShippingEnabled == true)
    }

internal val CheckoutState.selectedShippingOption: ShippingOption?
    get() = availableShippingOptions.firstOrNull { it.id == shipping.selectedId }

internal val CheckoutState.appliedPromo: Promo?
    get() = (promo.status as? PromoStatus.Applied)?.promo

internal val CheckoutState.isAddressDeliverable: Boolean
    get() = (address.validation as? Loadable.Ready)?.value == AddressValidation.Deliverable

/**
 * The one definition of "ready to pay": the request to send, or null while anything is missing.
 * The order reducer uses it as its guard and the place-order button projector for its enabled state,
 * so the two can never disagree.
 */
internal fun CheckoutState.orderRequestOrNull(): OrderRequest? {
    val context = contextOrNull ?: return null
    if (!context.isOnline || context.cart.isEmpty) return null
    if (submission == Submission.Submitting) return null
    if (promo.status is PromoStatus.Redeeming) return null

    val addressId = address.selectedId?.takeIf { isAddressDeliverable } ?: return null
    val shippingOptionId = selectedShippingOption?.id ?: return null
    val paymentMethodId = selectedPaymentId?.takeIf { id -> context.paymentMethods.any { it.id == id } } ?: return null

    return OrderRequest(checkoutId, addressId, shippingOptionId, paymentMethodId, appliedPromo?.code)
}
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew :feature:checkout:presentation:testDebugUnitTest`
Expected: PASS. 10 tests.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat(checkout): add the checkout MVI contract and readiness selectors"
```

---

### Task 9: Reducers — one per slice

**Files:**
- Create: `feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/reducer/{CheckoutTransition,SelectAddress,RequestQuotes,RevalidatePromo,ReconcileShippingSelection,LifecycleReducer,ContextReducer,CartReducer,AddressReducer,ShippingReducer,PromoReducer,PaymentReducer,OrderReducer,CheckoutReducer}.kt`
- Test:
  - `feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/ReducerFixtures.kt`
  - `feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/reducer/{LifecycleReducerTest,ContextReducerTest,CartReducerTest,AddressReducerTest,ShippingReducerTest,PromoReducerTest,PaymentReducerTest,OrderReducerTest,CheckoutReducerTest}.kt`

**Interfaces:**
- Consumes: contract and selectors (Task 8), domain rules (Task 3), `Reducer`, `Transition` and the DSL (Task 1).
- Produces:
  - `internal typealias CheckoutTransition = Transition<CheckoutState, CheckoutEffect, CheckoutEvent>`
  - `internal typealias CheckoutSliceReducer<A> = Reducer<CheckoutState, A, CheckoutEffect, CheckoutEvent>`
  - `internal class CheckoutReducer @Inject constructor(lifecycle, context, cart, address, shipping, promo, payment, order) : Reducer<CheckoutState, CheckoutAction, CheckoutEffect, CheckoutEvent>`
  - eight slice reducers, each `@Inject constructor()`
  - test fixture `checkoutReducer()`

- [ ] **Step 1: Write the fixture and the failing tests**

`feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/ReducerFixtures.kt`:

```kotlin
package com.example.feature.checkout.presentation

import com.example.feature.checkout.presentation.reducer.AddressReducer
import com.example.feature.checkout.presentation.reducer.CartReducer
import com.example.feature.checkout.presentation.reducer.CheckoutReducer
import com.example.feature.checkout.presentation.reducer.ContextReducer
import com.example.feature.checkout.presentation.reducer.LifecycleReducer
import com.example.feature.checkout.presentation.reducer.OrderReducer
import com.example.feature.checkout.presentation.reducer.PaymentReducer
import com.example.feature.checkout.presentation.reducer.PromoReducer
import com.example.feature.checkout.presentation.reducer.ShippingReducer

internal fun checkoutReducer(): CheckoutReducer = CheckoutReducer(
    lifecycle = LifecycleReducer(),
    context = ContextReducer(),
    cart = CartReducer(),
    address = AddressReducer(),
    shipping = ShippingReducer(),
    promo = PromoReducer(),
    payment = PaymentReducer(),
    order = OrderReducer(),
)
```

All tests below are in `feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/reducer/`. They are plain function calls: no coroutines, no Android.

`LifecycleReducerTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.Loadable
import org.junit.Assert.assertEquals
import org.junit.Test

class LifecycleReducerTest {

    private val reducer = LifecycleReducer()

    @Test
    fun `starting asks to observe the checkout context`() {
        val start = CheckoutState(checkoutId = CHECKOUT_ID)

        val transition = reducer.reduce(start, LifecycleAction.Started)

        assertEquals(start, transition.state)
        assertEquals(listOf(CheckoutEffect.ObserveContext), transition.effects)
    }

    @Test
    fun `retrying shows loading and observes the context again`() {
        val failed = CheckoutState(checkoutId = CHECKOUT_ID, context = Loadable.Failed(CheckoutError.Network))

        val transition = reducer.reduce(failed, LifecycleAction.RetryLoadClicked)

        assertEquals(Loadable.Loading, transition.state.context)
        assertEquals(listOf(CheckoutEffect.ObserveContext), transition.effects)
    }
}
```

`ContextReducerTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.OFFICE
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.domain.fixture.TEE
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.PromoSlice
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.contract.orderRequestOrNull
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextReducerTest {

    private val reducer = ContextReducer()

    @Test
    fun `the first context picks the default address and card, then checks the address and quotes`() {
        val context = checkoutContext()

        val transition = reducer.reduce(CheckoutState(checkoutId = CHECKOUT_ID), ContextAction.Updated(context))

        assertEquals(AddressSlice(HOME.id, Loadable.Loading), transition.state.address)
        assertEquals(VISA.id, transition.state.selectedPaymentId)
        assertEquals(Loadable.Loading, transition.state.shipping.quotes)
        assertEquals(
            listOf(CheckoutEffect.ValidateAddress(HOME.id), CheckoutEffect.LoadShippingQuotes(HOME.id, context.cart)),
            transition.effects,
        )
    }

    @Test
    fun `a cart change re-quotes shipping and re-checks an applied promo`() {
        val start = readyState().copy(promo = PromoSlice(input = "SAVE5", status = PromoStatus.Applied(SAVE5)))
        val newCart = cartOf(MUG, TEE)

        val transition = reducer.reduce(start, ContextAction.Updated(checkoutContext(cart = newCart)))

        assertEquals(
            listOf(CheckoutEffect.LoadShippingQuotes(HOME.id, newCart), CheckoutEffect.RedeemPromo("SAVE5", newCart)),
            transition.effects,
        )
        assertEquals(PromoStatus.Redeeming("SAVE5"), transition.state.promo.status)
    }

    @Test
    fun `emptying the cart clears shipping and blocks paying`() {
        val transition = reducer.reduce(readyState(), ContextAction.Updated(checkoutContext(cart = cartOf())))

        assertEquals(ShippingSlice(), transition.state.shipping)
        assertNull(transition.state.orderRequestOrNull())
        assertTrue(transition.effects.isEmpty())
    }

    @Test
    fun `switching express off moves an express choice to an allowed option`() {
        val start = readyState().copy(shipping = readyState().shipping.copy(selectedId = EXPRESS.id))

        val transition = reducer.reduce(start, ContextAction.Updated(checkoutContext(isExpressShippingEnabled = false)))

        assertEquals(PICKUP.id, transition.state.shipping.selectedId)
    }

    @Test
    fun `a removed address falls back to the default one`() {
        val start = readyState().copy(address = AddressSlice(OFFICE.id, Loadable.Ready(AddressValidation.Deliverable)))

        val transition = reducer.reduce(start, ContextAction.Updated(checkoutContext(addresses = listOf(HOME))))

        assertEquals(HOME.id, transition.state.address.selectedId)
        assertTrue(CheckoutEffect.ValidateAddress(HOME.id) in transition.effects)
    }

    @Test
    fun `a failed first load blocks the screen`() {
        val transition = reducer.reduce(CheckoutState(checkoutId = CHECKOUT_ID), ContextAction.Failed(CheckoutError.Network))

        assertEquals(Loadable.Failed(CheckoutError.Network), transition.state.context)
        assertTrue(transition.events.isEmpty())
    }

    @Test
    fun `a failed refresh keeps the checkout and reports the error once`() {
        val transition = reducer.reduce(readyState(), ContextAction.Failed(CheckoutError.Network))

        assertEquals(readyState(), transition.state)
        assertEquals(listOf(CheckoutEvent.ShowError(CheckoutError.Network)), transition.events)
    }
}
```

`CartReducerTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartReducerTest {

    private val reducer = CartReducer()

    @Test
    fun `changing a quantity asks to write it and waits for the cart to change`() {
        val transition = reducer.reduce(readyState(), CartAction.QuantityChanged(MUG.sku, 3))

        assertEquals(readyState(), transition.state)
        assertEquals(listOf(CheckoutEffect.UpdateQuantity(MUG.sku, 3)), transition.effects)
    }

    @Test
    fun `the same quantity or an unknown line changes nothing`() {
        assertTrue(reducer.reduce(readyState(), CartAction.QuantityChanged(MUG.sku, MUG.quantity)).effects.isEmpty())
        assertTrue(reducer.reduce(readyState(), CartAction.QuantityChanged("unknown", 1)).effects.isEmpty())
    }

    @Test
    fun `quantities cannot change while the order is being placed`() {
        val submitting = readyState().copy(submission = Submission.Submitting)

        assertTrue(reducer.reduce(submitting, CartAction.QuantityChanged(MUG.sku, 3)).effects.isEmpty())
    }

    @Test
    fun `a failed write is reported once`() {
        val transition = reducer.reduce(readyState(), CartAction.UpdateFailed(CheckoutError.Network))

        assertEquals(listOf(CheckoutEvent.ShowError(CheckoutError.Network)), transition.events)
    }
}
```

`AddressReducerTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.OFFICE
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressReducerTest {

    private val reducer = AddressReducer()

    @Test
    fun `picking another address restarts its check and the quotes`() {
        val transition = reducer.reduce(readyState(), AddressAction.Selected(OFFICE.id))

        assertEquals(AddressSlice(OFFICE.id, Loadable.Loading), transition.state.address)
        assertNull(transition.state.shipping.selectedId)
        assertEquals(
            listOf(CheckoutEffect.ValidateAddress(OFFICE.id), CheckoutEffect.LoadShippingQuotes(OFFICE.id, cartOf(MUG))),
            transition.effects,
        )
    }

    @Test
    fun `picking the current, valid address changes nothing`() {
        assertEquals(readyState(), reducer.reduce(readyState(), AddressAction.Selected(HOME.id)).state)
    }

    @Test
    fun `picking the address again after a failed check retries it`() {
        val failed = readyState().copy(address = AddressSlice(HOME.id, Loadable.Failed(CheckoutError.Network)))

        val transition = reducer.reduce(failed, AddressAction.Selected(HOME.id))

        assertTrue(CheckoutEffect.ValidateAddress(HOME.id) in transition.effects)
    }

    @Test
    fun `an unknown address is ignored`() {
        assertTrue(reducer.reduce(readyState(), AddressAction.Selected("nowhere")).effects.isEmpty())
    }

    @Test
    fun `the check result for the current address is stored`() {
        val checking = reducer.reduce(readyState(), AddressAction.Selected(OFFICE.id)).state
        val undeliverable = AddressValidation.Undeliverable(AddressValidation.Reason.PO_BOX_NOT_ALLOWED)

        val transition = reducer.reduce(checking, AddressAction.Validated(OFFICE.id, undeliverable))

        assertEquals(Loadable.Ready(undeliverable), transition.state.address.validation)
    }

    @Test
    fun `a late result for a previous address is ignored`() {
        val checking = reducer.reduce(readyState(), AddressAction.Selected(OFFICE.id)).state

        assertEquals(checking, reducer.reduce(checking, AddressAction.Validated(HOME.id, AddressValidation.Deliverable)).state)
        assertEquals(checking, reducer.reduce(checking, AddressAction.ValidationFailed(HOME.id, CheckoutError.Network)).state)
    }
}
```

`ShippingReducerTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MEMBER
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.OFFICE
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingAction
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Test

class ShippingReducerTest {

    private val reducer = ShippingReducer()
    private val quotes = listOf(STANDARD, PICKUP, EXPRESS)

    @Test
    fun `picking an offered option selects it`() {
        assertEquals(EXPRESS.id, reducer.reduce(readyState(), ShippingAction.Selected(EXPRESS.id)).state.shipping.selectedId)
    }

    @Test
    fun `picking an option that is not offered is ignored`() {
        val expressOff = readyState(checkoutContext(isExpressShippingEnabled = false))

        assertEquals(expressOff, reducer.reduce(expressOff, ShippingAction.Selected(EXPRESS.id)).state)
    }

    @Test
    fun `loaded quotes keep the current pick while it is still offered`() {
        val loading = readyState().copy(shipping = ShippingSlice(Loadable.Loading, selectedId = EXPRESS.id))

        val transition = reducer.reduce(loading, ShippingAction.QuotesLoaded(HOME.id, quotes))

        assertEquals(EXPRESS.id, transition.state.shipping.selectedId)
    }

    @Test
    fun `loaded quotes otherwise pick the cheapest option for the customer`() {
        val loading = readyState().copy(shipping = ShippingSlice(Loadable.Loading, selectedId = "gone"))

        val transition = reducer.reduce(loading, ShippingAction.QuotesLoaded(HOME.id, quotes))

        assertEquals(PICKUP.id, transition.state.shipping.selectedId)
    }

    @Test
    fun `members get free standard shipping rather than the slower pickup`() {
        val loading = readyState(checkoutContext(customer = MEMBER)).copy(shipping = ShippingSlice(Loadable.Loading))

        val transition = reducer.reduce(loading, ShippingAction.QuotesLoaded(HOME.id, quotes))

        assertEquals(STANDARD.id, transition.state.shipping.selectedId)
    }

    @Test
    fun `late quotes for a previous address are ignored`() {
        val start = readyState().copy(
            address = AddressSlice(OFFICE.id, Loadable.Loading),
            shipping = ShippingSlice(Loadable.Loading),
        )

        assertEquals(start, reducer.reduce(start, ShippingAction.QuotesLoaded(HOME.id, listOf(PICKUP))).state)
        assertEquals(start, reducer.reduce(start, ShippingAction.QuotesFailed(HOME.id, CheckoutError.Network)).state)
    }

    @Test
    fun `failed quotes for the current address are shown`() {
        val loading = readyState().copy(shipping = ShippingSlice(Loadable.Loading))

        val transition = reducer.reduce(loading, ShippingAction.QuotesFailed(HOME.id, CheckoutError.Network))

        assertEquals(Loadable.Failed(CheckoutError.Network), transition.state.shipping.quotes)
    }

    @Test
    fun `retrying asks for quotes again`() {
        val transition = reducer.reduce(readyState(), ShippingAction.RetryClicked)

        assertEquals(Loadable.Loading, transition.state.shipping.quotes)
        assertEquals(listOf(CheckoutEffect.LoadShippingQuotes(HOME.id, cartOf(MUG))), transition.effects)
    }
}
```

`PromoReducerTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.PromoSlice
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.appliedPromo
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromoReducerTest {

    private val reducer = PromoReducer()

    @Test
    fun `applying sends the trimmed code and waits for the answer`() {
        val typed = readyState().copy(promo = PromoSlice(input = " save5 "))

        val transition = reducer.reduce(typed, PromoAction.ApplyClicked)

        assertEquals(PromoStatus.Redeeming("save5"), transition.state.promo.status)
        assertEquals(listOf(CheckoutEffect.RedeemPromo("save5", cartOf(MUG))), transition.effects)
    }

    @Test
    fun `applying is ignored without a code, or while a promo is checked or applied`() {
        val redeeming = readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Redeeming("SAVE5")))
        val applied = readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Applied(SAVE5)))

        assertTrue(reducer.reduce(readyState(), PromoAction.ApplyClicked).effects.isEmpty())
        assertTrue(reducer.reduce(redeeming, PromoAction.ApplyClicked).effects.isEmpty())
        assertTrue(reducer.reduce(applied, PromoAction.ApplyClicked).effects.isEmpty())
    }

    @Test
    fun `typing after a rejected code clears the error`() {
        val rejected = readyState().copy(promo = PromoSlice("BAD", PromoStatus.Rejected(CheckoutError.Promo.Invalid)))

        val transition = reducer.reduce(rejected, PromoAction.InputChanged("BADD"))

        assertEquals(PromoSlice("BADD", PromoStatus.None), transition.state.promo)
    }

    @Test
    fun `a redeemed promo is applied`() {
        val redeeming = readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Redeeming("SAVE5")))

        assertEquals(PromoStatus.Applied(SAVE5), reducer.reduce(redeeming, PromoAction.Redeemed(SAVE5)).state.promo.status)
    }

    @Test
    fun `an answer arriving after the promo was removed is ignored`() {
        val redeeming = readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Redeeming("SAVE5")))
        val removed = reducer.reduce(redeeming, PromoAction.RemoveClicked).state

        assertEquals(PromoSlice(), reducer.reduce(removed, PromoAction.Redeemed(SAVE5)).state.promo)
    }

    @Test
    fun `a promo that no longer qualifies after a cart change stops discounting`() {
        // After a cart change the applied BIG50 is checked again, and the smaller order no longer qualifies.
        val rechecking = readyState().copy(promo = PromoSlice("BIG50", PromoStatus.Redeeming("BIG50")))

        val transition = reducer.reduce(rechecking, PromoAction.Rejected(CheckoutError.Promo.MinimumNotReached))

        assertEquals(PromoStatus.Rejected(CheckoutError.Promo.MinimumNotReached), transition.state.promo.status)
        assertNull(transition.state.appliedPromo)
    }
}
```

`PaymentReducerTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.fixture.MASTERCARD
import com.example.feature.checkout.presentation.contract.PaymentAction
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentReducerTest {

    private val reducer = PaymentReducer()

    @Test
    fun `picking an offered card selects it`() {
        assertEquals(MASTERCARD.id, reducer.reduce(readyState(), PaymentAction.Selected(MASTERCARD.id)).state.selectedPaymentId)
    }

    @Test
    fun `picking an unknown card is ignored`() {
        assertEquals(readyState(), reducer.reduce(readyState(), PaymentAction.Selected("gone")).state)
    }
}
```

`OrderReducerTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderReducerTest {

    private val reducer = OrderReducer()

    @Test
    fun `paying sends exactly the order the customer sees`() {
        val transition = reducer.reduce(readyState(), OrderAction.PlaceClicked)

        assertEquals(Submission.Submitting, transition.state.submission)
        assertEquals(
            listOf(CheckoutEffect.PlaceOrder(OrderRequest(CHECKOUT_ID, HOME.id, STANDARD.id, VISA.id, promoCode = null))),
            transition.effects,
        )
    }

    @Test
    fun `a second tap while the order is being placed is ignored`() {
        val submitting = reducer.reduce(readyState(), OrderAction.PlaceClicked).state

        val transition = reducer.reduce(submitting, OrderAction.PlaceClicked)

        assertEquals(submitting, transition.state)
        assertTrue(transition.effects.isEmpty())
    }

    @Test
    fun `paying is ignored until the address is confirmed`() {
        val checking = readyState().copy(address = AddressSlice(HOME.id, Loadable.Loading))

        assertTrue(reducer.reduce(checking, OrderAction.PlaceClicked).effects.isEmpty())
    }

    @Test
    fun `paying is ignored while offline`() {
        assertTrue(reducer.reduce(readyState(checkoutContext(isOnline = false)), OrderAction.PlaceClicked).effects.isEmpty())
    }

    @Test
    fun `a placed order navigates once`() {
        val submitting = readyState().copy(submission = Submission.Submitting)

        val transition = reducer.reduce(submitting, OrderAction.Placed(Order("o-42")))

        assertEquals(Submission.Idle, transition.state.submission)
        assertEquals(listOf(CheckoutEvent.NavigateToConfirmation("o-42")), transition.events)
    }

    @Test
    fun `a failed order is shown until dismissed`() {
        val submitting = readyState().copy(submission = Submission.Submitting)

        val failed = reducer.reduce(submitting, OrderAction.Failed(CheckoutError.Payment.Declined)).state

        assertEquals(Submission.Failed(CheckoutError.Payment.Declined), failed.submission)
        assertEquals(Submission.Idle, reducer.reduce(failed, OrderAction.ErrorDismissed).state.submission)
    }

    @Test
    fun `a price change re-quotes shipping so the customer can review the total`() {
        val submitting = readyState().copy(submission = Submission.Submitting)

        val transition = reducer.reduce(submitting, OrderAction.Failed(CheckoutError.PriceChanged))

        assertEquals(listOf(CheckoutEffect.LoadShippingQuotes(HOME.id, cartOf(MUG))), transition.effects)
    }
}
```

`CheckoutReducerTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.fixture.MASTERCARD
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.checkoutReducer
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PaymentAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckoutReducerTest {

    private val reducer = checkoutReducer()

    @Test
    fun `every action group reaches its slice reducer`() {
        assertEquals(
            listOf(CheckoutEffect.ObserveContext),
            reducer.reduce(CheckoutState(checkoutId = CHECKOUT_ID), LifecycleAction.Started).effects,
        )
        assertEquals(MASTERCARD.id, reducer.reduce(readyState(), PaymentAction.Selected(MASTERCARD.id)).state.selectedPaymentId)
        assertEquals(Submission.Submitting, reducer.reduce(readyState(), OrderAction.PlaceClicked).state.submission)
        assertEquals("SAVE5", reducer.reduce(readyState(), PromoAction.InputChanged("SAVE5")).state.promo.input)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :feature:checkout:presentation:testDebugUnitTest`
Expected: FAIL. `Unresolved reference 'LifecycleReducer'`, `'CheckoutReducer'` and the other reducers.

- [ ] **Step 3: Implement the transition aliases and the cross-slice steps**

All files in `feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/reducer/`.

`CheckoutTransition.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.Reducer
import com.example.core.mvi.Transition
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState

internal typealias CheckoutTransition = Transition<CheckoutState, CheckoutEffect, CheckoutEvent>

internal typealias CheckoutSliceReducer<A> = Reducer<CheckoutState, A, CheckoutEffect, CheckoutEvent>
```

`SelectAddress.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.then
import com.example.core.mvi.withEffects
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingSlice

/** Picking an address restarts its check, forgets the shipping choice and asks for new quotes. */
internal fun CheckoutState.selectAddress(addressId: String): CheckoutTransition {
    val checking: CheckoutTransition = copy(
        address = AddressSlice(selectedId = addressId, validation = Loadable.Loading),
        shipping = ShippingSlice(),
    ).withEffects(CheckoutEffect.ValidateAddress(addressId))
    return checking.then { it.requestQuotes() }
}
```

`RequestQuotes.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.withEffects
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.contract.contextOrNull

/** Asks for quotes for the chosen address and the current cart. Without both there is nothing to quote. */
internal fun CheckoutState.requestQuotes(): CheckoutTransition {
    val addressId = address.selectedId
    val cart = contextOrNull?.cart
    if (addressId == null || cart == null || cart.isEmpty) return copy(shipping = ShippingSlice()).only()
    return copy(shipping = shipping.copy(quotes = Loadable.Loading))
        .withEffects(CheckoutEffect.LoadShippingQuotes(addressId, cart))
}
```

`RevalidatePromo.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.withEffects
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.appliedPromo
import com.example.feature.checkout.presentation.contract.contextOrNull

/** A discount depends on the cart, so an applied promo is checked again whenever the cart changes. */
internal fun CheckoutState.revalidatePromo(): CheckoutTransition {
    val applied = appliedPromo ?: return only()
    val cart = contextOrNull?.cart ?: return only()
    return copy(promo = promo.copy(status = PromoStatus.Redeeming(applied.code)))
        .withEffects(CheckoutEffect.RedeemPromo(applied.code, cart))
}
```

`ReconcileShippingSelection.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.feature.checkout.domain.rule.preferredFor
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.availableShippingOptions
import com.example.feature.checkout.presentation.contract.contextOrNull

/** Keeps the shipping choice valid for the latest quotes, express flag and customer. */
internal fun CheckoutState.reconcileShippingSelection(): CheckoutTransition {
    if (shipping.quotes !is Loadable.Ready) return only()
    val customer = contextOrNull?.customer ?: return only()
    val preferred = availableShippingOptions.preferredFor(shipping.selectedId, customer)
    return copy(shipping = shipping.copy(selectedId = preferred?.id)).only()
}
```

- [ ] **Step 4: Implement the slice reducers and the router**

`LifecycleReducer.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.withEffects
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.Loadable
import javax.inject.Inject

internal class LifecycleReducer @Inject constructor() : CheckoutSliceReducer<LifecycleAction> {

    override fun reduce(state: CheckoutState, action: LifecycleAction): CheckoutTransition = when (action) {
        LifecycleAction.Started -> state.withEffects(CheckoutEffect.ObserveContext)
        LifecycleAction.RetryLoadClicked ->
            state.copy(context = Loadable.Loading).withEffects(CheckoutEffect.ObserveContext)
    }
}
```

`ContextReducer.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.then
import com.example.core.mvi.withEvent
import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.contract.contextOrNull
import javax.inject.Inject

internal class ContextReducer @Inject constructor() : CheckoutSliceReducer<ContextAction> {

    override fun reduce(state: CheckoutState, action: ContextAction): CheckoutTransition = when (action) {
        is ContextAction.Updated -> onUpdated(state, action.context)
        is ContextAction.Failed ->
            // Keep showing what is loaded when a refresh fails; only the first load blocks the screen.
            if (state.contextOrNull != null) state.withEvent(CheckoutEvent.ShowError(action.error))
            else state.copy(context = Loadable.Failed(action.error)).only()
    }

    private fun onUpdated(state: CheckoutState, context: CheckoutContext): CheckoutTransition {
        val previous = state.contextOrNull
        val addressBefore = state.address.selectedId
        val cartChanged = previous != null && previous.cart != context.cart

        return state.copy(context = Loadable.Ready(context)).only()
            .then { it.reconcileAddress(context) }
            .then { it.reconcilePayment(context) }
            .then { if (cartChanged) it.onCartChanged(addressBefore) else it.only() }
            .then { it.reconcileShippingSelection() }
    }

    /** A newly picked address already asked for quotes; otherwise the same address needs quotes for the new cart. */
    private fun CheckoutState.onCartChanged(addressBefore: String?): CheckoutTransition {
        val requote = if (address.selectedId == addressBefore) requestQuotes() else only()
        return requote.then { it.revalidatePromo() }
    }

    private fun CheckoutState.reconcileAddress(context: CheckoutContext): CheckoutTransition {
        if (context.addresses.any { it.id == address.selectedId }) return only()
        val fallback = context.addresses.firstOrNull { it.isDefault } ?: context.addresses.firstOrNull()
        return if (fallback != null) selectAddress(fallback.id)
        else copy(address = AddressSlice(), shipping = ShippingSlice()).only()
    }

    private fun CheckoutState.reconcilePayment(context: CheckoutContext): CheckoutTransition {
        if (context.paymentMethods.any { it.id == selectedPaymentId }) return only()
        val fallback = context.paymentMethods.firstOrNull { it.isDefault } ?: context.paymentMethods.firstOrNull()
        return copy(selectedPaymentId = fallback?.id).only()
    }
}
```

`CartReducer.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.withEffects
import com.example.core.mvi.withEvent
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.contract.contextOrNull
import javax.inject.Inject

internal class CartReducer @Inject constructor() : CheckoutSliceReducer<CartAction> {

    override fun reduce(state: CheckoutState, action: CartAction): CheckoutTransition = when (action) {
        is CartAction.QuantityChanged -> onQuantityChanged(state, action)
        is CartAction.UpdateFailed -> state.withEvent(CheckoutEvent.ShowError(action.error))
    }

    // No optimistic update: the cart stream is the source of truth and emits the new cart after the write.
    private fun onQuantityChanged(state: CheckoutState, action: CartAction.QuantityChanged): CheckoutTransition {
        val line = state.contextOrNull?.cart?.lines?.firstOrNull { it.sku == action.sku }
        val unchanged = line == null || line.quantity == action.quantity
        val locked = state.submission == Submission.Submitting
        return if (unchanged || locked) state.only()
        else state.withEffects(CheckoutEffect.UpdateQuantity(action.sku, action.quantity))
    }
}
```

`AddressReducer.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.contextOrNull
import javax.inject.Inject

internal class AddressReducer @Inject constructor() : CheckoutSliceReducer<AddressAction> {

    override fun reduce(state: CheckoutState, action: AddressAction): CheckoutTransition = when (action) {
        is AddressAction.Selected -> {
            val known = state.contextOrNull?.addresses?.any { it.id == action.addressId } == true
            val alreadyChecked = action.addressId == state.address.selectedId &&
                state.address.validation !is Loadable.Failed
            if (!known || alreadyChecked) state.only() else state.selectAddress(action.addressId)
        }

        // Results carry the address they were computed for, so a late answer for a previous address is ignored.
        is AddressAction.Validated ->
            if (action.addressId != state.address.selectedId) state.only()
            else state.copy(address = state.address.copy(validation = Loadable.Ready(action.result))).only()

        is AddressAction.ValidationFailed ->
            if (action.addressId != state.address.selectedId) state.only()
            else state.copy(address = state.address.copy(validation = Loadable.Failed(action.error))).only()
    }
}
```

`ShippingReducer.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.then
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingAction
import com.example.feature.checkout.presentation.contract.availableShippingOptions
import javax.inject.Inject

internal class ShippingReducer @Inject constructor() : CheckoutSliceReducer<ShippingAction> {

    override fun reduce(state: CheckoutState, action: ShippingAction): CheckoutTransition = when (action) {
        is ShippingAction.Selected ->
            if (state.availableShippingOptions.none { it.id == action.optionId }) state.only()
            else state.copy(shipping = state.shipping.copy(selectedId = action.optionId)).only()

        ShippingAction.RetryClicked -> state.requestQuotes()

        // Quotes carry the address they were computed for, so quotes for a previous address are ignored.
        is ShippingAction.QuotesLoaded ->
            if (action.addressId != state.address.selectedId) state.only()
            else state.copy(shipping = state.shipping.copy(quotes = Loadable.Ready(action.options))).only()
                .then { it.reconcileShippingSelection() }

        is ShippingAction.QuotesFailed ->
            if (action.addressId != state.address.selectedId) state.only()
            else state.copy(shipping = state.shipping.copy(quotes = Loadable.Failed(action.error))).only()
    }
}
```

`PromoReducer.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.withEffects
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.PromoSlice
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.contextOrNull
import javax.inject.Inject

internal class PromoReducer @Inject constructor() : CheckoutSliceReducer<PromoAction> {

    override fun reduce(state: CheckoutState, action: PromoAction): CheckoutTransition {
        val promo = state.promo
        return when (action) {
            is PromoAction.InputChanged -> state.copy(
                promo = promo.copy(
                    input = action.text,
                    status = if (promo.status is PromoStatus.Rejected) PromoStatus.None else promo.status,
                ),
            ).only()

            PromoAction.ApplyClicked -> {
                val code = promo.input.trim()
                val cart = state.contextOrNull?.cart
                val busy = promo.status is PromoStatus.Redeeming || promo.status is PromoStatus.Applied
                if (code.isEmpty() || cart == null || cart.isEmpty || busy) state.only()
                else state.copy(promo = promo.copy(status = PromoStatus.Redeeming(code)))
                    .withEffects(CheckoutEffect.RedeemPromo(code, cart))
            }

            PromoAction.RemoveClicked -> state.copy(promo = PromoSlice()).only()

            // Answers are accepted only while one is awaited: the customer may have removed the promo meanwhile.
            is PromoAction.Redeemed ->
                if (promo.status !is PromoStatus.Redeeming) state.only()
                else state.copy(promo = promo.copy(status = PromoStatus.Applied(action.promo))).only()

            is PromoAction.Rejected ->
                if (promo.status !is PromoStatus.Redeeming) state.only()
                else state.copy(promo = promo.copy(status = PromoStatus.Rejected(action.error))).only()
        }
    }
}
```

`PaymentReducer.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.PaymentAction
import com.example.feature.checkout.presentation.contract.contextOrNull
import javax.inject.Inject

internal class PaymentReducer @Inject constructor() : CheckoutSliceReducer<PaymentAction> {

    override fun reduce(state: CheckoutState, action: PaymentAction): CheckoutTransition = when (action) {
        is PaymentAction.Selected -> {
            val offered = state.contextOrNull?.paymentMethods?.any { it.id == action.paymentMethodId } == true
            if (!offered) state.only() else state.copy(selectedPaymentId = action.paymentMethodId).only()
        }
    }
}
```

`OrderReducer.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.then
import com.example.core.mvi.withEffects
import com.example.core.mvi.withEvent
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.contract.orderRequestOrNull
import javax.inject.Inject

internal class OrderReducer @Inject constructor() : CheckoutSliceReducer<OrderAction> {

    override fun reduce(state: CheckoutState, action: OrderAction): CheckoutTransition = when (action) {
        OrderAction.PlaceClicked -> {
            val request = state.orderRequestOrNull()
            if (request == null) state.only()
            else state.copy(submission = Submission.Submitting).withEffects(CheckoutEffect.PlaceOrder(request))
        }

        is OrderAction.Placed ->
            state.copy(submission = Submission.Idle).withEvent(CheckoutEvent.NavigateToConfirmation(action.order.id))

        is OrderAction.Failed -> {
            val failed = state.copy(submission = Submission.Failed(action.error)).only()
            // Prices moved under the customer: re-quote so the new total is shown before paying again.
            if (action.error == CheckoutError.PriceChanged) failed.then { it.requestQuotes() } else failed
        }

        OrderAction.ErrorDismissed -> state.copy(submission = Submission.Idle).only()
    }
}
```

`CheckoutReducer.kt`:

```kotlin
package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.Reducer
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PaymentAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.ShippingAction
import javax.inject.Inject

/**
 * Routes each action group to the reducer of its section, and does nothing else.
 *
 * The `when` over the sealed action groups is exhaustive, so a new section does not compile until it
 * is routed here: new behaviour lands in a new slice reducer, and this is the one line to add.
 */
internal class CheckoutReducer @Inject constructor(
    private val lifecycle: LifecycleReducer,
    private val context: ContextReducer,
    private val cart: CartReducer,
    private val address: AddressReducer,
    private val shipping: ShippingReducer,
    private val promo: PromoReducer,
    private val payment: PaymentReducer,
    private val order: OrderReducer,
) : Reducer<CheckoutState, CheckoutAction, CheckoutEffect, CheckoutEvent> {

    override fun reduce(state: CheckoutState, action: CheckoutAction): CheckoutTransition = when (action) {
        is LifecycleAction -> lifecycle.reduce(state, action)
        is ContextAction -> context.reduce(state, action)
        is CartAction -> cart.reduce(state, action)
        is AddressAction -> address.reduce(state, action)
        is ShippingAction -> shipping.reduce(state, action)
        is PromoAction -> promo.reduce(state, action)
        is PaymentAction -> payment.reduce(state, action)
        is OrderAction -> order.reduce(state, action)
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :feature:checkout:presentation:testDebugUnitTest`
Expected: PASS. 53 tests (10 selector tests and 43 reducer tests).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(checkout): add one pure reducer per checkout section"
```

---

### Task 10: Effect handler

**Files:**
- Create: `feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/effect/CheckoutEffectHandler.kt`
- Test:
  - `feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/CheckoutTestEnvironment.kt`
  - `feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/effect/CheckoutEffectHandlerTest.kt`

**Interfaces:**
- Consumes: the use cases (Task 4), the contract (Task 8), `EffectHandler` (Task 1).
- Produces:
  - `internal class CheckoutEffectHandler @Inject constructor(observeCheckoutContext, updateCartQuantity, validateAddress, getShippingQuotes, redeemPromo, placeOrder) : EffectHandler<CheckoutEffect, CheckoutAction>`
  - test helper `CheckoutTestEnvironment` with properties `cart`, `addresses`, `shipping`, `promos`, `paymentMethods`, `orders`, `customer`, `network`, `flags` and `effectHandler()`

- [ ] **Step 1: Write the test environment and the failing tests**

`feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/CheckoutTestEnvironment.kt`:

```kotlin
package com.example.feature.checkout.presentation

import com.example.feature.checkout.domain.fake.FakeAddressRepository
import com.example.feature.checkout.domain.fake.FakeCartRepository
import com.example.feature.checkout.domain.fake.FakeCustomerRepository
import com.example.feature.checkout.domain.fake.FakeFeatureFlagRepository
import com.example.feature.checkout.domain.fake.FakeNetworkMonitor
import com.example.feature.checkout.domain.fake.FakeOrderRepository
import com.example.feature.checkout.domain.fake.FakePaymentMethodRepository
import com.example.feature.checkout.domain.fake.FakePromoRepository
import com.example.feature.checkout.domain.fake.FakeShippingRepository
import com.example.feature.checkout.domain.usecase.GetShippingQuotesUseCase
import com.example.feature.checkout.domain.usecase.ObserveCheckoutContextUseCase
import com.example.feature.checkout.domain.usecase.PlaceOrderUseCase
import com.example.feature.checkout.domain.usecase.RedeemPromoUseCase
import com.example.feature.checkout.domain.usecase.UpdateCartQuantityUseCase
import com.example.feature.checkout.domain.usecase.ValidateAddressUseCase
import com.example.feature.checkout.presentation.effect.CheckoutEffectHandler

/** Every repository the checkout uses, as fakes, and the effect handler wired to real use cases over them. */
internal class CheckoutTestEnvironment(
    val cart: FakeCartRepository = FakeCartRepository(),
    val addresses: FakeAddressRepository = FakeAddressRepository(),
    val shipping: FakeShippingRepository = FakeShippingRepository(),
    val promos: FakePromoRepository = FakePromoRepository(),
    val paymentMethods: FakePaymentMethodRepository = FakePaymentMethodRepository(),
    val orders: FakeOrderRepository = FakeOrderRepository(),
    val customer: FakeCustomerRepository = FakeCustomerRepository(),
    val network: FakeNetworkMonitor = FakeNetworkMonitor(),
    val flags: FakeFeatureFlagRepository = FakeFeatureFlagRepository(),
) {
    fun effectHandler(): CheckoutEffectHandler = CheckoutEffectHandler(
        observeCheckoutContext = ObserveCheckoutContextUseCase(cart, addresses, paymentMethods, customer, network, flags),
        updateCartQuantity = UpdateCartQuantityUseCase(cart),
        validateAddress = ValidateAddressUseCase(addresses),
        getShippingQuotes = GetShippingQuotesUseCase(shipping),
        redeemPromo = RedeemPromoUseCase(promos),
        placeOrder = PlaceOrderUseCase(orders),
    )
}
```

`feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/effect/CheckoutEffectHandlerTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.effect

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.CheckoutTestEnvironment
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.ShippingAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckoutEffectHandlerTest {

    private val environment = CheckoutTestEnvironment()
    private val handler = environment.effectHandler()
    private val request = OrderRequest(CHECKOUT_ID, HOME.id, STANDARD.id, VISA.id, promoCode = null)

    private suspend fun results(effect: CheckoutEffect): List<CheckoutAction> = handler.handle(effect).toList()

    @Test
    fun `observing the context reports the combined context`() = runTest {
        assertEquals(ContextAction.Updated(checkoutContext()), handler.handle(CheckoutEffect.ObserveContext).first())
    }

    @Test
    fun `a failed context load is reported`() = runTest {
        environment.addresses.addresses = Outcome.Failure(CheckoutError.Network)

        assertEquals(ContextAction.Failed(CheckoutError.Network), handler.handle(CheckoutEffect.ObserveContext).first())
    }

    @Test
    fun `a quantity write reports nothing because the cart stream does`() = runTest {
        assertEquals(emptyList<CheckoutAction>(), results(CheckoutEffect.UpdateQuantity(MUG.sku, 3)))
        assertEquals(3, environment.cart.cart.value.lines.single().quantity)
    }

    @Test
    fun `a failed quantity write is reported`() = runTest {
        environment.cart.failure = CheckoutError.Network

        assertEquals(listOf(CartAction.UpdateFailed(CheckoutError.Network)), results(CheckoutEffect.UpdateQuantity(MUG.sku, 3)))
    }

    @Test
    fun `an address check reports its result for that address`() = runTest {
        assertEquals(
            listOf(AddressAction.Validated(HOME.id, AddressValidation.Deliverable)),
            results(CheckoutEffect.ValidateAddress(HOME.id)),
        )
    }

    @Test
    fun `a failed address check is reported for that address`() = runTest {
        environment.addresses.validation = Outcome.Failure(CheckoutError.Network)

        assertEquals(
            listOf(AddressAction.ValidationFailed(HOME.id, CheckoutError.Network)),
            results(CheckoutEffect.ValidateAddress(HOME.id)),
        )
    }

    @Test
    fun `quotes are reported for their address`() = runTest {
        assertEquals(
            listOf(ShippingAction.QuotesLoaded(HOME.id, listOf(STANDARD, PICKUP, EXPRESS))),
            results(CheckoutEffect.LoadShippingQuotes(HOME.id, cartOf(MUG))),
        )
    }

    @Test
    fun `failed quotes are reported for their address`() = runTest {
        environment.shipping.quotes = Outcome.Failure(CheckoutError.Network)

        assertEquals(
            listOf(ShippingAction.QuotesFailed(HOME.id, CheckoutError.Network)),
            results(CheckoutEffect.LoadShippingQuotes(HOME.id, cartOf(MUG))),
        )
    }

    @Test
    fun `a redeemed promo is reported`() = runTest {
        assertEquals(listOf(PromoAction.Redeemed(SAVE5)), results(CheckoutEffect.RedeemPromo("SAVE5", cartOf(MUG))))
    }

    @Test
    fun `a rejected promo is reported`() = runTest {
        environment.promos.result = Outcome.Failure(CheckoutError.Promo.Expired)

        assertEquals(
            listOf(PromoAction.Rejected(CheckoutError.Promo.Expired)),
            results(CheckoutEffect.RedeemPromo("OLD", cartOf(MUG))),
        )
    }

    @Test
    fun `a placed order is reported`() = runTest {
        assertEquals(listOf(OrderAction.Placed(Order("order-1"))), results(CheckoutEffect.PlaceOrder(request)))
    }

    @Test
    fun `a failed order is reported`() = runTest {
        environment.orders.result = Outcome.Failure(CheckoutError.Payment.Declined)

        assertEquals(listOf(OrderAction.Failed(CheckoutError.Payment.Declined)), results(CheckoutEffect.PlaceOrder(request)))
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :feature:checkout:presentation:testDebugUnitTest`
Expected: FAIL. `Unresolved reference 'CheckoutEffectHandler'`.

- [ ] **Step 3: Implement the effect handler**

`feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/effect/CheckoutEffectHandler.kt`:

```kotlin
package com.example.feature.checkout.presentation.effect

import com.example.core.domain.fold
import com.example.core.mvi.EffectHandler
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.usecase.GetShippingQuotesUseCase
import com.example.feature.checkout.domain.usecase.ObserveCheckoutContextUseCase
import com.example.feature.checkout.domain.usecase.PlaceOrderUseCase
import com.example.feature.checkout.domain.usecase.RedeemPromoUseCase
import com.example.feature.checkout.domain.usecase.UpdateCartQuantityUseCase
import com.example.feature.checkout.domain.usecase.ValidateAddressUseCase
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.ShippingAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The bridge between the pure reducers and the outside world: effect, then one use case call, then
 * the action that reports the result. Decisions live in the reducers; I/O lives behind the use cases.
 */
internal class CheckoutEffectHandler @Inject constructor(
    private val observeCheckoutContext: ObserveCheckoutContextUseCase,
    private val updateCartQuantity: UpdateCartQuantityUseCase,
    private val validateAddress: ValidateAddressUseCase,
    private val getShippingQuotes: GetShippingQuotesUseCase,
    private val redeemPromo: RedeemPromoUseCase,
    private val placeOrder: PlaceOrderUseCase,
) : EffectHandler<CheckoutEffect, CheckoutAction> {

    override fun handle(effect: CheckoutEffect): Flow<CheckoutAction> = when (effect) {
        CheckoutEffect.ObserveContext ->
            observeCheckoutContext().map { outcome -> outcome.fold(ContextAction::Updated, ContextAction::Failed) }

        is CheckoutEffect.UpdateQuantity -> once(
            call = { updateCartQuantity(effect.sku, effect.quantity) },
            onSuccess = { null }, // the cart stream reports the new cart
            onFailure = CartAction::UpdateFailed,
        )

        is CheckoutEffect.ValidateAddress -> once(
            call = { validateAddress(effect.addressId) },
            onSuccess = { AddressAction.Validated(effect.addressId, it) },
            onFailure = { AddressAction.ValidationFailed(effect.addressId, it) },
        )

        is CheckoutEffect.LoadShippingQuotes -> once(
            call = { getShippingQuotes(effect.addressId, effect.cart) },
            onSuccess = { ShippingAction.QuotesLoaded(effect.addressId, it) },
            onFailure = { ShippingAction.QuotesFailed(effect.addressId, it) },
        )

        is CheckoutEffect.RedeemPromo -> once(
            call = { redeemPromo(effect.code, effect.cart) },
            onSuccess = PromoAction::Redeemed,
            onFailure = PromoAction::Rejected,
        )

        is CheckoutEffect.PlaceOrder -> once(
            call = { placeOrder(effect.request) },
            onSuccess = OrderAction::Placed,
            onFailure = OrderAction::Failed,
        )
    }

    /** A one-shot call that reports at most one action. */
    private fun <T> once(
        call: suspend () -> CheckoutOutcome<T>,
        onSuccess: (T) -> CheckoutAction?,
        onFailure: (CheckoutError) -> CheckoutAction,
    ): Flow<CheckoutAction> = flow {
        call().fold(onSuccess, onFailure)?.let { emit(it) }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :feature:checkout:presentation:testDebugUnitTest`
Expected: PASS. 65 tests.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(checkout): add the effect handler that runs one use case per effect"
```

---

### Task 11: Projectors, UI models and copy

**Files:**
- Create:
  - `feature/checkout/presentation/src/main/res/values/strings.xml`
  - `feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/ui/model/{CheckoutUiState,CartSectionUi,CartLineUi,SelectableRowUi,AddressSectionUi,AddressStatusUi,ShippingSectionUi,PromoSectionUi,PaymentSectionUi,SummaryUi,SummaryRowUi,PlaceOrderButtonUi}.kt`
  - `feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/projector/{ReadyCheckout,CheckoutErrorText,AddressReasonText,CartSectionProjector,AddressSectionProjector,ShippingSectionProjector,PromoSectionProjector,PaymentSectionProjector,SummarySectionProjector,PlaceOrderButtonProjector,CheckoutProjector}.kt`
- Test:
  - `feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/ProjectorFixtures.kt`
  - `feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/projector/{CheckoutProjectorTest,CartSectionProjectorTest,AddressSectionProjectorTest,ShippingSectionProjectorTest,PromoSectionProjectorTest,PaymentSectionProjectorTest,SummarySectionProjectorTest,PlaceOrderButtonProjectorTest}.kt`

**Interfaces:**
- Consumes: the contract and selectors (Task 8), `CalculateOrderTotalsUseCase` and `priceFor` (Tasks 3–4), `UiText` and `MoneyFormatter` (Task 7).
- Produces:
  - Public UI models (fields as in the code below).
  - `internal class CheckoutProjector @Inject constructor(cart, address, shipping, promo, payment, summary, placeOrderButton) { fun project(state: CheckoutState): CheckoutUiState }`
  - Each section projector has `fun project(ready: ReadyCheckout): <Section>Ui`; `PlaceOrderButtonProjector.project(state, total)`.
  - `internal fun CheckoutError.toUiText(): UiText`, `internal fun AddressValidation.Reason.toUiText(): UiText`
  - Test fixtures: `TestMoneyFormatter`, `checkoutProjector()`, `readyCheckout(state = readyState())`

- [ ] **Step 1: Write the copy**

`feature/checkout/presentation/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="checkout_title">Checkout</string>
    <string name="checkout_offline">You\'re offline. You can review your order, but not pay yet.</string>

    <plurals name="checkout_item_count">
        <item quantity="one">%d item</item>
        <item quantity="other">%d items</item>
    </plurals>
    <string name="checkout_cart_empty">Your cart is empty.</string>
    <string name="checkout_cd_decrease">Decrease the quantity of %1$s</string>
    <string name="checkout_cd_increase">Increase the quantity of %1$s</string>
    <string name="checkout_cd_remove">Remove %1$s from the cart</string>

    <string name="checkout_section_address">Deliver to</string>
    <string name="checkout_address_line">%1$s, %2$s %3$s</string>
    <string name="checkout_address_checking">Checking the address…</string>
    <string name="checkout_address_deliverable">We deliver to this address.</string>

    <string name="checkout_section_shipping">Shipping</string>
    <plurals name="checkout_delivery_days">
        <item quantity="one">Arrives in %d day</item>
        <item quantity="other">Arrives in %d days</item>
    </plurals>
    <string name="checkout_free_for_members">Free for members</string>
    <string name="checkout_free">Free</string>
    <string name="checkout_shipping_unavailable">Shipping options appear once the cart has items and an address is chosen.</string>

    <string name="checkout_promo_label">Promo code</string>
    <string name="checkout_promo_applied">%1$s applied: −%2$s</string>

    <string name="checkout_section_payment">Pay with</string>
    <string name="checkout_card_label">%1$s •••• %2$s</string>

    <string name="checkout_section_summary">Summary</string>
    <string name="checkout_subtotal">Subtotal</string>
    <string name="checkout_shipping">Shipping</string>
    <string name="checkout_shipping_pending">Choose an option</string>
    <string name="checkout_discount">Discount</string>
    <string name="checkout_discount_amount">−%1$s</string>
    <string name="checkout_pay_amount">Pay %1$s</string>

    <string name="action_retry">Retry</string>
    <string name="action_apply">Apply</string>
    <string name="action_remove">Remove</string>
    <string name="action_ok">OK</string>

    <string name="address_outside_area">We don\'t deliver to this address yet.</string>
    <string name="address_incomplete">This address is missing details.</string>
    <string name="address_po_box">We can\'t deliver to PO boxes.</string>

    <string name="error_network">No connection. Check your network and try again.</string>
    <string name="error_unauthorized">Your session expired. Please sign in again.</string>
    <string name="error_not_found">That item is no longer available.</string>
    <string name="error_promo_invalid">That code isn\'t valid.</string>
    <string name="error_promo_expired">That code has expired.</string>
    <string name="error_promo_minimum">Your order doesn\'t reach the minimum for this code.</string>
    <string name="error_payment_declined">Your payment was declined. Try another card.</string>
    <string name="error_payment_requires_action">Your bank needs you to confirm this payment.</string>
    <string name="error_out_of_stock">Some items just sold out.</string>
    <string name="error_price_changed">Prices changed. We\'ve updated your total; please review it.</string>
    <string name="error_generic">Something went wrong. Please try again.</string>

    <string name="confirmation_title">Order placed</string>
    <string name="confirmation_order_number">Order number %1$s</string>
    <string name="confirmation_back_to_checkout">Back to checkout</string>
</resources>
```

- [ ] **Step 2: Write the UI models**

All files in `feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/ui/model/`. They are exactly what Compose draws; no domain type appears past this point.

`CheckoutUiState.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

sealed interface CheckoutUiState {

    data object Loading : CheckoutUiState

    data class Error(val message: UiText) : CheckoutUiState

    data class Content(
        val isOffline: Boolean,
        val cart: CartSectionUi,
        val address: AddressSectionUi,
        val shipping: ShippingSectionUi,
        val promo: PromoSectionUi,
        val payment: PaymentSectionUi,
        val summary: SummaryUi,
        val placeOrder: PlaceOrderButtonUi,
        val blockingError: UiText?,
    ) : CheckoutUiState
}
```

`CartSectionUi.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

data class CartSectionUi(val title: UiText, val lines: List<CartLineUi>, val isEmpty: Boolean)
```

`CartLineUi.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.model

data class CartLineUi(
    val sku: String,
    val name: String,
    val quantity: Int,
    val lineTotal: String,
    val canDecrement: Boolean,
    val decrementRemovesLine: Boolean,
    val canIncrement: Boolean,
)
```

`SelectableRowUi.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

data class SelectableRowUi(
    val id: String,
    val title: UiText,
    val subtitle: UiText?,
    val trailing: UiText?,
    val selected: Boolean,
)
```

`AddressSectionUi.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.model

data class AddressSectionUi(val rows: List<SelectableRowUi>, val status: AddressStatusUi)
```

`AddressStatusUi.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

sealed interface AddressStatusUi {
    data object None : AddressStatusUi
    data object Checking : AddressStatusUi
    data object Deliverable : AddressStatusUi
    data class Warning(val message: UiText) : AddressStatusUi
}
```

`ShippingSectionUi.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

sealed interface ShippingSectionUi {
    data object Hidden : ShippingSectionUi
    data object Loading : ShippingSectionUi
    data class Error(val message: UiText) : ShippingSectionUi
    data class Options(val rows: List<SelectableRowUi>) : ShippingSectionUi
}
```

`PromoSectionUi.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

data class PromoSectionUi(
    val input: String,
    val inputEnabled: Boolean,
    val applyEnabled: Boolean,
    val isRedeeming: Boolean,
    val applied: UiText?,
    val error: UiText?,
)
```

`PaymentSectionUi.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.model

data class PaymentSectionUi(val rows: List<SelectableRowUi>)
```

`SummaryUi.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.model

data class SummaryUi(val rows: List<SummaryRowUi>, val total: String)
```

`SummaryRowUi.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

data class SummaryRowUi(val label: UiText, val value: UiText)
```

`PlaceOrderButtonUi.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

data class PlaceOrderButtonUi(val label: UiText, val enabled: Boolean, val loading: Boolean)
```

- [ ] **Step 3: Write the fixtures and the failing projector tests**

`feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/ProjectorFixtures.kt`:

```kotlin
package com.example.feature.checkout.presentation

import com.example.core.ui.MoneyFormatter
import com.example.feature.checkout.domain.usecase.CalculateOrderTotalsUseCase
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.projector.AddressSectionProjector
import com.example.feature.checkout.presentation.projector.CartSectionProjector
import com.example.feature.checkout.presentation.projector.CheckoutProjector
import com.example.feature.checkout.presentation.projector.PaymentSectionProjector
import com.example.feature.checkout.presentation.projector.PlaceOrderButtonProjector
import com.example.feature.checkout.presentation.projector.PromoSectionProjector
import com.example.feature.checkout.presentation.projector.ReadyCheckout
import com.example.feature.checkout.presentation.projector.ShippingSectionProjector
import com.example.feature.checkout.presentation.projector.SummarySectionProjector

/** Formats amounts the same way on every machine, whatever the JVM's default locale. */
internal val TestMoneyFormatter = MoneyFormatter { "€" + it.amount.toPlainString() }

internal fun checkoutProjector(): CheckoutProjector = CheckoutProjector(
    cart = CartSectionProjector(TestMoneyFormatter),
    address = AddressSectionProjector(),
    shipping = ShippingSectionProjector(TestMoneyFormatter),
    promo = PromoSectionProjector(TestMoneyFormatter),
    payment = PaymentSectionProjector(),
    summary = SummarySectionProjector(TestMoneyFormatter, CalculateOrderTotalsUseCase()),
    placeOrderButton = PlaceOrderButtonProjector(),
)

/** The input of a section projector, built from a state whose context has loaded. */
internal fun readyCheckout(state: CheckoutState = readyState()): ReadyCheckout =
    ReadyCheckout(state, (state.context as Loadable.Ready).value)
```

All tests below are in `feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/projector/`. Projectors are pure, so the tests assert on `UiText` values with no Context and no Robolectric.

`CheckoutProjectorTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.checkoutProjector
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutProjectorTest {

    private val projector = checkoutProjector()

    private fun content(state: CheckoutState) = projector.project(state) as CheckoutUiState.Content

    @Test
    fun `a loading context shows the loading screen`() {
        assertEquals(CheckoutUiState.Loading, projector.project(CheckoutState(checkoutId = CHECKOUT_ID)))
    }

    @Test
    fun `a failed context shows a full-screen error`() {
        val failed = CheckoutState(checkoutId = CHECKOUT_ID, context = Loadable.Failed(CheckoutError.Network))

        assertEquals(CheckoutUiState.Error(UiText.res(R.string.error_network)), projector.project(failed))
    }

    @Test
    fun `being offline shows the banner`() {
        assertTrue(content(readyState(checkoutContext(isOnline = false))).isOffline)
        assertFalse(content(readyState()).isOffline)
    }

    @Test
    fun `the pay button shows the total`() {
        // 2 x 12.00 mugs + 4.99 standard shipping
        assertEquals(UiText.res(R.string.checkout_pay_amount, "€28.99"), content(readyState()).placeOrder.label)
    }

    @Test
    fun `a failed order becomes a blocking error`() {
        val state = readyState().copy(submission = Submission.Failed(CheckoutError.Payment.Declined))

        assertEquals(UiText.res(R.string.error_payment_declined), content(state).blockingError)
    }
}
```

`CartSectionProjectorTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.TEE
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.rule.MAX_QUANTITY_PER_LINE
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.TestMoneyFormatter
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyCheckout
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.CartLineUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartSectionProjectorTest {

    private val projector = CartSectionProjector(TestMoneyFormatter)

    @Test
    fun `lines show their totals and the title counts the items`() {
        val cart = projector.project(readyCheckout())

        assertEquals(UiText.plural(R.plurals.checkout_item_count, 2, 2), cart.title)
        assertEquals(
            listOf(CartLineUi(MUG.sku, MUG.name, 2, "€24.00", canDecrement = true, decrementRemovesLine = false, canIncrement = true)),
            cart.lines,
        )
    }

    @Test
    fun `decrementing a single unit removes the line`() {
        val cart = projector.project(readyCheckout(readyState(checkoutContext(cart = cartOf(TEE)))))

        assertTrue(cart.lines.single().decrementRemovesLine)
    }

    @Test
    fun `the plus button stops at the per-line maximum`() {
        val full = cartOf(MUG.copy(quantity = MAX_QUANTITY_PER_LINE))

        assertFalse(projector.project(readyCheckout(readyState(checkoutContext(cart = full)))).lines.single().canIncrement)
    }

    @Test
    fun `lines cannot be edited while the order is being placed`() {
        val line = projector.project(readyCheckout(readyState().copy(submission = Submission.Submitting))).lines.single()

        assertFalse(line.canDecrement)
        assertFalse(line.canIncrement)
    }

    @Test
    fun `an empty cart is flagged`() {
        val cart = projector.project(readyCheckout(readyState(checkoutContext(cart = cartOf()))))

        assertTrue(cart.isEmpty)
        assertTrue(cart.lines.isEmpty())
    }
}
```

`AddressSectionProjectorTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.readyCheckout
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.AddressStatusUi
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressSectionProjectorTest {

    private val projector = AddressSectionProjector()

    @Test
    fun `each address shows its label and address line, with the selection marked`() {
        val home = projector.project(readyCheckout()).rows.first { it.id == HOME.id }

        assertEquals(
            SelectableRowUi(
                id = HOME.id,
                title = UiText.Raw(HOME.label),
                subtitle = UiText.res(R.string.checkout_address_line, HOME.line1, HOME.postalCode, HOME.city),
                trailing = null,
                selected = true,
            ),
            home,
        )
    }

    @Test
    fun `the check status is described in words`() {
        val poBox = AddressValidation.Undeliverable(AddressValidation.Reason.PO_BOX_NOT_ALLOWED)

        assertEquals(AddressStatusUi.None, statusFor(Loadable.Idle))
        assertEquals(AddressStatusUi.Checking, statusFor(Loadable.Loading))
        assertEquals(AddressStatusUi.Deliverable, statusFor(Loadable.Ready(AddressValidation.Deliverable)))
        assertEquals(AddressStatusUi.Warning(UiText.res(R.string.address_po_box)), statusFor(Loadable.Ready(poBox)))
        assertEquals(AddressStatusUi.Warning(UiText.res(R.string.error_network)), statusFor(Loadable.Failed(CheckoutError.Network)))
    }

    private fun statusFor(validation: Loadable<AddressValidation>): AddressStatusUi =
        projector.project(readyCheckout(readyState().copy(address = AddressSlice(HOME.id, validation)))).status
}
```

`ShippingSectionProjectorTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.MEMBER
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.TestMoneyFormatter
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.readyCheckout
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.ShippingSectionUi
import org.junit.Assert.assertEquals
import org.junit.Test

class ShippingSectionProjectorTest {

    private val projector = ShippingSectionProjector(TestMoneyFormatter)

    private fun options(state: CheckoutState) = projector.project(readyCheckout(state)) as ShippingSectionUi.Options

    private fun section(shipping: ShippingSlice) = projector.project(readyCheckout(readyState().copy(shipping = shipping)))

    @Test
    fun `regular customers see each price and delivery time`() {
        val standard = options(readyState()).rows.first { it.id == STANDARD.id }

        assertEquals(UiText.Raw("€4.99"), standard.trailing)
        assertEquals(UiText.plural(R.plurals.checkout_delivery_days, 3, 3), standard.subtitle)
    }

    @Test
    fun `members see standard shipping as free but still pay for express`() {
        val rows = options(readyState(checkoutContext(customer = MEMBER))).rows

        assertEquals(UiText.res(R.string.checkout_free_for_members), rows.first { it.id == STANDARD.id }.trailing)
        assertEquals(UiText.Raw("€12.99"), rows.first { it.id == EXPRESS.id }.trailing)
    }

    @Test
    fun `express options are hidden while the flag is off`() {
        val rows = options(readyState(checkoutContext(isExpressShippingEnabled = false))).rows

        assertEquals(listOf(STANDARD.id, PICKUP.id), rows.map { it.id })
    }

    @Test
    fun `waiting, failed and missing quotes each have their own state`() {
        assertEquals(ShippingSectionUi.Loading, section(ShippingSlice(Loadable.Loading)))
        assertEquals(ShippingSectionUi.Error(UiText.res(R.string.error_network)), section(ShippingSlice(Loadable.Failed(CheckoutError.Network))))
        assertEquals(ShippingSectionUi.Hidden, section(ShippingSlice()))
    }
}
```

`PromoSectionProjectorTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.TestMoneyFormatter
import com.example.feature.checkout.presentation.contract.PromoSlice
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.readyCheckout
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromoSectionProjectorTest {

    private val projector = PromoSectionProjector(TestMoneyFormatter)

    private fun promo(slice: PromoSlice) = projector.project(readyCheckout(readyState().copy(promo = slice)))

    @Test
    fun `a typed code can be applied`() {
        val ui = promo(PromoSlice(input = "SAVE5"))

        assertTrue(ui.inputEnabled)
        assertTrue(ui.applyEnabled)
    }

    @Test
    fun `a blank code cannot be applied`() {
        assertFalse(promo(PromoSlice(input = " ")).applyEnabled)
    }

    @Test
    fun `checking a code locks the field`() {
        val ui = promo(PromoSlice("SAVE5", PromoStatus.Redeeming("SAVE5")))

        assertFalse(ui.inputEnabled)
        assertTrue(ui.isRedeeming)
    }

    @Test
    fun `an applied promo shows its discount`() {
        assertEquals(
            UiText.res(R.string.checkout_promo_applied, "SAVE5", "€5.00"),
            promo(PromoSlice("SAVE5", PromoStatus.Applied(SAVE5))).applied,
        )
    }

    @Test
    fun `a rejected code explains why and can be edited`() {
        val ui = promo(PromoSlice("OLD", PromoStatus.Rejected(CheckoutError.Promo.Expired)))

        assertEquals(UiText.res(R.string.error_promo_expired), ui.error)
        assertTrue(ui.inputEnabled)
    }
}
```

`PaymentSectionProjectorTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.fixture.MASTERCARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.readyCheckout
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PaymentSectionProjectorTest {

    @Test
    fun `each card shows its brand and last digits, with the selection marked`() {
        val rows = PaymentSectionProjector().project(readyCheckout()).rows

        assertEquals(
            SelectableRowUi(VISA.id, UiText.res(R.string.checkout_card_label, "Visa", "4242"), subtitle = null, trailing = null, selected = true),
            rows.first(),
        )
        assertFalse(rows.first { it.id == MASTERCARD.id }.selected)
    }
}
```

`SummarySectionProjectorTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.fixture.MEMBER
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.usecase.CalculateOrderTotalsUseCase
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.TestMoneyFormatter
import com.example.feature.checkout.presentation.contract.PromoSlice
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.readyCheckout
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.SummaryRowUi
import org.junit.Assert.assertEquals
import org.junit.Test

class SummarySectionProjectorTest {

    private val projector = SummarySectionProjector(TestMoneyFormatter, CalculateOrderTotalsUseCase())

    @Test
    fun `shows the subtotal, the shipping and the total`() {
        val summary = projector.project(readyCheckout())

        assertEquals(
            listOf(
                SummaryRowUi(UiText.res(R.string.checkout_subtotal), UiText.Raw("€24.00")),
                SummaryRowUi(UiText.res(R.string.checkout_shipping), UiText.Raw("€4.99")),
            ),
            summary.rows,
        )
        assertEquals("€28.99", summary.total)
    }

    @Test
    fun `free shipping says so`() {
        val summary = projector.project(readyCheckout(readyState(checkoutContext(customer = MEMBER))))

        assertEquals(UiText.res(R.string.checkout_free), summary.rows[1].value)
    }

    @Test
    fun `asks for a shipping choice until one is made`() {
        val summary = projector.project(readyCheckout(readyState().copy(shipping = ShippingSlice())))

        assertEquals(UiText.res(R.string.checkout_shipping_pending), summary.rows[1].value)
    }

    @Test
    fun `an applied promo adds a discount row`() {
        val summary = projector.project(readyCheckout(readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Applied(SAVE5)))))

        assertEquals(
            SummaryRowUi(UiText.res(R.string.checkout_discount), UiText.res(R.string.checkout_discount_amount, "€5.00")),
            summary.rows.last(),
        )
        assertEquals("€23.99", summary.total)
    }
}
```

`PlaceOrderButtonProjectorTest.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.PlaceOrderButtonUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceOrderButtonProjectorTest {

    private val projector = PlaceOrderButtonProjector()

    @Test
    fun `a ready checkout can pay the shown total`() {
        assertEquals(
            PlaceOrderButtonUi(UiText.res(R.string.checkout_pay_amount, "€28.99"), enabled = true, loading = false),
            projector.project(readyState(), total = "€28.99"),
        )
    }

    @Test
    fun `paying is disabled while offline`() {
        assertFalse(projector.project(readyState(checkoutContext(isOnline = false)), total = "€28.99").enabled)
    }

    @Test
    fun `the button spins and is disabled while the order is being placed`() {
        val button = projector.project(readyState().copy(submission = Submission.Submitting), total = "€28.99")

        assertTrue(button.loading)
        assertFalse(button.enabled)
    }
}
```

- [ ] **Step 4: Run the tests to verify they fail**

Run: `./gradlew :feature:checkout:presentation:testDebugUnitTest`
Expected: FAIL. `Unresolved reference 'CheckoutProjector'`, `'ReadyCheckout'` and the section projectors.

- [ ] **Step 5: Implement the projectors**

All files in `feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/projector/`.

`ReadyCheckout.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.presentation.contract.CheckoutState

/** The input of a section projector. It exists only once the context has loaded, so sections need no null checks. */
internal data class ReadyCheckout(val state: CheckoutState, val context: CheckoutContext)
```

`CheckoutErrorText.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.presentation.R

/** The single mapping from checkout errors to what the customer reads. */
internal fun CheckoutError.toUiText(): UiText = UiText.res(
    when (this) {
        CheckoutError.Network -> R.string.error_network
        CheckoutError.Unauthorized -> R.string.error_unauthorized
        CheckoutError.NotFound -> R.string.error_not_found
        is CheckoutError.Unexpected -> R.string.error_generic
        CheckoutError.Promo.Invalid -> R.string.error_promo_invalid
        CheckoutError.Promo.Expired -> R.string.error_promo_expired
        CheckoutError.Promo.MinimumNotReached -> R.string.error_promo_minimum
        CheckoutError.Payment.Declined -> R.string.error_payment_declined
        CheckoutError.Payment.RequiresAction -> R.string.error_payment_requires_action
        CheckoutError.OutOfStock -> R.string.error_out_of_stock
        CheckoutError.PriceChanged -> R.string.error_price_changed
    },
)
```

`AddressReasonText.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.presentation.R

internal fun AddressValidation.Reason.toUiText(): UiText = UiText.res(
    when (this) {
        AddressValidation.Reason.OUTSIDE_SERVICE_AREA -> R.string.address_outside_area
        AddressValidation.Reason.INCOMPLETE -> R.string.address_incomplete
        AddressValidation.Reason.PO_BOX_NOT_ALLOWED -> R.string.address_po_box
    },
)
```

`CartSectionProjector.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.MoneyFormatter
import com.example.core.ui.UiText
import com.example.feature.checkout.domain.rule.MAX_QUANTITY_PER_LINE
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.ui.model.CartLineUi
import com.example.feature.checkout.presentation.ui.model.CartSectionUi
import javax.inject.Inject

internal class CartSectionProjector @Inject constructor(private val money: MoneyFormatter) {

    fun project(ready: ReadyCheckout): CartSectionUi {
        val cart = ready.context.cart
        val editable = ready.state.submission != Submission.Submitting
        return CartSectionUi(
            title = UiText.plural(R.plurals.checkout_item_count, cart.itemCount, cart.itemCount),
            lines = cart.lines.map { line ->
                CartLineUi(
                    sku = line.sku,
                    name = line.name,
                    quantity = line.quantity,
                    lineTotal = money.format(line.lineTotal),
                    canDecrement = editable,
                    decrementRemovesLine = line.quantity == 1,
                    canIncrement = editable && line.quantity < MAX_QUANTITY_PER_LINE,
                )
            },
            isEmpty = cart.isEmpty,
        )
    }
}
```

`AddressSectionProjector.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.ui.model.AddressSectionUi
import com.example.feature.checkout.presentation.ui.model.AddressStatusUi
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import javax.inject.Inject

internal class AddressSectionProjector @Inject constructor() {

    fun project(ready: ReadyCheckout): AddressSectionUi {
        val slice = ready.state.address
        return AddressSectionUi(
            rows = ready.context.addresses.map { address ->
                SelectableRowUi(
                    id = address.id,
                    title = UiText.Raw(address.label),
                    subtitle = UiText.res(R.string.checkout_address_line, address.line1, address.postalCode, address.city),
                    trailing = null,
                    selected = address.id == slice.selectedId,
                )
            },
            status = when (val validation = slice.validation) {
                Loadable.Idle -> AddressStatusUi.None
                Loadable.Loading -> AddressStatusUi.Checking
                is Loadable.Failed -> AddressStatusUi.Warning(validation.error.toUiText())
                is Loadable.Ready -> when (val result = validation.value) {
                    AddressValidation.Deliverable -> AddressStatusUi.Deliverable
                    is AddressValidation.Undeliverable -> AddressStatusUi.Warning(result.reason.toUiText())
                }
            },
        )
    }
}
```

`ShippingSectionProjector.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.MoneyFormatter
import com.example.core.ui.UiText
import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.rule.priceFor
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.availableShippingOptions
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import com.example.feature.checkout.presentation.ui.model.ShippingSectionUi
import javax.inject.Inject

internal class ShippingSectionProjector @Inject constructor(private val money: MoneyFormatter) {

    fun project(ready: ReadyCheckout): ShippingSectionUi = when (val quotes = ready.state.shipping.quotes) {
        Loadable.Idle -> ShippingSectionUi.Hidden
        Loadable.Loading -> ShippingSectionUi.Loading
        is Loadable.Failed -> ShippingSectionUi.Error(quotes.error.toUiText())
        is Loadable.Ready -> ShippingSectionUi.Options(
            ready.state.availableShippingOptions.map { option ->
                SelectableRowUi(
                    id = option.id,
                    title = UiText.Raw(option.label),
                    subtitle = UiText.plural(R.plurals.checkout_delivery_days, option.estimatedDays, option.estimatedDays),
                    trailing = priceText(option, ready.context.customer),
                    selected = option.id == ready.state.shipping.selectedId,
                )
            },
        )
    }

    private fun priceText(option: ShippingOption, customer: Customer): UiText {
        val price = option.priceFor(customer)
        return when {
            price.isZero && customer.isMember -> UiText.res(R.string.checkout_free_for_members)
            price.isZero -> UiText.res(R.string.checkout_free)
            else -> UiText.Raw(money.format(price))
        }
    }
}
```

`PromoSectionProjector.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.MoneyFormatter
import com.example.core.ui.UiText
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.appliedPromo
import com.example.feature.checkout.presentation.ui.model.PromoSectionUi
import javax.inject.Inject

internal class PromoSectionProjector @Inject constructor(private val money: MoneyFormatter) {

    fun project(ready: ReadyCheckout): PromoSectionUi {
        val promo = ready.state.promo
        val editable = promo.status is PromoStatus.None || promo.status is PromoStatus.Rejected
        return PromoSectionUi(
            input = promo.input,
            inputEnabled = editable,
            applyEnabled = editable && promo.input.isNotBlank() && !ready.context.cart.isEmpty,
            isRedeeming = promo.status is PromoStatus.Redeeming,
            applied = ready.state.appliedPromo?.let { UiText.res(R.string.checkout_promo_applied, it.code, money.format(it.discount)) },
            error = (promo.status as? PromoStatus.Rejected)?.error?.toUiText(),
        )
    }
}
```

`PaymentSectionProjector.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.PaymentSectionUi
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import javax.inject.Inject

internal class PaymentSectionProjector @Inject constructor() {

    fun project(ready: ReadyCheckout): PaymentSectionUi = PaymentSectionUi(
        rows = ready.context.paymentMethods.map { method ->
            SelectableRowUi(
                id = method.id,
                title = UiText.res(R.string.checkout_card_label, method.brand, method.last4),
                subtitle = null,
                trailing = null,
                selected = method.id == ready.state.selectedPaymentId,
            )
        },
    )
}
```

`SummarySectionProjector.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.domain.Money
import com.example.core.ui.MoneyFormatter
import com.example.core.ui.UiText
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.usecase.CalculateOrderTotalsUseCase
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.appliedPromo
import com.example.feature.checkout.presentation.contract.selectedShippingOption
import com.example.feature.checkout.presentation.ui.model.SummaryRowUi
import com.example.feature.checkout.presentation.ui.model.SummaryUi
import javax.inject.Inject

/** Formats the totals. Computing them is a domain rule, so it is left to [CalculateOrderTotalsUseCase]. */
internal class SummarySectionProjector @Inject constructor(
    private val money: MoneyFormatter,
    private val calculateOrderTotals: CalculateOrderTotalsUseCase,
) {

    fun project(ready: ReadyCheckout): SummaryUi {
        val shippingOption = ready.state.selectedShippingOption
        val totals = calculateOrderTotals(ready.context.cart, shippingOption, ready.state.appliedPromo, ready.context.customer)
        val rows = buildList<SummaryRowUi> {
            add(SummaryRowUi(UiText.res(R.string.checkout_subtotal), UiText.Raw(money.format(totals.subtotal))))
            add(SummaryRowUi(UiText.res(R.string.checkout_shipping), shippingText(shippingOption, totals.shipping)))
            if (!totals.discount.isZero) {
                add(
                    SummaryRowUi(
                        UiText.res(R.string.checkout_discount),
                        UiText.res(R.string.checkout_discount_amount, money.format(totals.discount)),
                    ),
                )
            }
        }
        return SummaryUi(rows = rows, total = money.format(totals.total))
    }

    private fun shippingText(option: ShippingOption?, cost: Money): UiText = when {
        option == null -> UiText.res(R.string.checkout_shipping_pending)
        cost.isZero -> UiText.res(R.string.checkout_free)
        else -> UiText.Raw(money.format(cost))
    }
}
```

`PlaceOrderButtonProjector.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.contract.orderRequestOrNull
import com.example.feature.checkout.presentation.ui.model.PlaceOrderButtonUi
import javax.inject.Inject

internal class PlaceOrderButtonProjector @Inject constructor() {

    fun project(state: CheckoutState, total: String): PlaceOrderButtonUi = PlaceOrderButtonUi(
        label = UiText.res(R.string.checkout_pay_amount, total),
        enabled = state.orderRequestOrNull() != null, // the same rule the order reducer guards with
        loading = state.submission == Submission.Submitting,
    )
}
```

`CheckoutProjector.kt`:

```kotlin
package com.example.feature.checkout.presentation.projector

import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import javax.inject.Inject

/**
 * Turns state into what the screen draws: decides the overall shape, then lets each section projector
 * describe its section. A reducer answers "what happened?"; a projector answers "what does it look like?".
 */
internal class CheckoutProjector @Inject constructor(
    private val cart: CartSectionProjector,
    private val address: AddressSectionProjector,
    private val shipping: ShippingSectionProjector,
    private val promo: PromoSectionProjector,
    private val payment: PaymentSectionProjector,
    private val summary: SummarySectionProjector,
    private val placeOrderButton: PlaceOrderButtonProjector,
) {

    fun project(state: CheckoutState): CheckoutUiState = when (val context = state.context) {
        Loadable.Idle, Loadable.Loading -> CheckoutUiState.Loading
        is Loadable.Failed -> CheckoutUiState.Error(context.error.toUiText())
        is Loadable.Ready -> {
            val ready = ReadyCheckout(state, context.value)
            val summaryUi = summary.project(ready)
            CheckoutUiState.Content(
                isOffline = !context.value.isOnline,
                cart = cart.project(ready),
                address = address.project(ready),
                shipping = shipping.project(ready),
                promo = promo.project(ready),
                payment = payment.project(ready),
                summary = summaryUi,
                placeOrder = placeOrderButton.project(state, summaryUi.total),
                blockingError = (state.submission as? Submission.Failed)?.error?.toUiText(),
            )
        }
    }
}
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :feature:checkout:presentation:testDebugUnitTest`
Expected: PASS. 94 tests.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(checkout): add section projectors, UI models and checkout copy"
```

---

### Task 12: The ViewModel, end to end

**Files:**
- Create: `feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/CheckoutViewModel.kt`
- Test:
  - `feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/MainDispatcherRule.kt`
  - `feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/CheckoutViewModelTest.kt`

**Interfaces:**
- Consumes: `Store` (Task 1), `CheckoutReducer` (Task 9), `CheckoutEffectHandler` (Task 10), `CheckoutProjector` (Task 11).
- Produces:
  - `@HiltViewModel class CheckoutViewModel @Inject internal constructor(reducer, effectHandler, projector)`
  - `val uiState: StateFlow<CheckoutUiState>`
  - `val events: Flow<CheckoutEvent>`
  - `fun onIntent(intent: CheckoutIntent)`

- [ ] **Step 1: Write the failing end-to-end tests**

`feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/MainDispatcherRule.kt`:

```kotlin
package com.example.feature.checkout.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Runs viewModelScope on a test dispatcher, so each test decides when queued work runs. */
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {

    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

`feature/checkout/presentation/src/test/kotlin/com/example/feature/checkout/presentation/CheckoutViewModelTest.kt`:

```kotlin
package com.example.feature.checkout.presentation

import app.cash.turbine.test
import com.example.core.domain.Outcome
import com.example.core.ui.UiText
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fake.FakeAddressRepository
import com.example.feature.checkout.domain.fake.FakeOrderRepository
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.OFFICE
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import com.example.feature.checkout.presentation.ui.model.ShippingSectionUi
import com.example.feature.checkout.presentation.ui.model.SummaryRowUi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The whole loop on the JVM: the real store, reducers, effect handler, use cases and projector over fake repositories. */
class CheckoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Creates the ViewModel, keeps its UI state subscribed and lets the first load finish. */
    private fun TestScope.started(environment: CheckoutTestEnvironment = CheckoutTestEnvironment()): CheckoutViewModel {
        val viewModel = CheckoutViewModel(checkoutReducer(), environment.effectHandler(), checkoutProjector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        return viewModel
    }

    private val CheckoutViewModel.content: CheckoutUiState.Content
        get() = uiState.value as CheckoutUiState.Content

    @Test
    fun `starting shows the checkout with the defaults chosen`() = runTest {
        val content = started().content

        assertTrue(content.address.rows.first { it.id == HOME.id }.selected)
        assertTrue(content.payment.rows.first { it.id == VISA.id }.selected)
        assertTrue((content.shipping as ShippingSectionUi.Options).rows.first { it.id == PICKUP.id }.selected)
        // 2 x 12.00 mugs + 2.99 pickup, the cheapest option for a regular customer
        assertEquals(UiText.res(R.string.checkout_pay_amount, "€26.99"), content.placeOrder.label)
        assertTrue(content.placeOrder.enabled)
    }

    @Test
    fun `changing a quantity updates the cart and re-quotes shipping`() = runTest {
        val environment = CheckoutTestEnvironment()
        val viewModel = started(environment)

        viewModel.onIntent(CartAction.QuantityChanged(MUG.sku, 3))
        advanceUntilIdle()

        assertEquals(3, environment.cart.cart.value.lines.single().quantity)
        assertEquals(listOf(HOME.id, HOME.id), environment.shipping.quotedAddressIds)
    }

    @Test
    fun `applying a promo shows the discount`() = runTest {
        val environment = CheckoutTestEnvironment()
        val viewModel = started(environment)

        viewModel.onIntent(PromoAction.InputChanged(" save5 "))
        viewModel.onIntent(PromoAction.ApplyClicked)
        advanceUntilIdle()

        assertEquals(listOf("SAVE5"), environment.promos.redeemedCodes)
        assertEquals(
            SummaryRowUi(UiText.res(R.string.checkout_discount), UiText.res(R.string.checkout_discount_amount, "€5.00")),
            viewModel.content.summary.rows.last(),
        )
    }

    @Test
    fun `placing the order navigates to the confirmation once`() = runTest {
        val environment = CheckoutTestEnvironment()
        val viewModel = started(environment)

        viewModel.events.test {
            viewModel.onIntent(OrderAction.PlaceClicked)
            assertEquals(CheckoutEvent.NavigateToConfirmation("order-1"), awaitItem())
        }

        val request = environment.orders.placedRequests.single()
        assertEquals(OrderRequest(request.checkoutId, HOME.id, PICKUP.id, VISA.id, promoCode = null), request)
        assertTrue(request.checkoutId.isNotBlank())
    }

    @Test
    fun `a declined card shows a blocking error until dismissed`() = runTest {
        val environment = CheckoutTestEnvironment(orders = FakeOrderRepository(Outcome.Failure(CheckoutError.Payment.Declined)))
        val viewModel = started(environment)

        viewModel.onIntent(OrderAction.PlaceClicked)
        advanceUntilIdle()
        assertEquals(UiText.res(R.string.error_payment_declined), viewModel.content.blockingError)

        viewModel.onIntent(OrderAction.ErrorDismissed)
        advanceUntilIdle()
        assertNull(viewModel.content.blockingError)
        assertTrue(viewModel.content.placeOrder.enabled)
    }

    @Test
    fun `going offline shows the banner and blocks paying until reconnected`() = runTest {
        val environment = CheckoutTestEnvironment()
        val viewModel = started(environment)

        environment.network.online.value = false
        advanceUntilIdle()
        assertTrue(viewModel.content.isOffline)
        assertFalse(viewModel.content.placeOrder.enabled)

        environment.network.online.value = true
        advanceUntilIdle()
        assertFalse(viewModel.content.isOffline)
        assertTrue(viewModel.content.placeOrder.enabled)
    }

    @Test
    fun `a failed first load shows the error and retrying recovers`() = runTest {
        val environment = CheckoutTestEnvironment(
            addresses = FakeAddressRepository(addresses = Outcome.Failure(CheckoutError.Network)),
        )
        val viewModel = started(environment)
        assertEquals(CheckoutUiState.Error(UiText.res(R.string.error_network)), viewModel.uiState.value)

        environment.addresses.addresses = Outcome.Success(listOf(HOME, OFFICE))
        viewModel.onIntent(LifecycleAction.RetryLoadClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CheckoutUiState.Content)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :feature:checkout:presentation:testDebugUnitTest`
Expected: FAIL. `Unresolved reference 'CheckoutViewModel'`.

- [ ] **Step 3: Implement the ViewModel**

`feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/CheckoutViewModel.kt`:

```kotlin
package com.example.feature.checkout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.mvi.Store
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutIntent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.effect.CheckoutEffectHandler
import com.example.feature.checkout.presentation.projector.CheckoutProjector
import com.example.feature.checkout.presentation.reducer.CheckoutReducer
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject

/**
 * The checkout's lifecycle adapter: owns the store's scope, exposes projected UI state and one-off
 * events, and accepts intents. It holds no business rules, so it stays this size however much the
 * screen grows.
 */
@HiltViewModel
class CheckoutViewModel @Inject internal constructor(
    reducer: CheckoutReducer,
    effectHandler: CheckoutEffectHandler,
    private val projector: CheckoutProjector,
) : ViewModel() {

    private val store = Store(
        initialState = CheckoutState(checkoutId = UUID.randomUUID().toString()),
        scope = viewModelScope,
        reducer = reducer,
        effectHandler = effectHandler,
    )

    val uiState: StateFlow<CheckoutUiState> = store.state
        .map(projector::project)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), projector.project(store.state.value))

    val events: Flow<CheckoutEvent> = store.events

    init {
        store.dispatch(LifecycleAction.Started)
    }

    /** The UI can only send intents; effect results are not part of its vocabulary. */
    fun onIntent(intent: CheckoutIntent) = store.dispatch(intent)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :feature:checkout:presentation:testDebugUnitTest`
Expected: PASS. 101 tests.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(checkout): add the CheckoutViewModel running one store per screen"
```

---

### Task 13: Compose UI, confirmation screen and navigation

**Files:**
- Create:
  - `feature/checkout/presentation/src/main/res/drawable/{ic_add,ic_remove,ic_delete}.xml`
  - Under `feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/`:
    - `ui/preview/CheckoutPreviewData.kt`
    - `ui/component/{SectionHeader,SelectableRow,OfflineBanner,CartSection,AddressSection,ShippingSection,PromoSection,PaymentSection,SummarySection,PlaceOrderButton,ErrorState,BlockingErrorDialog}.kt`
    - `ui/CheckoutScreen.kt`
    - `ui/confirmation/OrderConfirmationScreen.kt`
    - `navigation/{CheckoutDestination,OrderConfirmationDestination,CheckoutNavigation}.kt`

**Interfaces:**
- Consumes: `CheckoutViewModel` (Task 12), UI models and copy (Task 11), `ObserveAsEvents`, `asString` and `ShopTheme` (Task 7).
- Produces:
  - `@Composable fun CheckoutRoute(onOrderPlaced: (orderId: String) -> Unit, viewModel: CheckoutViewModel = hiltViewModel())`
  - `@Composable fun CheckoutScreen(state, onIntent, modifier, snackbarHostState)`
  - `@Composable fun OrderConfirmationScreen(orderId, onBackToCheckout, modifier)`
  - `@Serializable data object CheckoutDestination`, `@Serializable data class OrderConfirmationDestination(val orderId: String)`
  - `NavGraphBuilder.checkoutScreen(onOrderPlaced)`, `NavGraphBuilder.orderConfirmationScreen(onBackToCheckout)`

- [ ] **Step 1: Add the icons**

`feature/checkout/presentation/src/main/res/drawable/ic_add.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
</vector>
```

`feature/checkout/presentation/src/main/res/drawable/ic_remove.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M19,13H5v-2h14v2z" />
</vector>
```

`feature/checkout/presentation/src/main/res/drawable/ic_delete.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z" />
</vector>
```

- [ ] **Step 2: Write the preview data and the shared components**

All Kotlin files below are in `feature/checkout/presentation/src/main/kotlin/com/example/feature/checkout/presentation/`.

`ui/preview/CheckoutPreviewData.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.preview

import com.example.core.ui.UiText
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.AddressSectionUi
import com.example.feature.checkout.presentation.ui.model.AddressStatusUi
import com.example.feature.checkout.presentation.ui.model.CartLineUi
import com.example.feature.checkout.presentation.ui.model.CartSectionUi
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import com.example.feature.checkout.presentation.ui.model.PaymentSectionUi
import com.example.feature.checkout.presentation.ui.model.PlaceOrderButtonUi
import com.example.feature.checkout.presentation.ui.model.PromoSectionUi
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import com.example.feature.checkout.presentation.ui.model.ShippingSectionUi
import com.example.feature.checkout.presentation.ui.model.SummaryRowUi
import com.example.feature.checkout.presentation.ui.model.SummaryUi

/** Sample UI state for previews only. Amounts arrive pre-formatted, as a projector delivers them. */
internal object CheckoutPreviewData {

    val cart = CartSectionUi(
        title = UiText.plural(R.plurals.checkout_item_count, 3, 3),
        lines = listOf(
            CartLineUi("mug", "Coffee mug", 2, "€24.00", canDecrement = true, decrementRemovesLine = false, canIncrement = true),
            CartLineUi("tee", "T-shirt", 1, "€20.00", canDecrement = true, decrementRemovesLine = true, canIncrement = true),
        ),
        isEmpty = false,
    )

    val address = AddressSectionUi(
        rows = listOf(
            SelectableRowUi("home", UiText.Raw("Home"), UiText.Raw("Calle Larios 1, 29005 Málaga"), trailing = null, selected = true),
            SelectableRowUi("office", UiText.Raw("Office"), UiText.Raw("Av. de Andalucía 10, 29007 Málaga"), trailing = null, selected = false),
        ),
        status = AddressStatusUi.Deliverable,
    )

    val shipping = ShippingSectionUi.Options(
        rows = listOf(
            SelectableRowUi(
                id = "standard",
                title = UiText.Raw("Standard"),
                subtitle = UiText.plural(R.plurals.checkout_delivery_days, 3, 3),
                trailing = UiText.res(R.string.checkout_free_for_members),
                selected = true,
            ),
            SelectableRowUi(
                id = "express",
                title = UiText.Raw("Express"),
                subtitle = UiText.plural(R.plurals.checkout_delivery_days, 1, 1),
                trailing = UiText.Raw("€12.99"),
                selected = false,
            ),
        ),
    )

    val promo = PromoSectionUi(
        input = "SAVE5",
        inputEnabled = true,
        applyEnabled = true,
        isRedeeming = false,
        applied = null,
        error = null,
    )

    val payment = PaymentSectionUi(
        rows = listOf(
            SelectableRowUi("visa", UiText.res(R.string.checkout_card_label, "Visa", "4242"), subtitle = null, trailing = null, selected = true),
            SelectableRowUi("mastercard", UiText.res(R.string.checkout_card_label, "Mastercard", "0002"), subtitle = null, trailing = null, selected = false),
        ),
    )

    val summary = SummaryUi(
        rows = listOf(
            SummaryRowUi(UiText.res(R.string.checkout_subtotal), UiText.Raw("€44.00")),
            SummaryRowUi(UiText.res(R.string.checkout_shipping), UiText.res(R.string.checkout_free)),
        ),
        total = "€44.00",
    )

    val placeOrder = PlaceOrderButtonUi(label = UiText.res(R.string.checkout_pay_amount, "€44.00"), enabled = true, loading = false)

    val content = CheckoutUiState.Content(
        isOffline = false,
        cart = cart,
        address = address,
        shipping = shipping,
        promo = promo,
        payment = payment,
        summary = summary,
        placeOrder = placeOrder,
        blockingError = null,
    )
}
```

`ui/component/SectionHeader.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = MaterialTheme.typography.titleMedium)
}
```

`ui/component/SelectableRow.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun SelectableRow(row: SelectableRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = row.selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = row.selected, onClick = null)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text = row.title.asString(), style = MaterialTheme.typography.bodyLarge)
            row.subtitle?.let { subtitle ->
                Text(
                    text = subtitle.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        row.trailing?.let { trailing ->
            Text(text = trailing.asString(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectableRowPreview() {
    ShopTheme {
        SelectableRow(row = CheckoutPreviewData.shipping.rows.first(), onClick = {})
    }
}
```

`ui/component/OfflineBanner.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = stringResource(R.string.checkout_offline),
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineBannerPreview() {
    ShopTheme { OfflineBanner() }
}
```

`ui/component/ErrorState.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.UiText
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R

@Composable
fun ErrorState(message: UiText, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message.asString(), textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(text = stringResource(R.string.action_retry)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    ShopTheme { ErrorState(message = UiText.res(R.string.error_network), onRetry = {}) }
}
```

`ui/component/BlockingErrorDialog.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.core.ui.UiText
import com.example.core.ui.asString
import com.example.feature.checkout.presentation.R

@Composable
fun BlockingErrorDialog(message: UiText, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.action_ok)) }
        },
        text = { Text(text = message.asString()) },
    )
}
```

- [ ] **Step 3: Write the section composables**

`ui/component/CartSection.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.CartLineUi
import com.example.feature.checkout.presentation.ui.model.CartSectionUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun CartSection(
    cart: CartSectionUi,
    onQuantityChange: (sku: String, quantity: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = cart.title.asString())
        if (cart.isEmpty) {
            Text(text = stringResource(R.string.checkout_cart_empty), style = MaterialTheme.typography.bodyMedium)
        }
        cart.lines.forEach { line ->
            CartLineRow(line = line, onQuantityChange = { quantity -> onQuantityChange(line.sku, quantity) })
        }
    }
}

@Composable
private fun CartLineRow(line: CartLineUi, onQuantityChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = line.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = { onQuantityChange(line.quantity - 1) }, enabled = line.canDecrement) {
            if (line.decrementRemovesLine) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.checkout_cd_remove, line.name),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_remove),
                    contentDescription = stringResource(R.string.checkout_cd_decrease, line.name),
                )
            }
        }
        Text(text = line.quantity.toString(), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onQuantityChange(line.quantity + 1) }, enabled = line.canIncrement) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.checkout_cd_increase, line.name),
            )
        }
        Text(
            text = line.lineTotal,
            modifier = Modifier.widthIn(min = 72.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CartSectionPreview() {
    ShopTheme { CartSection(cart = CheckoutPreviewData.cart, onQuantityChange = { _, _ -> }) }
}
```

`ui/component/AddressSection.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.AddressSectionUi
import com.example.feature.checkout.presentation.ui.model.AddressStatusUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun AddressSection(
    address: AddressSectionUi,
    onSelect: (addressId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(text = stringResource(R.string.checkout_section_address))
        address.rows.forEach { row -> SelectableRow(row = row, onClick = { onSelect(row.id) }) }
        AddressStatus(status = address.status)
    }
}

@Composable
private fun AddressStatus(status: AddressStatusUi) {
    when (status) {
        AddressStatusUi.None -> Unit
        AddressStatusUi.Checking -> Text(
            text = stringResource(R.string.checkout_address_checking),
            style = MaterialTheme.typography.bodySmall,
        )
        AddressStatusUi.Deliverable -> Text(
            text = stringResource(R.string.checkout_address_deliverable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        is AddressStatusUi.Warning -> Text(
            text = status.message.asString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddressSectionPreview() {
    ShopTheme { AddressSection(address = CheckoutPreviewData.address, onSelect = {}) }
}
```

`ui/component/ShippingSection.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.ShippingSectionUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun ShippingSection(
    shipping: ShippingSectionUi,
    onSelect: (optionId: String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(text = stringResource(R.string.checkout_section_shipping))
        when (shipping) {
            ShippingSectionUi.Hidden -> Text(
                text = stringResource(R.string.checkout_shipping_unavailable),
                style = MaterialTheme.typography.bodyMedium,
            )
            ShippingSectionUi.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            is ShippingSectionUi.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = shipping.message.asString(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onRetry) { Text(text = stringResource(R.string.action_retry)) }
            }
            is ShippingSectionUi.Options -> shipping.rows.forEach { row ->
                SelectableRow(row = row, onClick = { onSelect(row.id) })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShippingSectionPreview() {
    ShopTheme { ShippingSection(shipping = CheckoutPreviewData.shipping, onSelect = {}, onRetry = {}) }
}
```

`ui/component/PromoSection.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.PromoSectionUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun PromoSection(
    promo: PromoSectionUi,
    onInputChange: (String) -> Unit,
    onApply: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = promo.input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                enabled = promo.inputEnabled,
                label = { Text(text = stringResource(R.string.checkout_promo_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onApply() }),
            )
            if (promo.applied != null) {
                TextButton(onClick = onRemove) { Text(text = stringResource(R.string.action_remove)) }
            } else {
                TextButton(onClick = onApply, enabled = promo.applyEnabled) {
                    if (promo.isRedeeming) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(text = stringResource(R.string.action_apply))
                    }
                }
            }
        }
        promo.applied?.let { applied ->
            Text(text = applied.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        promo.error?.let { error ->
            Text(text = error.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PromoSectionPreview() {
    ShopTheme { PromoSection(promo = CheckoutPreviewData.promo, onInputChange = {}, onApply = {}, onRemove = {}) }
}
```

`ui/component/PaymentSection.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.PaymentSectionUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun PaymentSection(
    payment: PaymentSectionUi,
    onSelect: (paymentMethodId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(text = stringResource(R.string.checkout_section_payment))
        payment.rows.forEach { row -> SelectableRow(row = row, onClick = { onSelect(row.id) }) }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentSectionPreview() {
    ShopTheme { PaymentSection(payment = CheckoutPreviewData.payment, onSelect = {}) }
}
```

`ui/component/SummarySection.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.SummaryUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun SummarySection(summary: SummaryUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = stringResource(R.string.checkout_section_summary))
        HorizontalDivider()
        summary.rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = row.label.asString(), style = MaterialTheme.typography.bodyLarge)
                Text(text = row.value.asString(), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SummarySectionPreview() {
    ShopTheme { SummarySection(summary = CheckoutPreviewData.summary) }
}
```

`ui/component/PlaceOrderButton.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.ui.model.PlaceOrderButtonUi
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData

@Composable
fun PlaceOrderButton(button: PlaceOrderButtonUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = button.enabled, modifier = modifier.fillMaxWidth()) {
        if (button.loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
        } else {
            Text(text = button.label.asString())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceOrderButtonPreview() {
    ShopTheme { PlaceOrderButton(button = CheckoutPreviewData.placeOrder, onClick = {}) }
}
```

- [ ] **Step 4: Write the screens and navigation**

`ui/CheckoutScreen.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.ObserveAsEvents
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.CheckoutViewModel
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutIntent
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PaymentAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.ShippingAction
import com.example.feature.checkout.presentation.projector.toUiText
import com.example.feature.checkout.presentation.ui.component.AddressSection
import com.example.feature.checkout.presentation.ui.component.BlockingErrorDialog
import com.example.feature.checkout.presentation.ui.component.CartSection
import com.example.feature.checkout.presentation.ui.component.ErrorState
import com.example.feature.checkout.presentation.ui.component.OfflineBanner
import com.example.feature.checkout.presentation.ui.component.PaymentSection
import com.example.feature.checkout.presentation.ui.component.PlaceOrderButton
import com.example.feature.checkout.presentation.ui.component.PromoSection
import com.example.feature.checkout.presentation.ui.component.ShippingSection
import com.example.feature.checkout.presentation.ui.component.SummarySection
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData
import kotlinx.coroutines.launch

/** Stateful entry point: wires the ViewModel, its one-off events and the snackbar. Everything below is stateless. */
@Composable
fun CheckoutRoute(
    onOrderPlaced: (orderId: String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CheckoutEvent.NavigateToConfirmation -> onOrderPlaced(event.orderId)
            is CheckoutEvent.ShowError -> {
                val message = event.error.toUiText().asString(context)
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        }
    }

    CheckoutScreen(state = state, onIntent = viewModel::onIntent, snackbarHostState = snackbarHostState)
}

@Composable
fun CheckoutScreen(
    state: CheckoutUiState,
    onIntent: (CheckoutIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                CheckoutUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is CheckoutUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { onIntent(LifecycleAction.RetryLoadClicked) },
                )
                is CheckoutUiState.Content -> CheckoutContent(state = state, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun CheckoutContent(state: CheckoutUiState.Content, onIntent: (CheckoutIntent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { Text(text = stringResource(R.string.checkout_title), style = MaterialTheme.typography.headlineMedium) }
        if (state.isOffline) {
            item { OfflineBanner() }
        }
        item {
            CartSection(
                cart = state.cart,
                onQuantityChange = { sku, quantity -> onIntent(CartAction.QuantityChanged(sku, quantity)) },
            )
        }
        item {
            AddressSection(address = state.address, onSelect = { onIntent(AddressAction.Selected(it)) })
        }
        item {
            ShippingSection(
                shipping = state.shipping,
                onSelect = { onIntent(ShippingAction.Selected(it)) },
                onRetry = { onIntent(ShippingAction.RetryClicked) },
            )
        }
        item {
            PromoSection(
                promo = state.promo,
                onInputChange = { onIntent(PromoAction.InputChanged(it)) },
                onApply = { onIntent(PromoAction.ApplyClicked) },
                onRemove = { onIntent(PromoAction.RemoveClicked) },
            )
        }
        item {
            PaymentSection(payment = state.payment, onSelect = { onIntent(PaymentAction.Selected(it)) })
        }
        item { SummarySection(summary = state.summary) }
        item {
            PlaceOrderButton(button = state.placeOrder, onClick = { onIntent(OrderAction.PlaceClicked) })
        }
    }

    state.blockingError?.let { message ->
        BlockingErrorDialog(message = message, onDismiss = { onIntent(OrderAction.ErrorDismissed) })
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckoutScreenPreview() {
    ShopTheme { CheckoutScreen(state = CheckoutPreviewData.content, onIntent = {}) }
}
```

`ui/confirmation/OrderConfirmationScreen.kt`:

```kotlin
package com.example.feature.checkout.presentation.ui.confirmation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R

@Composable
fun OrderConfirmationScreen(
    orderId: String,
    onBackToCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = stringResource(R.string.confirmation_title), style = MaterialTheme.typography.headlineMedium)
            Text(text = stringResource(R.string.confirmation_order_number, orderId), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onBackToCheckout) { Text(text = stringResource(R.string.confirmation_back_to_checkout)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderConfirmationScreenPreview() {
    ShopTheme { OrderConfirmationScreen(orderId = "ORD-1001", onBackToCheckout = {}) }
}
```

`navigation/CheckoutDestination.kt`:

```kotlin
package com.example.feature.checkout.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object CheckoutDestination
```

`navigation/OrderConfirmationDestination.kt`:

```kotlin
package com.example.feature.checkout.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data class OrderConfirmationDestination(val orderId: String)
```

`navigation/CheckoutNavigation.kt`:

```kotlin
package com.example.feature.checkout.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.feature.checkout.presentation.ui.CheckoutRoute
import com.example.feature.checkout.presentation.ui.confirmation.OrderConfirmationScreen

fun NavGraphBuilder.checkoutScreen(onOrderPlaced: (orderId: String) -> Unit) {
    composable<CheckoutDestination> {
        CheckoutRoute(onOrderPlaced = onOrderPlaced)
    }
}

fun NavGraphBuilder.orderConfirmationScreen(onBackToCheckout: () -> Unit) {
    composable<OrderConfirmationDestination> { entry ->
        val destination = entry.toRoute<OrderConfirmationDestination>()
        OrderConfirmationScreen(orderId = destination.orderId, onBackToCheckout = onBackToCheckout)
    }
}
```

- [ ] **Step 5: Verify it compiles and every test still passes**

Run: `./gradlew :feature:checkout:presentation:assembleDebug :feature:checkout:presentation:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. 101 tests pass.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(checkout): add the checkout and confirmation screens with type-safe navigation"
```

---

### Task 14: The app, the README and the end-to-end check

**Files:**
- Modify: `settings.gradle.kts` (add `include(":app")`)
- Create:
  - `app/build.gradle.kts`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/res/values/{strings,themes}.xml`
  - `app/src/main/kotlin/com/example/app/{ShopApp,MainActivity,ShopNavHost}.kt`
  - `README.md`

**Interfaces:**
- Consumes: `checkoutScreen`, `orderConfirmationScreen`, the destinations (Task 13), `ShopTheme` (Task 7), and the data bindings (Task 6), which reach Hilt through the `:feature:checkout:data` dependency.
- Produces: the installable app `com.example.app`.

- [ ] **Step 1: Write the app module**

Append to `settings.gradle.kts`:

```kotlin
include(":app")
```

`app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.shop.android.application)
    alias(libs.plugins.shop.hilt)
}

android {
    namespace = "com.example.app"
    defaultConfig {
        applicationId = "com.example.app"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    // The composition root: the only module that sees both the presentation and the data modules.
    implementation(projects.core.ui)
    implementation(projects.feature.checkout.presentation)
    implementation(projects.feature.checkout.data)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
}
```

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".ShopApp"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Shop">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Checkout</string>
</resources>
```

`app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Shop" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

`app/src/main/kotlin/com/example/app/ShopApp.kt`:

```kotlin
package com.example.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShopApp : Application()
```

`app/src/main/kotlin/com/example/app/MainActivity.kt`:

```kotlin
package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.ui.theme.ShopTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ShopTheme {
                ShopNavHost()
            }
        }
    }
}
```

`app/src/main/kotlin/com/example/app/ShopNavHost.kt`:

```kotlin
package com.example.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.feature.checkout.presentation.navigation.CheckoutDestination
import com.example.feature.checkout.presentation.navigation.OrderConfirmationDestination
import com.example.feature.checkout.presentation.navigation.checkoutScreen
import com.example.feature.checkout.presentation.navigation.orderConfirmationScreen

@Composable
fun ShopNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = CheckoutDestination, modifier = modifier) {
        checkoutScreen(
            onOrderPlaced = { orderId ->
                // A paid checkout is never shown again: confirmation replaces it.
                navController.navigate(OrderConfirmationDestination(orderId)) {
                    popUpTo<CheckoutDestination> { inclusive = true }
                }
            },
        )
        orderConfirmationScreen(
            onBackToCheckout = {
                // A fresh checkout gets a new checkout id, so the next order is a new order.
                navController.navigate(CheckoutDestination) {
                    popUpTo<OrderConfirmationDestination> { inclusive = true }
                }
            },
        )
    }
}
```

- [ ] **Step 2: Build everything and run every test**

Run: `./gradlew test assembleDebug`
Expected: BUILD SUCCESSFUL. 185 tests pass:

| Module | Tests |
|---|---|
| `:core:mvi` | 7 |
| `:core:domain` | 10 |
| `:feature:checkout:domain` | 28 |
| `:feature:checkout:data` | 37 |
| `:core:ui` | 2 |
| `:feature:checkout:presentation` | 101 |

`app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 3: Write the README**

`README.md`:

````markdown
# Checkout MVI

A proof of concept of an MVI architecture that keeps scaling. A checkout screen has eight sections that depend on each other: the cart, the address, shipping, the promo code, payment, the summary, placing the order and connectivity. Here each section's logic lives in small, pure, tested pieces, and the ViewModel stays the same size however much the screen grows.

## Run it

You need JDK 17 or newer and the Android SDK with platform 37.

```bash
./gradlew test
```

```bash
./gradlew installDebug
```

The app talks to an in-memory demo backend with about 600 ms of latency, so it needs no server. Things to try:

| Try | What happens |
|---|---|
| Promo `SAVE5` | €5 off |
| Promo `BIG50` | €50 off orders of €100 or more. Below that it is refused; raise the mug quantity to qualify. |
| Promo `EXPIRED`, or any other code | Expired / not valid |
| Address "PO Box" or "Island cabin" | Cannot be delivered to, and Pay stays disabled |
| Card Mastercard •••• 0002 | Declined: a dialog explains it |
| Card Amex •••• 3155 | The bank asks for confirmation |
| Airplane mode | Offline banner, and Pay is disabled until the connection is back |
| Remove every item | Empty cart, no shipping options, Pay disabled |

The demo customer is a member, so standard shipping is free and express is not.

## How the MVI loop works

```
 UI ── CheckoutIntent ──► CheckoutViewModel.onIntent ──► Store.dispatch(action)
                                                            │
                                     CheckoutReducer.reduce(state, action)
                                                            │
                                  Transition(state, effects, events)
                                   │              │               │
                          state committed   CheckoutEffectHandler  events ──► ObserveAsEvents
                                   │          (one use case)                (navigation, snackbar)
                          CheckoutProjector         │
                                   │          result CheckoutAction ──► Store.dispatch
                          CheckoutUiState ──► UI
```

| Piece | Role |
|---|---|
| `CheckoutState` | The single source of truth, shaped like the domain |
| `CheckoutIntent` | What the UI may send; a subset of `CheckoutAction` |
| `CheckoutAction` | Everything the reducer handles: intents and effect results, grouped per section |
| `CheckoutReducer` | Routes each action group to one pure slice reducer |
| `CheckoutEffect` | A side effect as plain data; keyed effects are latest-wins |
| `CheckoutEffectHandler` | Runs an effect through one use case and reports the result as an action |
| `CheckoutEvent` | A one-off signal (navigation, transient error), collected with `ObserveAsEvents` |
| `CheckoutProjector` | Turns state into render-ready `CheckoutUiState`, one section projector per section |
| `Store` (`:core:mvi`) | The generic loop every screen reuses: ordered reducing, effects, events |

### Following one tap: "Apply" on a promo code

1. `PromoAction.ApplyClicked` (`contract/PromoAction.kt`) reaches `PromoReducer`.
2. `PromoReducer` marks the promo as being checked and asks for `CheckoutEffect.RedeemPromo`.
3. `CheckoutEffectHandler` calls `RedeemPromoUseCase` and reports `PromoAction.Redeemed` or `PromoAction.Rejected`.
4. `PromoReducer` applies the answer, and `PromoSectionProjector` shows it.

### Adding a section

1. Add a sealed `XAction`; the actions the UI sends also implement `CheckoutIntent`. The compiler then refuses to build until `CheckoutReducer` routes it.
2. Add `XReducer` and route it: one line.
3. If the section needs I/O, add a `CheckoutEffect` and its branch in `CheckoutEffectHandler`, calling one use case.
4. Add `XSectionProjector`, its UI model and its composable.

## Modules

```
:app ──────────────► :feature:checkout:presentation ──► :feature:checkout:domain ──► :core:domain
  │                          │         │
  │                          │         └──► :core:mvi
  │                          └──► :core:ui ──► :core:domain
  └──► :feature:checkout:data ──► :feature:checkout:domain
```

- `:core:mvi`, `:core:domain` and every `domain` module are pure Kotlin.
- Presentation never depends on data. Only `:app` sees both, so only `:app` assembles the Hilt graph.
- A new feature gets the same `domain` / `data` / `presentation` split.
- `build-logic` holds the convention plugins (`shop.jvm.library`, `shop.android.library`, `shop.android.feature`, …), so each module's build file is a few lines.

## Tests

`./gradlew test` runs 185 tests, all with hand-written fakes:

- **Reducers and projectors** are plain functions, so their tests need no coroutines and no Android.
- **Use cases** are tested over fakes of the repository interfaces, which live in the domain module's `testFixtures`.
- **Repositories** are tested over the demo backend, covering every scripted error code.
- **`CheckoutViewModelTest`** runs the whole loop: the real store, reducers, effect handler, use cases and projector over fakes.
````

- [ ] **Step 4: Run the app on the emulator and walk the demo**

Start the emulator and wait for it to boot:

```bash
~/Library/Android/sdk/emulator/emulator -avd Medium_Phone -no-snapshot-save -no-boot-anim
```

```bash
~/Library/Android/sdk/platform-tools/adb wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done'
```

Install and launch:

```bash
./gradlew :app:installDebug
```

```bash
~/Library/Android/sdk/platform-tools/adb shell am start -n com.example.app/.MainActivity
```

Walk the table in the README. Use `adb shell uiautomator dump` to find element bounds, `adb shell input tap X Y` and `adb shell input text SAVE5` to interact, and `adb exec-out screencap -p > shot.png` to look. Expected:

1. The checkout loads with Home, Visa and free Standard shipping selected, and Pay shows €44.00.
2. `SAVE5` applies and the summary shows −€5.00.
3. The PO Box shows "We can't deliver to PO boxes." and Pay is disabled.
4. The Mastercard shows the declined dialog.
5. The Visa lands on "Order placed", and "Back to checkout" opens a fresh checkout.
6. `adb shell cmd connectivity airplane-mode enable` shows the offline banner; `disable` removes it.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(app): wire the checkout POC into a runnable app and document it"
```
