package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisScopeKeyTest {
    @Test
    fun `달 안에 온전히 든 주는 Week로 잘린 주의 날들은 Day로 나온다`() {
        // 2027년 2월은 1일이 월요일, 28일이 일요일이라 잘린 주가 없다.
        val parts = AnalysisScopeKey.monthParts(LocalDate.of(2027, 2, 15))

        assertEquals(
            listOf(
                MonthPart.Week(LocalDate.of(2027, 2, 1)),
                MonthPart.Week(LocalDate.of(2027, 2, 8)),
                MonthPart.Week(LocalDate.of(2027, 2, 15)),
                MonthPart.Week(LocalDate.of(2027, 2, 22)),
            ),
            parts,
        )
    }

    @Test
    fun `첫날이 월요일인 달은 잘린 주 없이 첫 주부터 온전한 주로 시작한다`() {
        // 2026년 6월은 1일이 월요일이다.
        val parts = AnalysisScopeKey.monthParts(LocalDate.of(2026, 6, 1))

        assertEquals(MonthPart.Week(LocalDate.of(2026, 6, 1)), parts.first())
    }

    @Test
    fun `마지막 날이 일요일인 달은 잘린 주 없이 마지막 주까지 온전한 주로 끝난다`() {
        // 2025년 11월은 30일이 일요일이다.
        val parts = AnalysisScopeKey.monthParts(LocalDate.of(2025, 11, 1))

        assertEquals(MonthPart.Week(LocalDate.of(2025, 11, 24)), parts.last())
    }

    @Test
    fun `윤년 2월은 앞뒤로 잘린 주의 날들이 Day로 나온다`() {
        // 2024년 2월은 29일까지 있고, 1일이 목요일 29일이 목요일이라 앞뒤가 다 잘린다.
        val parts = AnalysisScopeKey.monthParts(LocalDate.of(2024, 2, 10))

        assertEquals(
            listOf(
                MonthPart.Day(LocalDate.of(2024, 2, 1)),
                MonthPart.Day(LocalDate.of(2024, 2, 2)),
                MonthPart.Day(LocalDate.of(2024, 2, 3)),
                MonthPart.Day(LocalDate.of(2024, 2, 4)),
                MonthPart.Week(LocalDate.of(2024, 2, 5)),
                MonthPart.Week(LocalDate.of(2024, 2, 12)),
                MonthPart.Week(LocalDate.of(2024, 2, 19)),
                MonthPart.Day(LocalDate.of(2024, 2, 26)),
                MonthPart.Day(LocalDate.of(2024, 2, 27)),
                MonthPart.Day(LocalDate.of(2024, 2, 28)),
                MonthPart.Day(LocalDate.of(2024, 2, 29)),
            ),
            parts,
        )
    }

    @Test
    fun `평달은 앞뒤로 잘린 주의 날들이 Day로 나온다`() {
        // 2026년 9월은 1일이 화요일, 30일이 수요일이다.
        val parts = AnalysisScopeKey.monthParts(LocalDate.of(2026, 9, 7))

        assertEquals(
            listOf(
                MonthPart.Day(LocalDate.of(2026, 9, 1)),
                MonthPart.Day(LocalDate.of(2026, 9, 2)),
                MonthPart.Day(LocalDate.of(2026, 9, 3)),
                MonthPart.Day(LocalDate.of(2026, 9, 4)),
                MonthPart.Day(LocalDate.of(2026, 9, 5)),
                MonthPart.Day(LocalDate.of(2026, 9, 6)),
                MonthPart.Week(LocalDate.of(2026, 9, 7)),
                MonthPart.Week(LocalDate.of(2026, 9, 14)),
                MonthPart.Week(LocalDate.of(2026, 9, 21)),
                MonthPart.Day(LocalDate.of(2026, 9, 28)),
                MonthPart.Day(LocalDate.of(2026, 9, 29)),
                MonthPart.Day(LocalDate.of(2026, 9, 30)),
            ),
            parts,
        )
    }

    @Test
    fun `조각들은 겹치지 않고 그 달의 모든 날을 빠짐없이 덮는다`() {
        // 2024년 1월부터 24개월을 훑어 매달 첫날부터 마지막날까지 정확히 한 번씩만 나오는지 본다.
        var month = LocalDate.of(2024, 1, 1)
        repeat(24) {
            val first = month.withDayOfMonth(1)
            val last = month.withDayOfMonth(month.lengthOfMonth())
            val expectedDays = generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toList()

            val coveredDays = AnalysisScopeKey.monthParts(month).flatMap { part ->
                when (part) {
                    is MonthPart.Week -> (0..6).map { part.start.plusDays(it.toLong()) }
                    is MonthPart.Day -> listOf(part.date)
                }
            }

            assertEquals("겹치지 않아야 한다: $month", coveredDays.size, coveredDays.toSet().size)
            assertEquals("빠짐없이 덮어야 한다: $month", expectedDays.toSet(), coveredDays.toSet())
            assertTrue("그 달의 날만 담아야 한다: $month", coveredDays.all { it in first..last })

            month = month.plusMonths(1)
        }
    }
}
