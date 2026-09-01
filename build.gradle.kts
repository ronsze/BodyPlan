import com.diffplug.gradle.spotless.SpotlessExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
// 모듈별 공통 설정은 build-logic의 `bodyplan.*` 컨벤션 플러그인에 있다.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless) apply false
}

// 포매팅은 ktlint가 단일 소스다. spotless는 `.editorconfig`를 읽지 않으므로 같은 값을 여기서 넘긴다.
val ktlintSettings = mapOf(
    "ktlint_code_style" to "android_studio",
    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    "ktlint_standard_filename" to "disabled",
    "max_line_length" to "120",
    "ij_kotlin_allow_trailing_comma" to "true",
    "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
)

subprojects {
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint(libs.versions.ktlint.get())
                .editorConfigOverride(ktlintSettings)
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(libs.versions.ktlint.get())
                .editorConfigOverride(ktlintSettings)
        }
    }
}
