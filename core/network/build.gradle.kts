plugins {
    alias(libs.plugins.bodyplan.android.library)
    alias(libs.plugins.bodyplan.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "kr.sdbk.bodyplan.core.network"
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
