package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kr.sdbk.bodyplan.core.domain.model.AnalysisChild
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.core.domain.model.MonthPart
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository

/**
 * 위 계층이 종합할 아래 계층의 결과를 모은다.
 *
 * 식단과 운동이 같은 얼개라 한 곳에 둔다. 없는 것은 만들지 않고 건너뛴다 —
 * 아래를 자동으로 타고 내려가면 버튼 한 번에 호출이 서른 번 넘게 날 수 있다.
 */
class AnalysisChildren
@Inject
constructor(private val analysisResultRepository: AnalysisResultRepository) {
    /** 주가 종합할 것: 그 주 날짜들의 분석. */
    suspend fun ofWeek(dailyKind: AnalysisKind, from: LocalDate, to: LocalDate): List<AnalysisChild> {
        val dates = datesOf(from, to)
        val saved = analysisResultRepository.getLatestOf(dailyKind, dates.map(AnalysisScopeKey::daily))
        return dates.mapNotNull { date ->
            saved[AnalysisScopeKey.daily(date)]?.let {
                AnalysisChild(label = date.format(DAY_FORMAT), content = it.content)
            }
        }
    }

    /**
     * 달이 종합할 것: 온전한 주는 주 분석, 잘린 주의 날들은 날짜 분석.
     *
     * 잘린 주를 주째로 쓰면 옆 달 기록이 섞인다.
     */
    suspend fun ofMonth(
        dailyKind: AnalysisKind,
        weeklyKind: AnalysisKind,
        anyDateInMonth: LocalDate,
    ): List<AnalysisChild> {
        val parts = AnalysisScopeKey.monthParts(anyDateInMonth)
        val weekKeys = parts.filterIsInstance<MonthPart.Week>().map { AnalysisScopeKey.weekly(it.start) }
        val dayKeys = parts.filterIsInstance<MonthPart.Day>().map { AnalysisScopeKey.daily(it.date) }
        val weeks = analysisResultRepository.getLatestOf(weeklyKind, weekKeys)
        val days = analysisResultRepository.getLatestOf(dailyKind, dayKeys)

        return parts.mapNotNull { part ->
            when (part) {
                is MonthPart.Week -> weeks[AnalysisScopeKey.weekly(part.start)]?.let {
                    val end = part.start.plusDays(6)
                    AnalysisChild(
                        label = "${part.start.format(DAY_FORMAT)} ~ ${end.format(SHORT_DAY_FORMAT)}",
                        content = it.content,
                    )
                }

                is MonthPart.Day -> days[AnalysisScopeKey.daily(part.date)]?.let {
                    AnalysisChild(label = part.date.format(DAY_FORMAT), content = it.content)
                }
            }
        }
    }

    private fun datesOf(from: LocalDate, to: LocalDate): List<LocalDate> = generateSequence(from) { it.plusDays(1) }
        .takeWhile { !it.isAfter(to) }
        .toList()
}

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
private val SHORT_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일")
