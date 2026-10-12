plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 26
        targetSdk = 35
    }
    buildFeatures { compose = true }
}

kotlin { jvmToolchain(17) }

dependencies {
    // The composition root: the only module that sees both features and :data.
    implementation(project(":feature:checkout"))
    implementation(project(":data"))
    implementation(project(":core:ui"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
