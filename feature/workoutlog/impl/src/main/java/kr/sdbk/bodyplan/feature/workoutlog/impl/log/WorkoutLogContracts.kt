package kr.sdbk.bodyplan.feature.workoutlog.impl.log

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolume
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSession
import kr.sdbk.bodyplan.core.domain.model.WorkoutSetKey
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
    /** 오늘만 세션을 연다. 어제도 편집은 되지만 "지금 운동 중"이 아니다. */
    val canStartSession: Boolean = false,
    /** 컨트롤러의 세션을 비춘 것. 오늘 화면에서만 채워진다. */
    val session: WorkoutSession? = null,
) : State {
    val isSessionActive: Boolean get() = session != null

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

    data object ClickStartSession : WorkoutLogIntent

    data object ClickPauseSession : WorkoutLogIntent

    data object ClickResumeSession : WorkoutLogIntent

    data object ClickEndSession : WorkoutLogIntent

    data class ToggleSetCompleted(val key: WorkoutSetKey) : WorkoutLogIntent

    data class ClickAdjustRest(val deltaSeconds: Int) : WorkoutLogIntent

    data object ClickSkipRest : WorkoutLogIntent
}

internal sealed interface WorkoutLogEffect : Effect {
    data class NavigateToEntryEdit(val date: LocalDate, val entryId: Long?) : WorkoutLogEffect

    data object GoBack : WorkoutLogEffect

    data class ShowMessage(val message: String) : WorkoutLogEffect
}
