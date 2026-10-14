package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.InbodyDetail
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.InbodyMetricKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BuildInbodyDetailUseCaseTest {
    private val build = BuildInbodyDetailUseCase()
    private val day1 = LocalDate.of(2026, 9, 1)
    private val day2 = LocalDate.of(2026, 9, 8)

    private fun InbodyDetail.metric(kind: InbodyMetricKind) = metrics.first { it.kind == kind }

    @Test
    fun `제지방량은 체중과 체지방량의 표시값 차이다`() {
        val entries = listOf(inbodyEntry(1, day1, InbodyMeasurement(weightKg = 68.44, bodyFatKg = 15.06)))

        val detail = build(entries, 1)!!

        assertEquals(151, detail.bodyFatTenths)
        assertEquals(533, detail.leanMassTenths)
    }

    @Test
    fun `체중이나 체지방량이 없으면 제지방량이 없다`() {
        val entries = listOf(inbodyEntry(1, day1, InbodyMeasurement(weightKg = 68.4)))

        assertNull(build(entries, 1)!!.leanMassTenths)
    }

    @Test
    fun `이전 대비 변화량은 표시값 차이이고 이전 값을 함께 낸다`() {
        val entries = listOf(
            inbodyEntry(1, day1, InbodyMeasurement(weightKg = 69.0)),
            inbodyEntry(2, day2, InbodyMeasurement(weightKg = 68.4)),
        )

        val weight = build(entries, 2)!!.metric(InbodyMetricKind.WEIGHT)

        assertEquals(-6, weight.changeTenths)
        assertEquals(690, weight.previousTenths)
    }

    @Test
    fun `이전 측정에 그 지표 값이 없으면 그 지표의 변화량만 없다`() {
        val entries = listOf(
            inbodyEntry(1, day1, InbodyMeasurement(weightKg = 69.0)),
            inbodyEntry(2, day2, InbodyMeasurement(weightKg = 68.4, bodyFatKg = 15.0)),
        )

        val detail = build(entries, 2)!!

        assertNull(detail.metric(InbodyMetricKind.BODY_FAT_PERCENT).changeTenths)
        assertNotNull(detail.metric(InbodyMetricKind.BODY_FAT_PERCENT).valueTenths)
        assertEquals(-6, detail.metric(InbodyMetricKind.WEIGHT).changeTenths)
    }

    @Test
    fun `이전 측정이 없으면 비교 정보 전체가 없다`() {
        val entries = listOf(inbodyEntry(1, day1, InbodyMeasurement(weightKg = 68.4, bodyFatKg = 15.0)))

        val detail = build(entries, 1)!!

        assertNull(detail.previous)
        assertEquals(true, detail.metrics.all { it.changeTenths == null && it.previousTenths == null })
    }

    private fun assertNoComparison(detail: InbodyDetail) {
        assertNull(detail.previous)
        assertEquals(true, detail.metrics.all { it.previousTenths == null && it.changeTenths == null })
    }

    @Test
    fun `분석 전 기록은 앞에 측정 기록이 있어도 비교 정보가 없다`() {
        val entries = listOf(
            inbodyEntry(1, day1, InbodyMeasurement(weightKg = 69.0)),
            inbodyEntry(2, day2),
        )

        assertNoComparison(build(entries, 2)!!)
    }

    @Test
    fun `읽힌 값이 없는 결과도 비교 정보가 없다`() {
        val entries = listOf(
            inbodyEntry(1, day1, InbodyMeasurement(weightKg = 69.0)),
            inbodyEntry(2, day2, measurement = null, withResult = true),
        )

        assertNoComparison(build(entries, 2)!!)
    }

    @Test
    fun `실패했어도 이전 결과가 있으면 비교 정보가 있다`() {
        val entries = listOf(
            inbodyEntry(1, day1, InbodyMeasurement(weightKg = 69.0)),
            inbodyEntry(2, day2, InbodyMeasurement(weightKg = 68.4), failureReason = "실패"),
        )

        val detail = build(entries, 2)!!

        assertEquals(1L, detail.previous?.record?.id)
        assertEquals(-6, detail.metric(InbodyMetricKind.WEIGHT).changeTenths)
    }
}
