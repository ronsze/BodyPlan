package kr.sdbk.bodyplan.core.domain.model

/** 이번 주 목표 대비 진행. 목표가 없으면 만들어지지 않는다. */
data class WeeklyGoalProgress(val goalDays: Int, val doneDays: Int, val streakWeeks: Int) {
    val isAchieved: Boolean get() = doneDays >= goalDays
}
