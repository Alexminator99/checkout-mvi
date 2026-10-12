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
