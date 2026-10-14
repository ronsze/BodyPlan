package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kr.sdbk.bodyplan.core.domain.model.InbodyEntry
import kr.sdbk.bodyplan.core.domain.model.InbodyEntryStatus
import kr.sdbk.bodyplan.core.domain.model.InbodyMetricCard
import kr.sdbk.bodyplan.core.domain.model.InbodyMetricKind
import kr.sdbk.bodyplan.core.domain.model.InbodyPeriod
import kr.sdbk.bodyplan.core.domain.model.InbodyTrend
import kr.sdbk.bodyplan.core.domain.model.InbodyTrendPoint

/** 선택한 기간의 지표별 카드와 그래프 값을 만든다. */
class BuildInbodyTrendUseCase @Inject constructor() {
    operator fun invoke(entries: List<InbodyEntry>, period: InbodyPeriod, today: LocalDate): InbodyTrend {
        val ordered = InbodyTimeline.ascending(entries)
        val start = startOf(period, ordered, today)
        val inRange = ordered.filter { !it.record.measuredOn.isBefore(start) && !it.record.measuredOn.isAfter(today) }

        val counted = inRange.filter { entry ->
            DISPLAYED_KINDS.any {
                InbodyMetrics.tenthsOf(entry.measurement, it) !=
                    null
            }
        }
        return InbodyTrend(
            rangeStart = start,
            rangeEnd = today,
            hasRecords = entries.isNotEmpty(),
            measuredCount = counted.size,
            latestMeasuredOn = counted.lastOrNull()?.record?.measuredOn,
            pendingCount = entries.count { it.result == null && it.status in PENDING_STATUSES },
            cards = DISPLAYED_KINDS.mapNotNull { kind -> cardOf(kind, inRange, start, today) },
        )
    }

    private fun startOf(period: InbodyPeriod, ordered: List<InbodyEntry>, today: LocalDate): LocalDate = when (period) {
        InbodyPeriod.ONE_MONTH -> today.minusMonths(1)

        InbodyPeriod.THREE_MONTHS -> today.minusMonths(3)

        InbodyPeriod.SIX_MONTHS -> today.minusMonths(6)

        InbodyPeriod.ALL ->
            ordered.firstOrNull(InbodyTimeline::isMeasured)?.record?.measuredOn ?: today
    }

    private fun cardOf(
        kind: InbodyMetricKind,
        inRange: List<InbodyEntry>,
        start: LocalDate,
        end: LocalDate,
    ): InbodyMetricCard? {
        val valued = inRange.mapNotNull { entry ->
            InbodyMetrics.tenthsOf(entry.measurement, kind)?.let { entry.record.measuredOn to it }
        }
        if (valued.isEmpty()) return null

        val minTenths = valued.minOf { it.second }
        val maxTenths = valued.maxOf { it.second }
        // 값 범위의 1.2배를 축으로 하되 1.0보다 짧지 않게 한다. 정수로 계산해 경계가 어긋나지 않는다.
        val lengthTimesTen = maxOf((maxTenths - minTenths) * AXIS_MARGIN_TIMES_TEN, MIN_AXIS_TIMES_TEN)
        val centerTimesTwo = minTenths + maxTenths
        val axisMin = Math.floorDiv(10 * centerTimesTwo - lengthTimesTen, 20)
        val axisMax = -Math.floorDiv(-(10 * centerTimesTwo + lengthTimesTen), 20)
        val axisLength = (axisMax - axisMin).toFloat()

        val totalDays = ChronoUnit.DAYS.between(start, end)
        val points = valued.map { (date, tenths) ->
            InbodyTrendPoint(
                measuredOn = date,
                valueTenths = tenths,
                xFraction = if (totalDays == 0L) {
                    CENTER
                } else {
                    ChronoUnit.DAYS.between(start, date).toFloat() / totalDays
                },
                yFraction = (tenths - axisMin) / axisLength,
            )
        }
        return InbodyMetricCard(
            kind = kind,
            latestTenths = valued.last().second,
            changeTenths = if (valued.size < 2) null else valued.last().second - valued.first().second,
            points = points,
            axisMinTenths = axisMin,
            axisMaxTenths = axisMax,
            tickTenths = listOf(axisMin, Math.floorDiv(axisMin + axisMax + 1, 2), axisMax),
            dateLabels = listOf(start, start.plusDays(totalDays / 2), end),
        )
    }

    private companion object {
        val DISPLAYED_KINDS = listOf(
            InbodyMetricKind.WEIGHT,
            InbodyMetricKind.SKELETAL_MUSCLE,
            InbodyMetricKind.BODY_FAT_PERCENT,
        )
        val PENDING_STATUSES = setOf(
            InbodyEntryStatus.NOT_ANALYZED,
            InbodyEntryStatus.RUNNING,
            InbodyEntryStatus.FAILED,
        )
        const val AXIS_MARGIN_TIMES_TEN = 12
        const val MIN_AXIS_TIMES_TEN = 100
        const val CENTER = 0.5f
    }
}
