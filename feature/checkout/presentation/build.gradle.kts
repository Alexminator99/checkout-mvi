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
