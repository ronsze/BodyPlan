package kr.sdbk.bodyplan.feature.dietlog.impl.analysis

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.feature.dietlog.api.DietAnalysisPeriod

/**
 * 기간 하나를 이루는 값 묶음.
 *
 * 화면·저장 열쇠·분석 범위가 같은 곳에서 나와야 저장한 결과를 다시 찾을 수 있어 한 번에 만든다.
 */
internal data class DietAnalysisPeriodInfo(
    val kind: AnalysisKind,
    val scopeKey: String,
    val label: String,
    val from: LocalDate,
    val to: LocalDate,
)

internal fun dietAnalysisPeriodInfo(period: DietAnalysisPeriod, date: LocalDate): DietAnalysisPeriodInfo =
    when (period) {
        DietAnalysisPeriod.DAILY -> DietAnalysisPeriodInfo(
            kind = AnalysisKind.DIET_DAILY,
            scopeKey = AnalysisScopeKey.daily(date),
            label = date.format(DAY_FORMAT),
            from = date,
            to = date,
        )

        DietAnalysisPeriod.WEEKLY -> {
            val start = AnalysisScopeKey.weekStart(date)
            val end = start.plusDays(6)
            DietAnalysisPeriodInfo(
                kind = AnalysisKind.DIET_WEEKLY,
                scopeKey = AnalysisScopeKey.weekly(date),
                label = "${start.format(DAY_FORMAT)} ~ ${end.format(SHORT_DAY_FORMAT)}",
                from = start,
                to = end,
            )
        }

        DietAnalysisPeriod.MONTHLY -> DietAnalysisPeriodInfo(
            kind = AnalysisKind.DIET_MONTHLY,
            scopeKey = AnalysisScopeKey.monthly(date),
            label = date.format(MONTH_FORMAT),
            from = date.withDayOfMonth(1),
            to = date.withDayOfMonth(date.lengthOfMonth()),
        )
    }

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
private val SHORT_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일")
private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월")
