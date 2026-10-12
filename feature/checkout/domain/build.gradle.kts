plugins {
    alias(libs.plugins.shop.jvm.library)
    `java-test-fixtures`
}

dependencies {
    api(projects.core.domain)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
