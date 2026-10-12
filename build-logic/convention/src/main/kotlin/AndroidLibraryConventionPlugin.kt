import com.android.build.api.dsl.LibraryExtension
import com.example.buildlogic.JAVA_VERSION
import com.example.buildlogic.addUnitTestDependencies
import com.example.buildlogic.configureKotlinCompilation
import com.example.buildlogic.libs
import com.example.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> {
            compileSdk = libs.versionInt("compileSdk")
            defaultConfig.minSdk = libs.versionInt("minSdk")
            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            testOptions.unitTests.isReturnDefaultValues = true
        }
        configureKotlinCompilation()
        addUnitTestDependencies()
    }
}
