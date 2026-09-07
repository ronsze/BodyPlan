package kr.sdbk.bodyplan.core.domain.model

/**
 * 인바디 분석 한 번에 필요한 것 전부.
 *
 * [imagePath]는 앱 내부 저장소로 이미 복사된 사진이다. 고른 URI를 그대로 두면 갤러리에서
 * 지웠을 때 이력의 사진이 깨지고, 접근 권한도 앱을 다시 켜면 사라진다.
 */
data class InbodyAnalysisRequest(val credential: AiCredential, val profile: UserProfile, val imagePath: String)
