package kr.sdbk.bodyplan.feature.my.impl.fake

import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.DietSummaryRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository

internal class FakeAiAnalysisRepository(
    private val content: AnalysisContent = AnalysisContent(summary = "요약", sections = emptyList()),
) : AiAnalysisRepository {
    var verifyFailure: Throwable? = null

    val verified: MutableList<AiCredential> = mutableListOf()

    var analyzeInbodyCallCount: Int = 0
        private set
    var lastInbodyRequest: InbodyAnalysisRequest? = null
        private set
    var analyzeInbodyFailure: Throwable? = null

    override suspend fun verifyCredential(credential: AiCredential) {
        verifyFailure?.let { throw it }
        verified += credential
    }

    override suspend fun analyzeDiet(request: DietAnalysisRequest): AnalysisContent = error("사용하지 않음")

    override suspend fun summarizeDiet(request: DietSummaryRequest): AnalysisContent = error("사용하지 않음")

    override suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent = error("사용하지 않음")

    override suspend fun analyzeInbody(request: InbodyAnalysisRequest): AnalysisContent {
        analyzeInbodyCallCount++
        lastInbodyRequest = request
        analyzeInbodyFailure?.let { throw it }
        return content
    }
}
