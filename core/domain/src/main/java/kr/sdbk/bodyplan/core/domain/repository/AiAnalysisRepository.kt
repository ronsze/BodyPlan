package kr.sdbk.bodyplan.core.domain.repository

import kr.sdbk.bodyplan.core.domain.model.AiCredential

/**
 * AI를 불러 기록을 분석한다.
 *
 * 지금은 인증 확인만 한다. 분석 함수는 그 기능을 만드는 단위에서 붙인다.
 */
interface AiAnalysisRepository {
    /**
     * 인증 정보가 실제로 쓸 수 있는지 확인한다.
     *
     * 거절되면 `AiUnauthorizedException`, 그 밖의 실패는 `AiRequestFailedException`을 던진다.
     */
    suspend fun verifyCredential(credential: AiCredential)
}
