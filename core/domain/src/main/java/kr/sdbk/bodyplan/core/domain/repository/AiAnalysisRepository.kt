package kr.sdbk.bodyplan.core.domain.repository

import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisSummaryRequest
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysis
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

    /** 끼니 하나를 분석한다. 사진과 메모로 먹은 음식과 칼로리, 영양 성분을 정리한다. */
    suspend fun analyzeDiet(request: DietAnalysisRequest): AnalysisContent

    /** 운동 기록 하나를 분석한다. */
    suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent

    /**
     * 아래 계층의 분석을 모아 위 계층을 만든다.
     *
     * 끼니→하루→주→월이 같은 얼개라 한 함수가 다 맡는다. 식단과 운동도 같다.
     */
    suspend fun summarize(request: AnalysisSummaryRequest): AnalysisContent

    /**
     * 인바디 사진을 읽어 체성분을 정리하고 식단·운동 개선 방향을 낸다.
     *
     * 글과 함께 읽어 낸 숫자도 받는다 — 그래프와 프로필 갱신이 숫자를 쓴다.
     */
    suspend fun analyzeInbody(request: InbodyAnalysisRequest): InbodyAnalysis
}
