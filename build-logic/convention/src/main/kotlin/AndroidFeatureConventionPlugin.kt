import com.example.buildlogic.libs
import com.example.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/** A feature's presentation module: Compose, Hilt, type-safe navigation and the shared MVI and UI cores. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("shop.android.library.compose")
        pluginManager.apply("shop.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        dependencies {
            "implementation"(project(":core:mvi"))
            "implementation"(project(":core:ui"))
            "implementation"(libs.library("androidx-lifecycle-runtime-compose"))
            "implementation"(libs.library("androidx-lifecycle-viewmodel"))
            "implementation"(libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            "implementation"(libs.library("androidx-navigation-compose"))
        }
    }
}
