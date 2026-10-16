package kr.sdbk.bodyplan.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.InbodyEntry
import kr.sdbk.bodyplan.core.domain.repository.AnalysisRunner
import kr.sdbk.bodyplan.core.domain.repository.InbodyRecordRepository

/** 기록·결과·진행 중인 작업을 합쳐 측정 순서 내림차순으로 낸다. */
class ObserveInbodyEntriesUseCase
@Inject
constructor(
    private val inbodyRecordRepository: InbodyRecordRepository,
    private val analysisRunner: AnalysisRunner,
) {
    operator fun invoke(): Flow<List<InbodyEntry>> = combine(
        inbodyRecordRepository.observeRecords(),
        analysisRunner.observeAll(AnalysisKind.INBODY),
    ) { records, runs ->
        val runByRecord = runs.associateBy { it.scopeKey }
        InbodyTimeline.descending(
            records.map { InbodyEntry(it.record, it.result, runByRecord[it.record.id.toString()]) },
        )
    }
}
