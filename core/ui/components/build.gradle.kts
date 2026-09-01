plugins {
    alias(libs.plugins.bodyplan.android.library)
    alias(libs.plugins.bodyplan.android.compose)
}

android {
    namespace = "kr.sdbk.bodyplan.core.ui.components"
}

dependencies {
    api(project(":core:ui:coordinator"))
    api(project(":core:designsystem"))
}
