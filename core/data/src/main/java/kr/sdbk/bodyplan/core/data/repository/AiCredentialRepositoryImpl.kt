package kr.sdbk.bodyplan.core.data.repository

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.requiresToken
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.local.datastore.AppPreferences

internal class AiCredentialRepositoryImpl
@Inject
constructor(private val preferences: AppPreferences) :
    AiCredentialRepository {
    override fun observeCredential(): Flow<AiCredential?> =
        combine(preferences.aiProvider, preferences.aiToken, ::toCredential)

    override suspend fun getCredential(): AiCredential? =
        toCredential(preferences.getAiProvider(), preferences.getAiToken())

    override suspend fun save(credential: AiCredential) {
        preferences.saveAiCredential(credential.provider.name, credential.token)
    }

    override suspend fun clear() {
        preferences.clearAiCredential()
    }

    /**
     * 한쪽만 남아 있으면 쓸 수 없으므로 없는 것으로 본다. 저장된 제공자 이름이 낯설 때도 같다.
     * 온디바이스는 키가 없는 것이 정상이라 빈 토큰을 그대로 받는다.
     */
    private fun toCredential(provider: String?, token: String?): AiCredential? {
        if (provider == null || token == null) return null
        val parsed = AiProvider.entries.firstOrNull { it.name == provider } ?: return null
        if (parsed.requiresToken && token.isBlank()) return null
        return AiCredential(provider = parsed, token = token)
    }
}
