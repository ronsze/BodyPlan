plugins {
    alias(libs.plugins.bodyplan.android.library)
}

android {
    namespace = "kr.sdbk.bodyplan.core.local"
}

// 로컬 저장소 모듈. 스키마·DAO를 넣는 시점에 Room 의존성과 `bodyplan.android.hilt`를 추가한다.
