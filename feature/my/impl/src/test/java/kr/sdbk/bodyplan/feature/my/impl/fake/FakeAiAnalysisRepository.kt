package kr.sdbk.bodyplan.feature.my.impl.fake

import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository

internal class FakeAiAnalysisRepository : AiAnalysisRepository {
    var verifyFailure: Throwable? = null

    val verified: MutableList<AiCredential> = mutableListOf()

    override suspend fun verifyCredential(credential: AiCredential) {
        verifyFailure?.let { throw it }
        verified += credential
    }
}
