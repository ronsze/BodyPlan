plugins {
    alias(libs.plugins.bodyplan.android.library)
    alias(libs.plugins.bodyplan.android.hilt)
}

android {
    namespace = "kr.sdbk.bodyplan.core.data"
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:local"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
