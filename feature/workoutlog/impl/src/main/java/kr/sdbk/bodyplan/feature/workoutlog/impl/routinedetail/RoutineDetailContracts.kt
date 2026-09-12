package kr.sdbk.bodyplan.feature.workoutlog.impl.routinedetail

import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class RoutineDetailState(
    val routineId: Long,
    val routine: Routine? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : State

internal sealed interface RoutineDetailIntent : Intent {
    data object ClickAddEntry : RoutineDetailIntent

    data class ClickEntry(val id: Long) : RoutineDetailIntent

    data class ClickDeleteEntry(val id: Long) : RoutineDetailIntent

    data object ClickBack : RoutineDetailIntent

    data object ClickRetry : RoutineDetailIntent
}

internal sealed interface RoutineDetailEffect : Effect {
    data class NavigateToEntryEdit(val routineId: Long, val entryId: Long?) : RoutineDetailEffect

    data object GoBack : RoutineDetailEffect

    data class ShowMessage(val message: String) : RoutineDetailEffect
}
