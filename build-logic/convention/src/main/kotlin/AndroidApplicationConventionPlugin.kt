import com.android.build.api.dsl.ApplicationExtension
import kr.sdbk.bodyplan.buildlogic.configureAndroid
import kr.sdbk.bodyplan.buildlogic.intVersion
import kr.sdbk.bodyplan.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            configureAndroid(this)
            defaultConfig {
                targetSdk = intVersion("targetSdk")
                versionCode = intVersion("versionCode")
                versionName = version("versionName")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
        }
    }
}
