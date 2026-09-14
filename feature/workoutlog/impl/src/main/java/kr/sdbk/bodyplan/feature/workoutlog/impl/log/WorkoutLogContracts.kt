package kr.sdbk.bodyplan.feature.workoutlog.impl.log

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolume
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.groupedByBodyPart
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class WorkoutLogState(
    val date: LocalDate,
    val entries: List<WorkoutEntry> = emptyList(),
    /** [entries]에서 센 부위별 무게 볼륨. 셈이 UseCase에 있어 파생 getter가 아니라 필드다. */
    val bodyPartVolumes: List<BodyPartVolume> = emptyList(),
    /** 그날 최고값을 갱신한 종목. 저장하지 않고 볼 때마다 판정한다. */
    val personalRecordExerciseIds: Set<Long> = emptySet(),
    val isEditable: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val memo: String? = null,
    val memoInput: String = "",
    val isEditingMemo: Boolean = false,
    val isSavingMemo: Boolean = false,
    val routines: List<Routine> = emptyList(),
    val isRoutineSheetVisible: Boolean = false,
    val isApplyingRoutine: Boolean = false,
) : State {
    /** 시트에 보일 부위별 루틴 묶음. 루틴이 없는 부위는 나오지 않는다. */
    val routineSections: List<Pair<BodyPart, List<Routine>>> get() = routines.groupedByBodyPart()
}

internal sealed interface WorkoutLogIntent : Intent {
    data object ClickAddEntry : WorkoutLogIntent

    data class ClickEntry(val id: Long) : WorkoutLogIntent

    data class ClickDeleteEntry(val id: Long) : WorkoutLogIntent

    data object ClickBack : WorkoutLogIntent

    data object ClickRetry : WorkoutLogIntent

    data object ClickEditMemo : WorkoutLogIntent

    data class ChangeMemoInput(val text: String) : WorkoutLogIntent

    data object ClickSaveMemo : WorkoutLogIntent

    data object ClickCancelMemo : WorkoutLogIntent

    data object ClickLoadRoutine : WorkoutLogIntent

    data object DismissRoutineSheet : WorkoutLogIntent

    data class SelectRoutine(val id: Long) : WorkoutLogIntent
}

internal sealed interface WorkoutLogEffect : Effect {
    data class NavigateToEntryEdit(val date: LocalDate, val entryId: Long?) : WorkoutLogEffect

    data object GoBack : WorkoutLogEffect

    data class ShowMessage(val message: String) : WorkoutLogEffect
}
