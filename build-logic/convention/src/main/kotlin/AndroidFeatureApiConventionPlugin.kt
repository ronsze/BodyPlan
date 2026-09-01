import kr.sdbk.bodyplan.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/** feature의 외부 공개 계약 모듈. NavKey와 navigate 확장 함수만 담으므로 Compose·Hilt를 붙이지 않는다. */
class AndroidFeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("bodyplan.android.library")
        pluginManager.apply(libs.findPlugin("kotlin-serialization").get().get().pluginId)

        dependencies {
            add("api", project(":core:navigation"))
        }
    }
}
