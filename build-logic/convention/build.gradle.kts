import org.gradle.plugin.devel.PluginDeclaration

plugins {
    `kotlin-dsl`
}

group = "kr.sdbk.bodyplan.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
}

/** 카탈로그의 플러그인 id로 컨벤션 플러그인을 등록한다. id는 카탈로그가 단일 소스다. */
fun NamedDomainObjectContainer<PluginDeclaration>.registerConvention(
    plugin: Provider<PluginDependency>,
    implementationClass: String,
) {
    val pluginId = plugin.get().pluginId
    register(pluginId) {
        this.id = pluginId
        this.implementationClass = implementationClass
    }
}

gradlePlugin {
    plugins {
        registerConvention(libs.plugins.bodyplan.android.application, "AndroidApplicationConventionPlugin")
        registerConvention(libs.plugins.bodyplan.android.library, "AndroidLibraryConventionPlugin")
        registerConvention(libs.plugins.bodyplan.android.compose, "AndroidComposeConventionPlugin")
        registerConvention(libs.plugins.bodyplan.android.hilt, "AndroidHiltConventionPlugin")
        registerConvention(libs.plugins.bodyplan.android.feature, "AndroidFeatureConventionPlugin")
        registerConvention(libs.plugins.bodyplan.jvm.library, "JvmLibraryConventionPlugin")
    }
}
