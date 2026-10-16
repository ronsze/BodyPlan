package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.domain.model.InbodyEntry
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.InbodyRecord
import kr.sdbk.bodyplan.core.domain.model.InbodyRecordWithResult

internal fun inbodyRecord(id: Long, measuredOn: LocalDate, failureReason: String? = null): InbodyRecord = InbodyRecord(
    id = id,
    imagePath = "/path/$id.jpg",
    measuredOn = measuredOn,
    createdAtMillis = id,
    failureReason = failureReason,
)

internal fun inbodyResult(recordId: Long, measurement: InbodyMeasurement?): AnalysisResult = AnalysisResult(
    id = recordId,
    kind = AnalysisKind.INBODY,
    scopeKey = recordId.toString(),
    content = AnalysisContent(summary = "요약", sections = emptyList()),
    createdAtMillis = recordId,
    imagePath = null,
    measurement = measurement,
)

/** [measurement]가 있으면 결과가 있는 기록이고, [withResult]로 measurement가 null인 결과도 만들 수 있다. */
internal fun inbodyEntry(
    id: Long,
    measuredOn: LocalDate,
    measurement: InbodyMeasurement? = null,
    withResult: Boolean = measurement != null,
    failureReason: String? = null,
    run: AnalysisRun? = null,
): InbodyEntry = InbodyEntry(
    record = inbodyRecord(id, measuredOn, failureReason),
    result = if (withResult) inbodyResult(id, measurement) else null,
    run = run,
)

internal fun inbodyRecordWithResult(
    id: Long,
    measuredOn: LocalDate,
    measurement: InbodyMeasurement?,
): InbodyRecordWithResult =
    InbodyRecordWithResult(inbodyRecord(id, measuredOn), measurement?.let { inbodyResult(id, it) })
