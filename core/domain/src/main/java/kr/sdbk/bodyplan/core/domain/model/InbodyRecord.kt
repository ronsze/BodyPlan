package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate

/**
 * 인바디 사진 한 장과 사용자가 고른 측정일.
 *
 * 분석 결과는 기록에 붙지 않고 따로 든다 — 사진만 저장한 기록이 있어야 한다.
 * [failureReason]은 마지막 시도가 실패했을 때의 사유이고, 새 시도를 시작하면 지워진다.
 */
data class InbodyRecord(
    val id: Long,
    val imagePath: String,
    val measuredOn: LocalDate,
    val createdAtMillis: Long,
    val failureReason: String? = null,
)

/** 저장소가 내는 기록과 그 결과. 결과는 기록당 최대 하나다. */
data class InbodyRecordWithResult(val record: InbodyRecord, val result: AnalysisResult?)

/** 기록의 분석 상태. 저장하지 않고 진행 중인 작업과 기록·결과에서 정한다. */
enum class InbodyEntryStatus { NOT_ANALYZED, RUNNING, FAILED, ANALYZED }

/**
 * 화면이 그리는 기록 하나. 기록·결과·진행 중인 작업을 합친 것이다.
 *
 * 실패 사유가 있어도 이전 결과가 있으면 [result]는 그대로 남는다.
 */
data class InbodyEntry(val record: InbodyRecord, val result: AnalysisResult?, val run: AnalysisRun?) {
    val status: InbodyEntryStatus
        get() = when {
            run?.state == AnalysisRunState.RUNNING -> InbodyEntryStatus.RUNNING
            record.failureReason != null -> InbodyEntryStatus.FAILED
            result != null -> InbodyEntryStatus.ANALYZED
            else -> InbodyEntryStatus.NOT_ANALYZED
        }

    /** 결과에서 읽힌 값. 하나도 읽히지 않았거나 결과가 없으면 `null`이다. */
    val measurement: InbodyMeasurement? get() = result?.measurement
}
