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
    // 이 모듈의 컴포넌트가 도메인 모델을 파라미터로 받는다.
    api(project(":core:domain"))
}
