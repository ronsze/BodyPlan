package kr.sdbk.bodyplan.feature.workoutlog.impl.fake

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
        sections = listOf(AnalysisSection(title = "부위별 볼륨", body = "가슴")),
    ),
) : AiAnalysisRepository {
    var failure: Throwable? = null
    var analyzeWorkoutCallCount: Int = 0
        private set
    var lastRequest: WorkoutAnalysisRequest? = null
        private set

    override suspend fun verifyCredential(credential: AiCredential) = error("사용하지 않음")

    override suspend fun analyzeDiet(request: DietAnalysisRequest): AnalysisContent = error("사용하지 않음")

    override suspend fun summarizeDiet(request: DietSummaryRequest): AnalysisContent = error("사용하지 않음")

    override suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent {
        analyzeWorkoutCallCount++
        lastRequest = request
        failure?.let { throw it }
        return content
    }

    override suspend fun analyzeInbody(request: InbodyAnalysisRequest): AnalysisContent = error("사용하지 않음")
}
