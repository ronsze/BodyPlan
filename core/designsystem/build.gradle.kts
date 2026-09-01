plugins {
    alias(libs.plugins.bodyplan.android.library)
    alias(libs.plugins.bodyplan.android.compose)
}

android {
    namespace = "kr.sdbk.bodyplan.core.designsystem"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
