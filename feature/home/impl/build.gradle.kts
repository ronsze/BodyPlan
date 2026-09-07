plugins {
    alias(libs.plugins.bodyplan.android.feature.impl)
}

android {
    namespace = "kr.sdbk.bodyplan.feature.home.impl"
}

dependencies {
    implementation(projects.feature.workoutlog.api)
}
