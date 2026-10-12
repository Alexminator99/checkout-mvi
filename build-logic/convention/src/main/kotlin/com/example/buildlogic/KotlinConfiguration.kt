package com.example.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal val JAVA_VERSION = JavaVersion.VERSION_17

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
    }
    configureKotlinCompilation()
}

internal fun Project.configureKotlinCompilation() {
    tasks.withType<KotlinCompile>().configureEach {
        val isTestCompilation = name.contains("Test")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            if (isTestCompilation) {
                optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
    }
}
