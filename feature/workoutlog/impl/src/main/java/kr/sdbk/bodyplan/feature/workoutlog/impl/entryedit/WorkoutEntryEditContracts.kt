package kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutOptions
import kr.sdbk.bodyplan.core.domain.model.countsRepeats
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

/**
 * 편집 중인 세트 한 줄. [id]는 저장소와 무관한 화면 안의 일련번호다 —
 * 목록 키와 삭제 대상 지정에 안정된 식별자가 필요해 도메인 모델을 그대로 쓰지 않는다.
 *
 * 값이 `null`이면 빈 칸이다. 손으로 넣는 칸은 지우는 도중 비어 있을 수 있어 0과 구분해야 한다.
 */
internal data class SetInput(val id: Long, val repeatCount: Int?, val intensityValue: Int?)

/** 저장 대상. 일지는 날짜에, 루틴은 루틴 id에 묶인다. */
internal sealed interface WorkoutEntryEditTarget {
    data class Log(val date: LocalDate) : WorkoutEntryEditTarget

    data class Routine(val routineId: Long) : WorkoutEntryEditTarget
}

internal data class WorkoutEntryEditState(
    val target: WorkoutEntryEditTarget,
    val editingEntryId: Long? = null,
    val selectedBodyPart: BodyPart? = null,
    val exercises: List<Exercise> = emptyList(),
    val selectedExercise: Exercise? = null,
    val sets: List<SetInput> = emptyList(),
    val nextSetInputId: Long = 0L,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) : State {
    /**
     * 화면에 늘어놓을 종목. 지워진 종목의 기록을 수정할 때 그 종목이 [exercises]에 없으므로
     * 앞에 끼워 넣는다. 그러지 않으면 세트는 보이는데 고른 종목만 사라져 보인다.
     */
    val selectableExercises: List<Exercise>
        get() {
            val selected = selectedExercise ?: return exercises
            return if (exercises.any { it.id == selected.id }) exercises else listOf(selected) + exercises
        }

    val canSave: Boolean
        get() {
            val exercise = selectedExercise ?: return false
            return sets.isNotEmpty() && !isSaving && sets.all { it.isComplete(exercise.intensityType) }
        }

    /** 루틴은 부위 하나에 묶여 있어 항목의 부위를 고르게 하지 않는다. */
    val isBodyPartLocked: Boolean get() = target is WorkoutEntryEditTarget.Routine

    val canAddSet: Boolean get() = selectedExercise != null && sets.size < WorkoutOptions.MAX_SET_COUNT

    val canRemoveSet: Boolean get() = sets.size > 1
}

internal sealed interface WorkoutEntryEditIntent : Intent {
    data class SelectBodyPart(val bodyPart: BodyPart) : WorkoutEntryEditIntent

    data class SelectExercise(val id: Long) : WorkoutEntryEditIntent

    data object ClickAddSet : WorkoutEntryEditIntent

    data class ClickRemoveSet(val setInputId: Long) : WorkoutEntryEditIntent

    /** 손으로 넣는 칸과 목록에서 고르는 칸이 같은 길을 탄다 — 목록은 고른 값을 글자로 넘긴다. */
    data class ChangeSetRepeatCount(val setInputId: Long, val text: String) : WorkoutEntryEditIntent

    data class ChangeSetIntensity(val setInputId: Long, val text: String) : WorkoutEntryEditIntent

    data object ClickSave : WorkoutEntryEditIntent

    data object ClickBack : WorkoutEntryEditIntent

    data object ClickRetry : WorkoutEntryEditIntent
}

internal sealed interface WorkoutEntryEditEffect : Effect {
    data object GoBack : WorkoutEntryEditEffect

    data class ShowMessage(val message: String) : WorkoutEntryEditEffect
}

/** 저장할 수 있는 줄인지. 목록에서 고른 축은 늘 값이 있고, 손으로 넣는 축만 범위를 본다. */
internal fun SetInput.isComplete(type: IntensityType): Boolean {
    val intensity = intensityValue ?: return false
    val repeat = repeatCount ?: return false
    val intensityOk = type != IntensityType.WEIGHT || WorkoutOptions.isValidWeight(intensity)
    val repeatOk = !type.countsRepeats || WorkoutOptions.isValidRepeatCount(repeat)
    return intensityOk && repeatOk
}
