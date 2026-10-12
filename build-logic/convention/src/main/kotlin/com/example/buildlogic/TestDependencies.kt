package com.example.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.addUnitTestDependencies() {
    dependencies {
        "testImplementation"(libs.library("junit"))
        "testImplementation"(libs.library("kotlinx-coroutines-test"))
        "testImplementation"(libs.library("turbine"))
    }
}
