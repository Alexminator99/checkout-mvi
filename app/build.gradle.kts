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
