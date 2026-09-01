import kr.sdbk.bodyplan.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/** feature의 내부 구현 모듈. 화면·ViewModel을 담고, 형제 api 모듈을 재노출해 :app이 impl 하나만 의존해도 NavKey를 쓸 수 있게 한다. */
class AndroidFeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("bodyplan.android.library")
        pluginManager.apply("bodyplan.android.compose")
        pluginManager.apply("bodyplan.android.hilt")

        // :feature:<이름>:impl → :feature:<이름>:api. 모듈마다 적지 않도록 경로에서 유도한다.
        val apiPath = "${parent!!.path}:api"

        dependencies {
            add("api", project(apiPath))
            add("implementation", project(":core:ui:components"))
            add("implementation", project(":core:domain"))
            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}
