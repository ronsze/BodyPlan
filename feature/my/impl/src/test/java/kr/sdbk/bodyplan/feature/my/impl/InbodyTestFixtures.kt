package kr.sdbk.bodyplan.feature.my.impl

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunState
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.InbodyRecord
import kr.sdbk.bodyplan.core.domain.model.InbodyRecordWithResult

internal val TEST_TODAY: LocalDate = LocalDate.of(2026, 10, 14)

internal val testClock: Clock =
    Clock.fixed(TEST_TODAY.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"))

internal fun inbodyResult(
    id: Long,
    measurement: InbodyMeasurement? = InbodyMeasurement(weightKg = 70.0, skeletalMuscleKg = 32.0, bodyFatKg = 14.0),
): AnalysisResult = AnalysisResult(
    id = id,
    kind = AnalysisKind.INBODY,
    scopeKey = id.toString(),
    content = AnalysisContent(summary = "요약", sections = emptyList()),
    createdAtMillis = 0L,
    measurement = measurement,
)

internal fun inbodyItem(
    id: Long,
    measuredOn: LocalDate = TEST_TODAY,
    measurement: InbodyMeasurement? = null,
    failureReason: String? = null,
): InbodyRecordWithResult = InbodyRecordWithResult(
    record = InbodyRecord(
        id = id,
        imagePath = "file-$id",
        measuredOn = measuredOn,
        createdAtMillis = 0L,
        failureReason = failureReason,
    ),
    result = measurement?.let { inbodyResult(id, it) },
)

internal fun runningRun(id: Long): AnalysisRun =
    AnalysisRun(kind = AnalysisKind.INBODY, scopeKey = id.toString(), state = AnalysisRunState.RUNNING)
