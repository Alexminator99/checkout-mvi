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
