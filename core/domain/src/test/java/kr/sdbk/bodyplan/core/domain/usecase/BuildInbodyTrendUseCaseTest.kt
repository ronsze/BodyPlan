package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunState
import kr.sdbk.bodyplan.core.domain.model.InbodyEntry
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.InbodyMetricCard
import kr.sdbk.bodyplan.core.domain.model.InbodyMetricKind
import kr.sdbk.bodyplan.core.domain.model.InbodyPeriod
import kr.sdbk.bodyplan.core.domain.model.InbodyTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildInbodyTrendUseCaseTest {
    private val build = BuildInbodyTrendUseCase()
    private val today = LocalDate.of(2026, 9, 30)

    private fun weightEntry(id: Long, on: LocalDate, weightKg: Double): InbodyEntry =
        inbodyEntry(id, on, InbodyMeasurement(weightKg = weightKg))

    private fun fatEntry(id: Long, on: LocalDate, weightKg: Double, bodyFatKg: Double): InbodyEntry =
        inbodyEntry(id, on, InbodyMeasurement(weightKg = weightKg, bodyFatKg = bodyFatKg))

    private fun InbodyTrend.card(kind: InbodyMetricKind): InbodyMetricCard = cards.first { it.kind == kind }

    @Test
    fun `1개월 전 시작일은 달에 같은 일자가 없으면 그 달 마지막 날이다`() {
        val common = emptyList<InbodyEntry>()

        assertEquals(
            LocalDate.of(2026, 2, 28),
            build(common, InbodyPeriod.ONE_MONTH, LocalDate.of(2026, 3, 31)).rangeStart,
        )
        assertEquals(
            LocalDate.of(2028, 2, 29),
            build(common, InbodyPeriod.ONE_MONTH, LocalDate.of(2028, 3, 31)).rangeStart,
        )
        assertEquals(
            LocalDate.of(2026, 2, 28),
            build(common, InbodyPeriod.SIX_MONTHS, LocalDate.of(2026, 8, 31)).rangeStart,
        )
    }

    @Test
    fun `전체의 시작일은 측정 기록 중 가장 이른 측정일이고 측정 기록이 없으면 오늘이다`() {
        val entries = listOf(
            inbodyEntry(1, LocalDate.of(2026, 4, 1)),
            weightEntry(2, LocalDate.of(2026, 5, 10), 70.0),
            weightEntry(3, LocalDate.of(2026, 6, 1), 69.0),
        )

        assertEquals(LocalDate.of(2026, 5, 10), build(entries, InbodyPeriod.ALL, today).rangeStart)
        assertEquals(today, build(listOf(inbodyEntry(1, LocalDate.of(2026, 4, 1))), InbodyPeriod.ALL, today).rangeStart)
    }

    @Test
    fun `기간 시작일과 끝일에 측정한 기록은 그래프에 포함된다`() {
        val start = LocalDate.of(2026, 8, 30)
        val entries = listOf(weightEntry(1, start, 70.0), weightEntry(2, today, 69.0))

        val trend = build(entries, InbodyPeriod.ONE_MONTH, today)

        assertEquals(listOf(start, today), trend.card(InbodyMetricKind.WEIGHT).points.map { it.measuredOn })
    }

    @Test
    fun `기간을 3개월로 바꾸면 범위와 포함 기록이 바뀐다`() {
        val entries = listOf(weightEntry(1, today.minusMonths(2), 70.0))

        val oneMonth = build(entries, InbodyPeriod.ONE_MONTH, today)
        val threeMonths = build(entries, InbodyPeriod.THREE_MONTHS, today)

        assertTrue(oneMonth.cards.isEmpty())
        assertEquals(today.minusMonths(3), threeMonths.rangeStart)
        assertEquals(1, threeMonths.card(InbodyMetricKind.WEIGHT).points.size)
    }

    @Test
    fun `표시값은 십진 반올림이라 20점45는 20점5이고 20점449는 20점4다`() {
        val entries = listOf(
            weightEntry(1, today.minusDays(2), 20.45),
            weightEntry(2, today.minusDays(1), 20.449),
        )

        val points = build(entries, InbodyPeriod.ONE_MONTH, today).card(InbodyMetricKind.WEIGHT).points

        assertEquals(listOf(205, 204), points.map { it.valueTenths })
    }

    @Test
    fun `체중이 0이거나 없거나 체지방량이 없으면 체지방률 값이 없다`() {
        val entries = listOf(
            fatEntry(1, today.minusDays(3), weightKg = 0.0, bodyFatKg = 10.0),
            inbodyEntry(2, today.minusDays(2), InbodyMeasurement(bodyFatKg = 10.0)),
            weightEntry(3, today.minusDays(1), 70.0),
        )

        val trend = build(entries, InbodyPeriod.ONE_MONTH, today)

        assertTrue(trend.cards.none { it.kind == InbodyMetricKind.BODY_FAT_PERCENT })
    }

    @Test
    fun `체지방률은 체지방량을 체중으로 나눈 백분율의 소수 첫째 자리 반올림이다`() {
        val entries = listOf(fatEntry(1, today, weightKg = 72.4, bodyFatKg = 15.4))

        val card = build(entries, InbodyPeriod.ONE_MONTH, today).card(InbodyMetricKind.BODY_FAT_PERCENT)

        assertEquals(213, card.points.single().valueTenths)
        assertEquals(213, card.latestTenths)
    }

    @Test
    fun `체지방률 20점43과 20점38은 같은 20점4이고 같은 높이이며 변화는 0이다`() {
        val entries = listOf(
            fatEntry(1, today.minusDays(2), weightKg = 100.0, bodyFatKg = 20.43),
            fatEntry(2, today.minusDays(1), weightKg = 100.0, bodyFatKg = 20.38),
        )

        val card = build(entries, InbodyPeriod.ONE_MONTH, today).card(InbodyMetricKind.BODY_FAT_PERCENT)

        assertEquals(listOf(204, 204), card.points.map { it.valueTenths })
        assertEquals(card.points[0].yFraction, card.points[1].yFraction, 0f)
        assertEquals(0, card.changeTenths)
    }

    @Test
    fun `최근 값은 가장 나중 기록의 표시값이고 변화량은 최근 값에서 첫 값을 뺀 값이다`() {
        val entries = listOf(
            weightEntry(1, today.minusDays(3), 70.0),
            weightEntry(2, today.minusDays(2), 69.0),
            weightEntry(3, today.minusDays(1), 68.4),
        )

        val card = build(entries, InbodyPeriod.ONE_MONTH, today).card(InbodyMetricKind.WEIGHT)

        assertEquals(684, card.latestTenths)
        assertEquals(-16, card.changeTenths)
    }

    @Test
    fun `표시값이 같으면 변화량은 0이다`() {
        val entries = listOf(
            fatEntry(1, today.minusDays(2), weightKg = 100.0, bodyFatKg = 20.4),
            fatEntry(2, today.minusDays(1), weightKg = 100.0, bodyFatKg = 20.4),
        )

        val card = build(entries, InbodyPeriod.ONE_MONTH, today).card(InbodyMetricKind.BODY_FAT_PERCENT)

        assertEquals(0, card.changeTenths)
    }

    @Test
    fun `측정이 한 번뿐이면 점 하나와 최근 값만 있고 변화량은 없다`() {
        val entries = listOf(weightEntry(1, today, 70.0))

        val card = build(entries, InbodyPeriod.ONE_MONTH, today).card(InbodyMetricKind.WEIGHT)

        assertEquals(1, card.points.size)
        assertEquals(700, card.latestTenths)
        assertNull(card.changeTenths)
    }

    @Test
    fun `한 지표의 값이 하나도 없으면 그 카드만 빠진다`() {
        val entries = listOf(weightEntry(1, today, 70.0))

        val trend = build(entries, InbodyPeriod.ONE_MONTH, today)

        assertEquals(listOf(InbodyMetricKind.WEIGHT), trend.cards.map { it.kind })
    }

    @Test
    fun `기록은 있으나 기간 안에 측정 기록이 없으면 카드가 없다`() {
        val entries = listOf(weightEntry(1, today.minusMonths(2), 70.0))

        val trend = build(entries, InbodyPeriod.ONE_MONTH, today)

        assertTrue(trend.hasRecords)
        assertTrue(trend.cards.isEmpty())
    }

    @Test
    fun `기록이 하나도 없으면 빈 상태다`() {
        val trend = build(emptyList(), InbodyPeriod.ONE_MONTH, today)

        assertFalse(trend.hasRecords)
        assertTrue(trend.cards.isEmpty())
    }

    @Test
    fun `시작일과 끝일이 같으면 점의 가로 위치는 0점5이고 날짜 셋은 같은 날이다`() {
        val entries = listOf(weightEntry(1, today, 70.0))

        val card = build(entries, InbodyPeriod.ALL, today).card(InbodyMetricKind.WEIGHT)

        assertEquals(0.5f, card.points.single().xFraction, 0f)
        assertEquals(listOf(today, today, today), card.dateLabels)
    }

    @Test
    fun `같은 측정일의 두 점은 같은 가로 위치이고 최근 값은 나중 기록이다`() {
        val day = today.minusDays(5)
        val entries = listOf(weightEntry(1, day, 70.0), weightEntry(2, day, 69.0))

        val card = build(entries, InbodyPeriod.ONE_MONTH, today).card(InbodyMetricKind.WEIGHT)

        assertEquals(card.points[0].xFraction, card.points[1].xFraction, 0f)
        assertEquals(690, card.latestTenths)
    }

    @Test
    fun `가운데 날짜는 일수 차이의 절반을 내린 날이다`() {
        val start = LocalDate.of(2026, 9, 1)
        val end = LocalDate.of(2026, 9, 30)
        val entries = listOf(weightEntry(1, start, 70.0))

        val card = build(entries, InbodyPeriod.ALL, end).card(InbodyMetricKind.WEIGHT)

        assertEquals(listOf(start, LocalDate.of(2026, 9, 15), end), card.dateLabels)
    }

    @Test
    fun `표시값 범위가 0이면 축 길이는 1점0이고 점은 가운데다`() {
        val entries = listOf(
            weightEntry(1, today.minusDays(2), 70.0),
            weightEntry(2, today.minusDays(1), 70.0),
        )

        val card = build(entries, InbodyPeriod.ONE_MONTH, today).card(InbodyMetricKind.WEIGHT)

        assertEquals(10, card.axisMaxTenths - card.axisMinTenths)
        assertEquals(listOf(0.5f, 0.5f), card.points.map { it.yFraction })
        assertEquals(listOf(695, 700, 705), card.tickTenths)
    }

    @Test
    fun `표시값 범위가 0점2이면 축 길이는 1점0이다`() {
        val entries = listOf(
            weightEntry(1, today.minusDays(2), 70.0),
            weightEntry(2, today.minusDays(1), 70.2),
        )

        val card = build(entries, InbodyPeriod.ONE_MONTH, today).card(InbodyMetricKind.WEIGHT)

        assertEquals(10, card.axisMaxTenths - card.axisMinTenths)
    }

    @Test
    fun `표시값 범위가 10점0이면 축 길이는 12점0이다`() {
        val entries = listOf(
            weightEntry(1, today.minusDays(2), 60.0),
            weightEntry(2, today.minusDays(1), 70.0),
        )

        val card = build(entries, InbodyPeriod.ONE_MONTH, today).card(InbodyMetricKind.WEIGHT)

        assertEquals(120, card.axisMaxTenths - card.axisMinTenths)
    }

    @Test
    fun `축의 최솟값은 내리고 최댓값은 올려 소수 첫째 자리로 맞춘다`() {
        val entries = listOf(
            weightEntry(1, today.minusDays(2), 68.4),
            weightEntry(2, today.minusDays(1), 70.0),
        )

        val card = build(entries, InbodyPeriod.ONE_MONTH, today).card(InbodyMetricKind.WEIGHT)

        assertEquals(682, card.axisMinTenths)
        assertEquals(702, card.axisMaxTenths)
    }

    @Test
    fun `세로 눈금은 최솟값 중간 최댓값 셋이고 점의 세로 위치는 축에서의 비율이다`() {
        val entries = listOf(
            weightEntry(1, today.minusDays(2), 68.4),
            weightEntry(2, today.minusDays(1), 70.0),
        )

        val card = build(entries, InbodyPeriod.ONE_MONTH, today).card(InbodyMetricKind.WEIGHT)

        assertEquals(listOf(682, 692, 702), card.tickTenths)
        assertEquals(0.1f, card.points[0].yFraction, 1e-5f)
        assertEquals(0.9f, card.points[1].yFraction, 1e-5f)
    }

    @Test
    fun `키만 읽힌 결과는 측정 횟수와 그래프에 들어가지 않는다`() {
        val entries = listOf(inbodyEntry(1, today, InbodyMeasurement(heightCm = 175.0)))

        val trend = build(entries, InbodyPeriod.ONE_MONTH, today)

        assertEquals(0, trend.measuredCount)
        assertTrue(trend.cards.isEmpty())
    }

    @Test
    fun `분석 대기 안내의 수는 결과가 없는 분석 전 분석 중 분석 실패 기록의 수다`() {
        val running = AnalysisRun(AnalysisKind.INBODY, "2", AnalysisRunState.RUNNING)
        val entries = listOf(
            inbodyEntry(1, today.minusDays(2)),
            inbodyEntry(2, today.minusDays(1), InbodyMeasurement(weightKg = 70.0), run = running),
        )

        val trend = build(entries, InbodyPeriod.ONE_MONTH, today)

        assertEquals(1, trend.pendingCount)
    }

    @Test
    fun `분석 전 기록은 그래프에 쓰이지 않는다`() {
        val trend = build(listOf(inbodyEntry(1, today)), InbodyPeriod.ONE_MONTH, today)

        assertTrue(trend.cards.isEmpty())
    }

    @Test
    fun `측정 요약은 기간 안 가장 최근 측정일과 측정 횟수다`() {
        val entries = listOf(
            weightEntry(1, LocalDate.of(2026, 9, 1), 70.0),
            weightEntry(2, LocalDate.of(2026, 9, 15), 69.0),
            weightEntry(3, LocalDate.of(2026, 9, 30), 68.0),
        )

        val trend = build(entries, InbodyPeriod.THREE_MONTHS, today)

        assertEquals(LocalDate.of(2026, 9, 30), trend.latestMeasuredOn)
        assertEquals(3, trend.measuredCount)
    }
}
