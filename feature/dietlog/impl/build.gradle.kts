plugins {
    alias(libs.plugins.bodyplan.android.feature.impl)
}

android {
    namespace = "kr.sdbk.bodyplan.feature.dietlog.impl"
}

dependencies {
    // 토큰이 없을 때 토큰 등록 화면으로 보낸다. 다른 feature의 api만 의존한다.
    implementation(project(":feature:my:api"))
}
