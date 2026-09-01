import com.android.build.api.dsl.ApplicationExtension
import kr.sdbk.bodyplan.buildlogic.configureAndroid
import kr.sdbk.bodyplan.buildlogic.intVersion
import kr.sdbk.bodyplan.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

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

        // feature를 추가할 때마다 :app의 의존성을 고치지 않도록, 모든 impl 모듈을 경로로 붙인다.
        // 프로젝트 트리는 settings 평가 시점에 완성되므로 경로만 읽는 것은 configuration 시점에 안전하다.
        val featureImplPaths =
            rootProject.subprojects
                .map { it.path }
                .filter { it.startsWith(":feature:") && it.endsWith(":impl") }

        dependencies {
            featureImplPaths.forEach { add("implementation", project(it)) }
        }
    }
}
