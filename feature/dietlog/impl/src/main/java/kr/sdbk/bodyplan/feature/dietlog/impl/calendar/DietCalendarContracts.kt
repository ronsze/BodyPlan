package kr.sdbk.bodyplan.feature.dietlog.impl.calendar

import java.time.LocalDate
import java.time.YearMonth
import kr.sdbk.bodyplan.core.domain.model.DietDayStatus
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class DietCalendarState(
    val yearMonth: YearMonth,
    val today: LocalDate,
    val dayStatuses: Map<LocalDate, DietDayStatus> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : State

internal sealed interface DietCalendarIntent : Intent {
    /** 캘린더가 옮겨 간 달을 그대로 받는다. 이동 폭을 화면이 되짚지 않게 한다. */
    data class ChangeMonth(val yearMonth: YearMonth) : DietCalendarIntent

    data class ClickDate(val date: LocalDate) : DietCalendarIntent

    data object ClickRetry : DietCalendarIntent
}

internal sealed interface DietCalendarEffect : Effect {
    data class NavigateToLog(val date: LocalDate) : DietCalendarEffect
}
