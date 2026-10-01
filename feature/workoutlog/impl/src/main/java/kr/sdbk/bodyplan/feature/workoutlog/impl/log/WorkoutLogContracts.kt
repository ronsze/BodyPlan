package kr.sdbk.bodyplan.feature.workoutlog.impl.log

import java.time.LocalDate
import java.time.YearMonth
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolume
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolumeComparison
import kr.sdbk.bodyplan.core.domain.model.Condition
import kr.sdbk.bodyplan.core.domain.model.DayStatus
import kr.sdbk.bodyplan.core.domain.model.Effort
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutMemo
import kr.sdbk.bodyplan.core.domain.model.WorkoutSession
import kr.sdbk.bodyplan.core.domain.model.WorkoutSetKey
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class WorkoutLogState(
    val date: LocalDate,
    val entries: List<WorkoutEntry> = emptyList(),
    /** [entries]에서 센 부위별 무게 볼륨. 셈이 UseCase에 있어 파생 getter가 아니라 필드다. */
    val bodyPartVolumes: List<BodyPartVolume> = emptyList(),
    /** 볼륨을 견줄 날. 사용자가 고르기 전에는 `null`이고, 그동안 비교는 나오지 않는다. */
    val comparisonDate: LocalDate? = null,
    val isComparisonDatePickerVisible: Boolean = false,
    /** 견줄 날을 고르는 달력이 보여 주는 달과 그 달의 날짜별 상태. 운동 기록 캘린더와 같은 내용을 그린다. */
    val comparisonPickerMonth: YearMonth = YearMonth.from(date),
    val comparisonPickerStatuses: Map<LocalDate, DayStatus> = emptyMap(),
    /** [bodyPartVolumes]를 [comparisonDate]의 같은 부위와 견준 것. 날짜를 고르기 전이나 견줄 수 없으면 비어 있다. */
    val bodyPartVolumeComparisons: List<BodyPartVolumeComparison> = emptyList(),
    /** 그날 최고값을 갱신한 종목. 저장하지 않고 볼 때마다 판정한다. */
    val personalRecordExerciseIds: Set<Long> = emptySet(),
    val isEditable: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val memo: WorkoutMemo? = null,
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
}

internal sealed interface WorkoutLogIntent : Intent {
    data object ClickAddEntry : WorkoutLogIntent

    data class ClickEntry(val id: Long) : WorkoutLogIntent

    data class ClickDeleteEntry(val id: Long) : WorkoutLogIntent

    data object ClickBack : WorkoutLogIntent

    data object ClickRetry : WorkoutLogIntent

    /** 이미 고른 값을 다시 누르면 해제한다. 칩은 누르는 즉시 저장된다. */
    data class SelectCondition(val condition: Condition) : WorkoutLogIntent

    data class SelectEffort(val effort: Effort) : WorkoutLogIntent

    data class TogglePainPart(val bodyPart: BodyPart) : WorkoutLogIntent

    data object ClickPickComparisonDate : WorkoutLogIntent

    data class SelectComparisonDate(val date: LocalDate) : WorkoutLogIntent

    data object DismissComparisonDatePicker : WorkoutLogIntent

    data class ChangeComparisonPickerMonth(val yearMonth: YearMonth) : WorkoutLogIntent

    data object ClickClearComparisonDate : WorkoutLogIntent

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
