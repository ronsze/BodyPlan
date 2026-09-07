package kr.sdbk.bodyplan.core.ui.components

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import org.junit.Assert.assertEquals
import org.junit.Test

/** 식단·운동이 같은 계산을 쓰므로 여기 한 벌만 둔다. */
class AnalysisPeriodInfoTest {
    @Test
    fun `날짜별은 그 날 하루만 덮고 열쇠는 epochDay다`() {
        val date = LocalDate.of(2026, 9, 8)

        val info = analysisPeriodInfo(AnalysisKind.DIET_DAILY, date)

        assertEquals(AnalysisKind.DIET_DAILY, info.kind)
        assertEquals(date.toEpochDay().toString(), info.scopeKey)
        assertEquals(date, info.from)
        assertEquals(date, info.to)
        assertEquals("2026년 9월 8일", info.label)
    }

    @Test
    fun `주간은 월요일에서 일요일까지다`() {
        // 2026-09-08은 화요일이다.
        val info = analysisPeriodInfo(AnalysisKind.WORKOUT_WEEKLY, LocalDate.of(2026, 9, 8))

        assertEquals(LocalDate.of(2026, 9, 7), info.from)
        assertEquals(LocalDate.of(2026, 9, 13), info.to)
        assertEquals(LocalDate.of(2026, 9, 7).toEpochDay().toString(), info.scopeKey)
    }

    @Test
    fun `주의 첫날에 들어와도 그 주를 앞당기지 않는다`() {
        val monday = LocalDate.of(2026, 9, 7)

        val info = analysisPeriodInfo(AnalysisKind.DIET_WEEKLY, monday)

        assertEquals(monday, info.from)
        assertEquals(LocalDate.of(2026, 9, 13), info.to)
    }

    @Test
    fun `월간은 그 달 1일부터 말일까지다`() {
        val info = analysisPeriodInfo(AnalysisKind.WORKOUT_MONTHLY, LocalDate.of(2026, 2, 15))

        assertEquals(LocalDate.of(2026, 2, 1), info.from)
        assertEquals(LocalDate.of(2026, 2, 28), info.to)
        assertEquals("2026-02", info.scopeKey)
        assertEquals("2026년 2월", info.label)
    }

    @Test
    fun `인바디는 기간이 없어 열쇠가 비어 있다`() {
        val info = analysisPeriodInfo(AnalysisKind.INBODY, LocalDate.of(2026, 9, 8))

        assertEquals("", info.scopeKey)
        assertEquals("", info.label)
    }
}
