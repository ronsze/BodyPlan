package kr.sdbk.bodyplan.core.data.repository

import java.io.IOException
import javax.inject.Inject
import kr.sdbk.bodyplan.core.data.ai.AnalysisContentParser
import kr.sdbk.bodyplan.core.data.ai.AnalysisImageLoader
import kr.sdbk.bodyplan.core.data.ai.AnalysisPrompt
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AiRequestFailedException
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.DietSummaryRequest
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.network.AiClient
import kr.sdbk.bodyplan.core.network.AiHttpException
import kr.sdbk.bodyplan.core.network.AiImage
import kr.sdbk.bodyplan.core.network.ClaudeApi
import kr.sdbk.bodyplan.core.network.GeminiApi
import kr.sdbk.bodyplan.core.network.GptApi

internal class AiAnalysisRepositoryImpl
@Inject
constructor(
    @ClaudeApi private val claude: AiClient,
    @GptApi private val gpt: AiClient,
    @GeminiApi private val gemini: AiClient,
    private val imageLoader: AnalysisImageLoader,
    private val contentParser: AnalysisContentParser,
) : AiAnalysisRepository {
    override suspend fun verifyCredential(credential: AiCredential) {
        runCatching { clientFor(credential.provider).verify(credential.token) }
            .onFailure { throw it.toDomainFailure() }
    }

    override suspend fun analyzeDiet(request: DietAnalysisRequest): AnalysisContent = complete(
        credential = request.credential,
        userPrompt = AnalysisPrompt.diet(request),
        images = imageLoader.load(request.entries.map { it.imagePath }),
    )

    // 종합은 이미 분석된 글만 보낸다. 기간의 사진을 다시 실으면 요청이 커져 느리고 비싸다.
    override suspend fun summarizeDiet(request: DietSummaryRequest): AnalysisContent = complete(
        credential = request.credential,
        userPrompt = AnalysisPrompt.dietSummary(request),
        images = emptyList(),
    )

    // 운동 기록에는 사진이 없다. 날짜별과 기간이 같은 경로를 탄다.
    override suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent = complete(
        credential = request.credential,
        userPrompt = AnalysisPrompt.workout(request),
        images = emptyList(),
    )

    private suspend fun complete(
        credential: AiCredential,
        userPrompt: String,
        images: List<AiImage>,
    ): AnalysisContent {
        val raw = runCatching {
            clientFor(credential.provider).complete(
                token = credential.token,
                systemPrompt = AnalysisPrompt.SYSTEM,
                userPrompt = userPrompt,
                images = images,
            )
        }.getOrElse { throw it.toDomainFailure() }
        // 빈 답을 결과로 저장하면 멀쩡한 이전 결과가 화면에서 밀려난다. 부르지 못한 것으로 본다.
        if (raw.isBlank()) throw AiRequestFailedException(null)
        return contentParser.parse(raw)
    }

    private fun clientFor(provider: AiProvider): AiClient = when (provider) {
        AiProvider.CLAUDE -> claude
        AiProvider.GPT -> gpt
        AiProvider.GEMINI -> gemini
    }

    /** 화면이 키가 틀린 것과 부르지 못한 것을 다른 문구로 알리도록 여기서 가른다. */
    private fun Throwable.toDomainFailure(): Throwable = when {
        this is AiHttpException && code in UNAUTHORIZED_CODES -> AiUnauthorizedException()
        this is AiHttpException -> AiRequestFailedException(this)
        this is IOException -> AiRequestFailedException(this)
        else -> this
    }
}

// 제미나이는 키가 틀려도 400을 주는 경우가 있어 함께 본다.
private val UNAUTHORIZED_CODES = setOf(400, 401, 403)
