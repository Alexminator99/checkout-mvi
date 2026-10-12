import com.android.build.api.dsl.ApplicationExtension
import com.example.buildlogic.JAVA_VERSION
import com.example.buildlogic.addComposeDependencies
import com.example.buildlogic.configureKotlinCompilation
import com.example.buildlogic.libs
import com.example.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        extensions.configure<ApplicationExtension> {
            compileSdk = libs.versionInt("compileSdk")
            defaultConfig {
                minSdk = libs.versionInt("minSdk")
                targetSdk = libs.versionInt("targetSdk")
            }
            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            buildFeatures.compose = true
        }
        configureKotlinCompilation()
        addComposeDependencies()
    }
}
