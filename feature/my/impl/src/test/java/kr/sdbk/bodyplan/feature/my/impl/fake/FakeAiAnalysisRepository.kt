package kr.sdbk.bodyplan.feature.my.impl.fake

import kotlinx.coroutines.CompletableDeferred
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisSummaryRequest
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysis
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository

internal class FakeAiAnalysisRepository(
    private val content: AnalysisContent = AnalysisContent(summary = "요약", sections = emptyList()),
    private val measurement: InbodyMeasurement = InbodyMeasurement(),
) : AiAnalysisRepository {
    var verifyFailure: Throwable? = null

    /** 완료 전 검증 중 상태를 관찰하려는 테스트가 채운다. `null`이면 즉시 끝난다. */
    var verifyGate: CompletableDeferred<Unit>? = null

    val verified: MutableList<AiCredential> = mutableListOf()

    var analyzeInbodyCallCount: Int = 0
        private set
    var lastInbodyRequest: InbodyAnalysisRequest? = null
        private set
    var analyzeInbodyFailure: Throwable? = null

    override suspend fun verifyCredential(credential: AiCredential) {
        verifyGate?.await()
        verifyFailure?.let { throw it }
        verified += credential
    }

    override suspend fun analyzeDiet(request: DietAnalysisRequest): AnalysisContent = error("사용하지 않음")

    override suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent = error("사용하지 않음")

    override suspend fun summarize(request: AnalysisSummaryRequest): AnalysisContent = error("사용하지 않음")

    override suspend fun analyzeInbody(request: InbodyAnalysisRequest): InbodyAnalysis {
        analyzeInbodyCallCount++
        lastInbodyRequest = request
        analyzeInbodyFailure?.let { throw it }
        return InbodyAnalysis(content = content, measurement = measurement)
    }
}
