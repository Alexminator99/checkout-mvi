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
