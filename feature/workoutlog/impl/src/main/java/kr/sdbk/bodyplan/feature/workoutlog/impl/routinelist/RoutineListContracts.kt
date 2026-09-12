package kr.sdbk.bodyplan.feature.workoutlog.impl.routinelist

import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.groupedByBodyPart
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class RoutineListState(
    val routines: List<Routine> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isDialogVisible: Boolean = false,
    val dialogName: String = "",
    val dialogBodyPart: BodyPart = BodyPart.CHEST,
    val isCreating: Boolean = false,
) : State {
    /** 부위 순서로 묶은 목록. 루틴이 없는 부위는 나오지 않는다. */
    val sections: List<Pair<BodyPart, List<Routine>>> get() = routines.groupedByBodyPart()
}

internal sealed interface RoutineListIntent : Intent {
    data object ClickAddRoutine : RoutineListIntent

    data class ChangeDialogName(val name: String) : RoutineListIntent

    data class SelectDialogBodyPart(val bodyPart: BodyPart) : RoutineListIntent

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
