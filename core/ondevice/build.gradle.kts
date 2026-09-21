plugins {
    alias(libs.plugins.bodyplan.android.library)
    alias(libs.plugins.bodyplan.android.hilt)
}

android {
    namespace = "kr.sdbk.bodyplan.core.ondevice"
}

dependencies {
    // 온디바이스도 제공자 하나다. 부르는 창구(AiClient)는 core:network의 것을 그대로 쓴다.
    implementation(project(":core:network"))
    implementation(libs.mlkit.genai.prompt)
    implementation(libs.kotlinx.coroutines.core)
}
