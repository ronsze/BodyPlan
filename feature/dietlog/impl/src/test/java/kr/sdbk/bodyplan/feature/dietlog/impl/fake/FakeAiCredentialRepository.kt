package kr.sdbk.bodyplan.feature.dietlog.impl.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository

internal class FakeAiCredentialRepository(initial: AiCredential? = null) : AiCredentialRepository {
    private val credential = MutableStateFlow(initial)

    override fun observeCredential(): Flow<AiCredential?> = credential

    override suspend fun getCredential(): AiCredential? = credential.value

    override suspend fun save(credential: AiCredential) {
        this.credential.value = credential
    }

    override suspend fun clear() {
        credential.value = null
    }
}
