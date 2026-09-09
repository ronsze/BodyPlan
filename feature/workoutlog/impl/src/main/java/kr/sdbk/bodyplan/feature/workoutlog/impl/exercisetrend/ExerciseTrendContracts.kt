package kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend

import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrend
import kr.sdbk.bodyplan.core.domain.model.RecordedExercise
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class ExerciseTrendState(
    val selectedBodyPart: BodyPart? = null,
    val exercises: List<RecordedExercise> = emptyList(),
    val selectedExerciseId: Long? = null,
    val trend: ExerciseTrend? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : State {
    /** 화면이 그리는 칩. 부위로 거른 것을 상태에 따로 담지 않는다 — 두 값이 어긋날 자리를 만들지 않는다. */
    val selectableExercises: List<RecordedExercise>
        get() = exercises.filter { it.bodyPart == selectedBodyPart }
}

internal sealed interface ExerciseTrendIntent : Intent {
    data class SelectBodyPart(val bodyPart: BodyPart) : ExerciseTrendIntent

    data class SelectExercise(val id: Long) : ExerciseTrendIntent

    data object ClickBack : ExerciseTrendIntent

    data object ClickRetry : ExerciseTrendIntent
}

internal sealed interface ExerciseTrendEffect : Effect {
    data object GoBack : ExerciseTrendEffect
}
