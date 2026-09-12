plugins {
    alias(libs.plugins.bodyplan.android.feature.impl)
}

android {
    namespace = "kr.sdbk.bodyplan.feature.my.impl"
}

dependencies {
    // 루틴 관리 화면으로 보낸다. 다른 feature의 api만 의존한다.
    implementation(project(":feature:workoutlog:api"))
}
