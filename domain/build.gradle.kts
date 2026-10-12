// Pure Kotlin. The innermost ring: depends on nothing project-internal.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(17) }

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)   // JSR-330 annotation only; not Hilt, not Android

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
