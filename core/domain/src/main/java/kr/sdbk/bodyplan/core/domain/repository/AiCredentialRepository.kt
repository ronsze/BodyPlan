package kr.sdbk.bodyplan.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.AiCredential

/** 고른 제공자와 인증 정보를 기기에 보관한다. */
interface AiCredentialRepository {
    fun observeCredential(): Flow<AiCredential?>

    suspend fun getCredential(): AiCredential?

    suspend fun save(credential: AiCredential)

    /** 연결 해제. 제공자와 키를 함께 지운다. */
    suspend fun clear()
}
