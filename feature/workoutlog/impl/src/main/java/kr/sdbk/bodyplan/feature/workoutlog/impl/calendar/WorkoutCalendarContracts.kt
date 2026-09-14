package kr.sdbk.bodyplan.feature.workoutlog.impl.calendar

import java.time.LocalDate
import java.time.YearMonth
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolume
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
    /** 캘린더를 달 전체로 펼쳤는지. 처음엔 이번 주만 보인다. */
    val isCalendarExpanded: Boolean = false,
    /** 이번 주 부위별 볼륨. 달 조회와 별개로 구독하므로 실패도 따로 든다. */
    val weeklyVolumes: List<BodyPartVolume> = emptyList(),
    val weeklyVolumeErrorMessage: String? = null,
) : State

internal sealed interface WorkoutCalendarIntent : Intent {
    /** 캘린더가 옮겨 간 달을 그대로 받는다. 이동 폭을 화면이 되짚지 않게 한다. */
    data class ChangeMonth(val yearMonth: YearMonth) : WorkoutCalendarIntent

    data class ClickDate(val date: LocalDate) : WorkoutCalendarIntent

    data object ToggleCalendarExpansion : WorkoutCalendarIntent

    data object ClickManageExercise : WorkoutCalendarIntent

    data object ClickExerciseTrend : WorkoutCalendarIntent

    data object ClickRetry : WorkoutCalendarIntent

    data object ClickRetryWeeklyVolume : WorkoutCalendarIntent
}

internal sealed interface WorkoutCalendarEffect : Effect {
    data class NavigateToLog(val date: LocalDate) : WorkoutCalendarEffect

    data object NavigateToExerciseManage : WorkoutCalendarEffect

    data object NavigateToExerciseTrend : WorkoutCalendarEffect
}
