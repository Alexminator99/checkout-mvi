plugins {
    alias(libs.plugins.shop.android.feature)
}

android {
    namespace = "com.example.feature.checkout.presentation"
}

dependencies {
    implementation(projects.feature.checkout.domain)
    implementation(libs.androidx.activity.compose)

    testImplementation(testFixtures(projects.feature.checkout.domain))
}
