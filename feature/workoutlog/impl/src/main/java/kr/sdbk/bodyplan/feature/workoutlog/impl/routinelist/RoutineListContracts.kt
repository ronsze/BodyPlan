package kr.sdbk.bodyplan.feature.workoutlog.impl.routinelist

import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class RoutineListState(
    val routines: List<Routine> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isDialogVisible: Boolean = false,
    val dialogName: String = "",
    val isCreating: Boolean = false,
) : State

internal sealed interface RoutineListIntent : Intent {
    data object ClickAddRoutine : RoutineListIntent

    data class ChangeDialogName(val name: String) : RoutineListIntent

    data object ConfirmDialog : RoutineListIntent

    data object DismissDialog : RoutineListIntent

    data class ClickRoutine(val id: Long) : RoutineListIntent

    data class ClickDeleteRoutine(val id: Long) : RoutineListIntent

    data object ClickBack : RoutineListIntent

    data object ClickRetry : RoutineListIntent
}

internal sealed interface RoutineListEffect : Effect {
    data object GoBack : RoutineListEffect

    data class NavigateToRoutineDetail(val routineId: Long) : RoutineListEffect

    data class ShowMessage(val message: String) : RoutineListEffect
}
