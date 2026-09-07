package kr.sdbk.bodyplan.core.domain.repository

import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.DietSummaryRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest

/** AI를 불러 기록을 분석한다. 실패는 삼키지 않고 호출부로 던진다. */
interface AiAnalysisRepository {
    /**
     * 인증 정보가 실제로 쓸 수 있는지 확인한다.
     *
     * 거절되면 `AiUnauthorizedException`, 그 밖의 실패는 `AiRequestFailedException`을 던진다.
     */
    suspend fun verifyCredential(credential: AiCredential)

    /** 하루치 식단을 분석한다. 사진과 메모로 먹은 음식과 칼로리, 영양 성분을 정리한다. */
    suspend fun analyzeDiet(request: DietAnalysisRequest): AnalysisContent

    /** 날짜별 분석을 모아 기간의 흐름을 종합한다. 사진을 다시 보내지 않는다. */
    suspend fun summarizeDiet(request: DietSummaryRequest): AnalysisContent

    /** 운동 기록을 분석한다. 날짜별과 기간이 같은 재료를 쓰고 요청의 종류로만 갈린다. */
    suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent

    /** 인바디 사진을 읽어 체성분을 정리하고 식단·운동 개선 방향을 낸다. */
    suspend fun analyzeInbody(request: InbodyAnalysisRequest): AnalysisContent
}
