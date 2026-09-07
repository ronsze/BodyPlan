package kr.sdbk.bodyplan.feature.dietlog.impl.analysis

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.feature.dietlog.api.DietAnalysisPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

internal class DietAnalysisPeriodInfoTest {
    private val date = LocalDate.of(2026, 9, 10)

    @Test
    fun `날짜별은 그 날 하루만 범위로 삼는다`() {
        val info = dietAnalysisPeriodInfo(DietAnalysisPeriod.DAILY, date)

        assertEquals(AnalysisKind.DIET_DAILY, info.kind)
        assertEquals(AnalysisScopeKey.daily(date), info.scopeKey)
        assertEquals(date, info.from)
        assertEquals(date, info.to)
    }

    @Test
    fun `주간은 그 주의 월요일부터 일요일까지를 범위로 삼는다`() {
        val info = dietAnalysisPeriodInfo(DietAnalysisPeriod.WEEKLY, date)

        assertEquals(AnalysisKind.DIET_WEEKLY, info.kind)
        assertEquals(AnalysisScopeKey.weekly(date), info.scopeKey)
        assertEquals(LocalDate.of(2026, 9, 7), info.from)
        assertEquals(LocalDate.of(2026, 9, 13), info.to)
    }

    @Test
    fun `월간은 그 달의 첫날부터 마지막 날까지를 범위로 삼는다`() {
        val info = dietAnalysisPeriodInfo(DietAnalysisPeriod.MONTHLY, date)

        assertEquals(AnalysisKind.DIET_MONTHLY, info.kind)
        assertEquals(AnalysisScopeKey.monthly(date), info.scopeKey)
        assertEquals(LocalDate.of(2026, 9, 1), info.from)
        assertEquals(LocalDate.of(2026, 9, 30), info.to)
    }
}
