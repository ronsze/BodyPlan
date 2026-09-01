import kr.sdbk.bodyplan.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("bodyplan.android.library")
        pluginManager.apply("bodyplan.android.compose")
        pluginManager.apply("bodyplan.android.hilt")

        dependencies {
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:domain"))
            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}
