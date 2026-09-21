plugins {
    alias(libs.plugins.bodyplan.android.library)
    alias(libs.plugins.bodyplan.android.hilt)
    // 분석 결과의 묶음 목록을 한 컬럼에 JSON으로 담는다.
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "kr.sdbk.bodyplan.core.data"
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:local"))
    implementation(project(":core:network"))
    implementation(project(":core:ondevice"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    // 세션 알림과 foreground service 시작에 쓴다.
    implementation(libs.androidx.core.ktx)
    // 분석을 화면 밖에서 돌린다. 워커가 Hilt 주입을 받으려면 androidx.hilt가 함께 필요하다.
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.kotlinx.coroutines.test)
}
