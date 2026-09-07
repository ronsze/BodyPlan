package kr.sdbk.bodyplan.feature.dietlog.impl.fake

import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.DietSummaryRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository

internal class FakeAiAnalysisRepository(
    private val content: AnalysisContent = AnalysisContent(
        summary = "요약",
        sections = listOf(AnalysisSection(title = "먹은 음식", body = "밥")),
    ),
) : AiAnalysisRepository {
    var failure: Throwable? = null
    var analyzeDietCallCount: Int = 0
        private set
    var summarizeDietCallCount: Int = 0
        private set

    override suspend fun verifyCredential(credential: AiCredential) = error("사용하지 않음")

    override suspend fun analyzeDiet(request: DietAnalysisRequest): AnalysisContent {
        analyzeDietCallCount++
        failure?.let { throw it }
        return content
    }

    override suspend fun summarizeDiet(request: DietSummaryRequest): AnalysisContent {
        summarizeDietCallCount++
        failure?.let { throw it }
        return content
    }

    override suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent = error("사용하지 않음")

    override suspend fun analyzeInbody(request: InbodyAnalysisRequest): AnalysisContent = error("사용하지 않음")
}
