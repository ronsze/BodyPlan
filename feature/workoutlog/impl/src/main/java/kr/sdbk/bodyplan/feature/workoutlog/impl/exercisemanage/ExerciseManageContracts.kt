package kr.sdbk.bodyplan.feature.workoutlog.impl.exercisemanage

import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class ExerciseManageState(
    val selectedBodyPart: BodyPart = BodyPart.CHEST,
    val exercises: List<Exercise> = emptyList(),
    val editingExercise: Exercise? = null,
    val isDialogVisible: Boolean = false,
    val dialogName: String = "",
    val dialogIntensityType: IntensityType = IntensityType.WEIGHT,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : State

internal sealed interface ExerciseManageIntent : Intent {
    data class SelectBodyPart(val bodyPart: BodyPart) : ExerciseManageIntent

    data object ClickAdd : ExerciseManageIntent

    data class ClickEdit(val id: Long) : ExerciseManageIntent

    data class ClickDelete(val id: Long) : ExerciseManageIntent

    data class ChangeDialogName(val value: String) : ExerciseManageIntent

    data class SelectDialogIntensityType(val type: IntensityType) : ExerciseManageIntent

    data object ConfirmDialog : ExerciseManageIntent

    data object DismissDialog : ExerciseManageIntent

    data object ClickBack : ExerciseManageIntent

    data object ClickRetry : ExerciseManageIntent
}

internal sealed interface ExerciseManageEffect : Effect {
    data object GoBack : ExerciseManageEffect

    data class ShowMessage(val message: String) : ExerciseManageEffect
}
