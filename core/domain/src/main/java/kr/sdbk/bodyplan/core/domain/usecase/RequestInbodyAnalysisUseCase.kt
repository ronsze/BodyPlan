package kr.sdbk.bodyplan.core.domain.usecase

import javax.inject.Inject
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunRequest
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisRunner
import kr.sdbk.bodyplan.core.domain.repository.InbodyRecordRepository

/** 기록 하나의 분석을 요청한다. AI 연결이 없으면 [AiCredentialMissingException]을 던진다. */
class RequestInbodyAnalysisUseCase
@Inject
constructor(
    private val aiCredentialRepository: AiCredentialRepository,
    private val inbodyRecordRepository: InbodyRecordRepository,
    private val analysisRunner: AnalysisRunner,
) {
    suspend operator fun invoke(recordId: Long) {
        if (aiCredentialRepository.getCredential() == null) throw AiCredentialMissingException()
        // 지난 시도의 실패 표시가 남아 있으면 새 시도가 도는 동안에도 실패로 보인다.
        inbodyRecordRepository.setFailureReason(recordId, null)
        analysisRunner.start(
            AnalysisRunRequest(kind = AnalysisKind.INBODY, scopeKey = recordId.toString(), periodLabel = ""),
        )
    }
}
