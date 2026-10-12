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
include(":core:domain")
include(":feature:checkout:domain")
include(":feature:checkout:data")
