package kr.sdbk.bodyplan.feature.my.impl.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository

internal class FakeAiCredentialRepository(initial: AiCredential? = null) : AiCredentialRepository {
    private val credential = MutableStateFlow(initial)

    var saveFailure: Throwable? = null

    var saveCallCount: Int = 0
        private set

    override fun observeCredential(): Flow<AiCredential?> = credential

    override suspend fun getCredential(): AiCredential? = credential.value

    override suspend fun save(credential: AiCredential) {
        saveCallCount++
        saveFailure?.let { throw it }
        this.credential.value = credential
    }

    override suspend fun clear() {
        saveFailure?.let { throw it }
        credential.value = null
    }
}
