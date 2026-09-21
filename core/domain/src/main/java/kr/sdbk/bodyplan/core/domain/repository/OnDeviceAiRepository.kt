package kr.sdbk.bodyplan.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.OnDeviceDownload
import kr.sdbk.bodyplan.core.domain.model.OnDeviceModelStatus

/**
 * 기기 안 모델의 준비 상태와 내려받기. 추론 자체는 [AiAnalysisRepository]가 다른 제공자와 같은 길로 한다.
 *
 * 실패는 삼키지 않고 호출부로 던진다.
 */
interface OnDeviceAiRepository {
    suspend fun getStatus(): OnDeviceModelStatus

    /** 수집을 취소하면 더 이상 관찰하지 않는다. 내려받기 자체가 멈추는지는 플랫폼이 정한다. */
    fun download(): Flow<OnDeviceDownload>
}
