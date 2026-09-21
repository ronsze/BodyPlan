package kr.sdbk.bodyplan.core.domain.model

/**
 * 고른 제공자와 그 인증 정보.
 *
 * 둘은 한 벌로만 뜻이 있다. 따로 두면 키만 남거나 제공자만 남아 어느 쪽도 쓸 수 없다.
 * 온디바이스는 키가 없어 [token]이 빈 문자열이다 — nullable로 두면 호출부마다 분기가 생긴다.
 */
data class AiCredential(val provider: AiProvider, val token: String) {
    companion object {
        fun onDevice(): AiCredential = AiCredential(provider = AiProvider.ON_DEVICE, token = "")
    }
}
