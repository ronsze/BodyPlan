package kr.sdbk.bodyplan.core.data.repository

import java.io.IOException
import javax.inject.Inject
import kr.sdbk.bodyplan.core.data.ai.AiErrorReason
import kr.sdbk.bodyplan.core.data.ai.AnalysisContentParser
import kr.sdbk.bodyplan.core.data.ai.AnalysisImageLoader
import kr.sdbk.bodyplan.core.data.ai.AnalysisPrompt
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AiRequestFailedException
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisSummaryRequest
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysis
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.network.AiClient
import kr.sdbk.bodyplan.core.network.AiHttpException
import kr.sdbk.bodyplan.core.network.AiImage
import kr.sdbk.bodyplan.core.network.ClaudeApi
import kr.sdbk.bodyplan.core.network.GeminiApi
import kr.sdbk.bodyplan.core.network.GptApi
import kr.sdbk.bodyplan.core.ondevice.OnDeviceAiException
import kr.sdbk.bodyplan.core.ondevice.OnDeviceApi

internal class AiAnalysisRepositoryImpl
@Inject
constructor(
    @ClaudeApi private val claude: AiClient,
    @GptApi private val gpt: AiClient,
    @GeminiApi private val gemini: AiClient,
    @OnDeviceApi private val onDevice: AiClient,
    private val imageLoader: AnalysisImageLoader,
    private val contentParser: AnalysisContentParser,
    private val errorReason: AiErrorReason,
) : AiAnalysisRepository {
    override suspend fun verifyCredential(credential: AiCredential) {
        runCatching { clientFor(credential.provider).verify(credential.token) }
            .onFailure { throw it.toDomainFailure(credential.token) }
    }

    override suspend fun analyzeDiet(request: DietAnalysisRequest): AnalysisContent = complete(
        credential = request.credential,
        userPrompt = AnalysisPrompt.diet(request),
        images = imageLoader.load(request.entries.map { it.imagePath }),
    )

    // 종합은 이미 분석된 글만 보낸다. 사진이나 기록 원문을 다시 실으면 한 달치가 요청 하나에 실린다.
    override suspend fun summarize(request: AnalysisSummaryRequest): AnalysisContent = complete(
        credential = request.credential,
        userPrompt = AnalysisPrompt.summary(request),
        images = emptyList(),
    )

    // 운동 기록에는 사진이 없다. 날짜별과 기간이 같은 경로를 탄다.
    override suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent = complete(
        credential = request.credential,
        userPrompt = AnalysisPrompt.workout(request),
        images = emptyList(),
    )

    override suspend fun analyzeInbody(request: InbodyAnalysisRequest): InbodyAnalysis {
        val raw = call(
            credential = request.credential,
            userPrompt = AnalysisPrompt.inbody(request),
            images = imageLoader.load(listOf(request.imagePath)),
        )
        return InbodyAnalysis(
            content = contentParser.parse(raw),
            measurement = contentParser.parseMeasurement(raw),
        )
    }

    private suspend fun complete(
        credential: AiCredential,
        userPrompt: String,
        images: List<AiImage>,
    ): AnalysisContent = contentParser.parse(call(credential, userPrompt, images))

    private suspend fun call(credential: AiCredential, userPrompt: String, images: List<AiImage>): String {
        val raw = runCatching {
            clientFor(credential.provider).complete(
                token = credential.token,
                systemPrompt = AnalysisPrompt.SYSTEM,
                userPrompt = userPrompt,
                images = images,
            )
        }.getOrElse { throw it.toDomainFailure(credential.token) }
        // 빈 답을 결과로 저장하면 멀쩡한 이전 결과가 화면에서 밀려난다. 부르지 못한 것으로 본다.
        if (raw.isBlank()) throw AiRequestFailedException(null)
        return raw
    }

    private fun clientFor(provider: AiProvider): AiClient = when (provider) {
        AiProvider.CLAUDE -> claude
        AiProvider.GPT -> gpt
        AiProvider.GEMINI -> gemini
        AiProvider.ON_DEVICE -> onDevice
    }

    /**
     * 화면이 키가 틀린 것과 부르지 못한 것을 다른 문구로 알리도록 여기서 가른다.
     *
     * 400을 인증 실패로 보지 않는다. 400은 요청이 잘못됐을 때 오는 코드라, 함께 묶으면
     * 멀쩡한 키를 쓰는 사용자가 키를 의심하며 계속 다시 넣게 된다.
     */
    private fun Throwable.toDomainFailure(token: String): Throwable = when {
        this is AiHttpException && code in UNAUTHORIZED_CODES -> AiUnauthorizedException()

        // 제미나이는 잘못된 키에 400을 준다. 코드만으로는 요청 오류와 구분되지 않아 본문을 본다.
        this is AiHttpException && code == BAD_REQUEST && errorReason.isInvalidKey(body) ->
            AiUnauthorizedException()

        this is AiHttpException -> AiRequestFailedException(this, httpReason(token))

        // 온디바이스는 인증이 없다. 사유는 이미 사용자에게 보일 문구로 정해져 있다.
        this is OnDeviceAiException -> AiRequestFailedException(this, reason)

        this is IOException -> AiRequestFailedException(this, networkReason(token))

        else -> this
    }

    /** 사유를 읽어 내지 못해도 코드는 남긴다. 아무것도 없으면 어디를 볼지 정할 수 없다. */
    private fun AiHttpException.httpReason(token: String): String =
        errorReason.of(body)?.let { errorReason.withoutSecrets(it, token) } ?: "HTTP $code"

    /** 끊긴 이유가 시간 초과인지 이름 풀이 실패인지에 따라 볼 곳이 다르다. */
    private fun IOException.networkReason(token: String): String {
        val detail = message?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()
        return errorReason.withoutSecrets("${javaClass.simpleName}$detail", token).take(REASON_MAX_LENGTH)
    }
}

private const val REASON_MAX_LENGTH = 300

private val UNAUTHORIZED_CODES = setOf(401, 403)
private const val BAD_REQUEST = 400
