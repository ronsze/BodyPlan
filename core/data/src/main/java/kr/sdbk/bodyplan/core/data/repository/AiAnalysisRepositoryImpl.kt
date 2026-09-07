package kr.sdbk.bodyplan.core.data.repository

import java.io.IOException
import javax.inject.Inject
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AiRequestFailedException
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.network.AiClient
import kr.sdbk.bodyplan.core.network.AiHttpException
import kr.sdbk.bodyplan.core.network.ClaudeApi
import kr.sdbk.bodyplan.core.network.GeminiApi
import kr.sdbk.bodyplan.core.network.GptApi

internal class AiAnalysisRepositoryImpl
@Inject
constructor(
    @ClaudeApi private val claude: AiClient,
    @GptApi private val gpt: AiClient,
    @GeminiApi private val gemini: AiClient,
) : AiAnalysisRepository {
    override suspend fun verifyCredential(credential: AiCredential) {
        runCatching { clientFor(credential.provider).verify(credential.token) }
            .onFailure { throw it.toDomainFailure() }
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
