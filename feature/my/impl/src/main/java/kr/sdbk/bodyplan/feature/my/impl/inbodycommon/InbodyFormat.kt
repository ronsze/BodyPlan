package kr.sdbk.bodyplan.feature.my.impl.inbodycommon

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kr.sdbk.bodyplan.core.domain.model.InbodyEntryStatus
import kr.sdbk.bodyplan.core.domain.model.InbodyMetricKind

private val FULL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("M.dd")
private val RANGE_END: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd")

/** 십분의 일 단위 정수를 소수 첫째 자리 문구로. 684 → "68.4". */
internal fun Int.tenthsText(): String = String.format(Locale.US, "%.1f", this / TENTHS)

internal fun LocalDate.fullText(): String = FULL_DATE.format(this)

internal fun LocalDate.shortText(): String = SHORT_DATE.format(this)

/** 조회 범위 문구. 같은 해 안이라는 보장이 없어 시작에는 해를 적고 끝에는 월·일만 적는다. */
internal fun rangeText(start: LocalDate, end: LocalDate): String =
    "${FULL_DATE.format(start)} – ${RANGE_END.format(end)}"

internal val InbodyMetricKind.title: String
    get() = when (this) {
        InbodyMetricKind.WEIGHT -> "체중"
        InbodyMetricKind.SKELETAL_MUSCLE -> "골격근량"
        InbodyMetricKind.BODY_FAT_PERCENT -> "체지방률"
    }

internal val InbodyMetricKind.unit: String
    get() = when (this) {
        InbodyMetricKind.WEIGHT, InbodyMetricKind.SKELETAL_MUSCLE -> "kg"
        InbodyMetricKind.BODY_FAT_PERCENT -> "%"
    }

/** 비율의 차이는 %가 아니라 %p다. */
internal val InbodyMetricKind.changeUnit: String
    get() = if (this == InbodyMetricKind.BODY_FAT_PERCENT) "%p" else unit

/** 변화량 문구. 0이면 방향 없이 변화 없음이다. */
internal fun changeText(kind: InbodyMetricKind, changeTenths: Int): String = when {
    changeTenths > 0 -> "↑ ${changeTenths.tenthsText()} ${kind.changeUnit}"
    changeTenths < 0 -> "↓ ${abs(changeTenths).tenthsText()} ${kind.changeUnit}"
    else -> "변화 없음"
}

internal val InbodyEntryStatus.label: String
    get() = when (this) {
        InbodyEntryStatus.NOT_ANALYZED -> "분석 전"
        InbodyEntryStatus.RUNNING -> "분석 중"
        InbodyEntryStatus.FAILED -> "분석 실패"
        InbodyEntryStatus.ANALYZED -> "분석 결과 있음"
    }

private const val TENTHS = 10.0
