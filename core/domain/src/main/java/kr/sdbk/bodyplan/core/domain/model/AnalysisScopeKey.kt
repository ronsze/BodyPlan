package kr.sdbk.bodyplan.core.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 저장된 분석 결과를 다시 찾는 열쇠를 만든다.
 *
 * 화면과 종합 UseCase가 같은 열쇠를 만들어야 저장한 결과를 서로 찾을 수 있어 한 곳에 둔다.
 */
object AnalysisScopeKey {
    /** 주의 첫날은 월요일로 본다. 캘린더 표기와 같다. */
    val WEEK_START: DayOfWeek = DayOfWeek.MONDAY

    fun daily(date: LocalDate): String = date.toEpochDay().toString()

    fun weekly(date: LocalDate): String = weekStart(date).toEpochDay().toString()

    fun monthly(date: LocalDate): String = date.format(MONTH_FORMAT)

    fun weekStart(date: LocalDate): LocalDate = date.minusDays(
        ((date.dayOfWeek.value - WEEK_START.value) + 7) % 7L,
    )

    /**
     * 그 달을 덮는 조각들. 달 안에 온전히 든 주는 주째로, 잘린 주의 날들은 날짜째로 쓴다.
     *
     * 잘린 주를 주째로 쓰면 옆 달 기록이 섞인다.
     */
    fun monthParts(date: LocalDate): List<MonthPart> {
        val first = date.withDayOfMonth(1)
        val last = date.withDayOfMonth(date.lengthOfMonth())
        val parts = mutableListOf<MonthPart>()
        var cursor = first
        while (!cursor.isAfter(last)) {
            val weekStart = weekStart(cursor)
            val weekEnd = weekStart.plusDays(6)
            if (weekStart >= first && weekEnd <= last) {
                parts += MonthPart.Week(weekStart)
                cursor = weekEnd.plusDays(1)
            } else {
                parts += MonthPart.Day(cursor)
                cursor = cursor.plusDays(1)
            }
        }
        return parts
    }

    private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
}

/** 달을 이루는 조각. 온전한 주이거나 잘린 주의 하루다. */
sealed interface MonthPart {
    data class Week(val start: LocalDate) : MonthPart

    data class Day(val date: LocalDate) : MonthPart
}
