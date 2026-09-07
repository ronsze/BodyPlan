package kr.sdbk.bodyplan.feature.workoutlog.impl.calendar

import java.time.LocalDate
import java.time.YearMonth
import kr.sdbk.bodyplan.core.domain.model.DayStatus
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class WorkoutCalendarState(
    val yearMonth: YearMonth,
    val today: LocalDate,
    val dayStatuses: Map<LocalDate, DayStatus> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : State

internal sealed interface WorkoutCalendarIntent : Intent {
    /** 캘린더가 옮겨 간 달을 그대로 받는다. 이동 폭을 화면이 되짚지 않게 한다. */
    data class ChangeMonth(val yearMonth: YearMonth) : WorkoutCalendarIntent

    data class ClickDate(val date: LocalDate) : WorkoutCalendarIntent

    data object ClickManageExercise : WorkoutCalendarIntent

    data object ClickRetry : WorkoutCalendarIntent
}

internal sealed interface WorkoutCalendarEffect : Effect {
    data class NavigateToLog(val date: LocalDate) : WorkoutCalendarEffect

    data object NavigateToExerciseManage : WorkoutCalendarEffect
}
