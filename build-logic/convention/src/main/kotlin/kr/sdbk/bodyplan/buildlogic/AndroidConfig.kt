package kr.sdbk.bodyplan.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** 카탈로그 `[versions]`의 값을 읽는다. SDK·버전 숫자의 단일 소스는 카탈로그다. */
fun Project.version(alias: String): String = libs.findVersion(alias).get().requiredVersion

fun Project.intVersion(alias: String): Int = version(alias).toInt()

/** 모든 Android 모듈이 공유하는 SDK·Java 설정. */
fun Project.configureAndroid(extension: CommonExtension) {
    extension.apply {
        compileSdk {
            version = release(intVersion("compileSdk"))
        }
        defaultConfig.minSdk = intVersion("minSdk")
        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
}

/** Compose 컴파일러 플러그인과 빌드 피처를 켠다. */
fun Project.configureCompose(extension: CommonExtension) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extension.buildFeatures.compose = true
}
