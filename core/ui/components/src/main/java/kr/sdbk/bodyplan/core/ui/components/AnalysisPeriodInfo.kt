package kr.sdbk.bodyplan.core.ui.components

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisPeriodUnit
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.core.domain.model.periodUnit

/**
 * 기간 하나를 이루는 값 묶음.
 *
 * 화면 표기·저장 열쇠·분석 범위가 한 곳에서 나와야 저장한 결과를 다시 찾을 수 있다.
 */
data class AnalysisPeriodInfo(
    val kind: AnalysisKind,
    val scopeKey: String,
    val label: String,
    val from: LocalDate,
    val to: LocalDate,
)

/**
 * [date]가 든 기간을 [kind]의 단위로 잡는다.
 *
 * 식단과 운동이 같은 계산을 쓴다. 주 시작과 월말 계산이 두 벌이면 한쪽만 고쳐질 수 있어
 * 여기 하나만 둔다.
 */
fun analysisPeriodInfo(kind: AnalysisKind, date: LocalDate): AnalysisPeriodInfo = when (kind.periodUnit) {
    AnalysisPeriodUnit.DAY -> AnalysisPeriodInfo(
        kind = kind,
        scopeKey = AnalysisScopeKey.daily(date),
        label = date.format(DAY_FORMAT),
        from = date,
        to = date,
    )

    AnalysisPeriodUnit.WEEK -> {
        val start = AnalysisScopeKey.weekStart(date)
        val end = start.plusDays(6)
        AnalysisPeriodInfo(
            kind = kind,
            scopeKey = AnalysisScopeKey.weekly(date),
            label = "${start.format(DAY_FORMAT)} ~ ${end.format(SHORT_DAY_FORMAT)}",
            from = start,
            to = end,
        )
    }

    AnalysisPeriodUnit.MONTH -> AnalysisPeriodInfo(
        kind = kind,
        scopeKey = AnalysisScopeKey.monthly(date),
        label = date.format(MONTH_FORMAT),
        from = date.withDayOfMonth(1),
        to = date.withDayOfMonth(date.lengthOfMonth()),
    )

    // 인바디는 기간이 없다. 매번 새 결과를 쌓으므로 열쇠도 범위도 쓰지 않는다.
    AnalysisPeriodUnit.NONE -> AnalysisPeriodInfo(
        kind = kind,
        scopeKey = "",
        label = "",
        from = date,
        to = date,
    )
}

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
private val SHORT_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일")
private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월")
