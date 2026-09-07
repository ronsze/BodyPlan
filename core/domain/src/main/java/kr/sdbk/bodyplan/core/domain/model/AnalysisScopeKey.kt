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

    private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
}
