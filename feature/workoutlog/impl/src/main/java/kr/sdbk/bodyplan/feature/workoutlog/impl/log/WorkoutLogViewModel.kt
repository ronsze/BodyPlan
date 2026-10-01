package kr.sdbk.bodyplan.feature.workoutlog.impl.log

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolumeComparison
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutMemo
import kr.sdbk.bodyplan.core.domain.model.WorkoutSetKey
import kr.sdbk.bodyplan.core.domain.repository.RoutineRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutSessionController
import kr.sdbk.bodyplan.core.domain.usecase.CompareBodyPartVolumeWithDateUseCase
import kr.sdbk.bodyplan.core.domain.usecase.GetMonthlyDayStatusUseCase
import kr.sdbk.bodyplan.core.domain.usecase.IsEditableDateUseCase
import kr.sdbk.bodyplan.core.domain.usecase.ObserveWorkoutLogUseCase
import kr.sdbk.bodyplan.core.domain.usecase.SummarizeBodyPartVolumeUseCase
import kr.sdbk.bodyplan.core.ui.components.volumeText
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutLogNavKey

@HiltViewModel(assistedFactory = WorkoutLogViewModel.Factory::class)
internal class WorkoutLogViewModel
@AssistedInject
constructor(
    private val workoutLogRepository: WorkoutLogRepository,
    private val routineRepository: RoutineRepository,
    private val isEditableDate: IsEditableDateUseCase,
    private val observeWorkoutLog: ObserveWorkoutLogUseCase,
    private val summarizeBodyPartVolume: SummarizeBodyPartVolumeUseCase,
    private val compareBodyPartVolumeWithDate: CompareBodyPartVolumeWithDateUseCase,
    private val getMonthlyDayStatus: GetMonthlyDayStatusUseCase,
    private val clock: Clock,
    private val sessionController: WorkoutSessionController,
    @Assisted navKey: WorkoutLogNavKey,
) : BaseViewModel<WorkoutLogState, WorkoutLogIntent, WorkoutLogEffect>(
    initialState = WorkoutLogState(date = LocalDate.ofEpochDay(navKey.dateEpochDay)),
) {
    private var loadJob: Job? = null
    private var pickerMonthJob: Job? = null

    /** 칩을 연달아 눌러도 저장이 뒤섞이지 않게 한 줄로 세운다 — 뒤에 시작한 저장이 앞의 것을 덮어쓰면 안 된다. */
    private val memoSaveMutex = Mutex()

    override suspend fun initializeData() {
        // 자정을 넘겨도 화면 안에서 편집 가능 여부가 바뀌지 않도록 진입 시점에 한 번만 판정한다.
        val today = LocalDate.now(clock)
        updateState { it.copy(isEditable = isEditableDate(it.date, today), canStartSession = it.date == today) }
        observeLog()
        // 조회 전용 날짜에는 불러올 일이 없어 루틴을 읽지 않는다.
        if (state.value.isEditable) observeRoutines()
        // 세션은 앱 전역에 하나뿐이고 오늘의 것이다. 다른 날짜 화면은 비추지 않는다.
        if (state.value.canStartSession) observeSession()
    }

    override fun handleIntent(intent: WorkoutLogIntent) {
        when (intent) {
            is WorkoutLogIntent.ClickAddEntry ->
                updateEffect(WorkoutLogEffect.NavigateToEntryEdit(state.value.date, null))

            is WorkoutLogIntent.ClickEntry ->
                updateEffect(WorkoutLogEffect.NavigateToEntryEdit(state.value.date, intent.id))

            is WorkoutLogIntent.ClickDeleteEntry -> deleteEntry(intent.id)

            is WorkoutLogIntent.ClickBack -> updateEffect(WorkoutLogEffect.GoBack)

            is WorkoutLogIntent.ClickRetry -> observeLog()

            is WorkoutLogIntent.SelectCondition ->
                updateMemoChips { it.copy(condition = intent.condition.takeUnless { c -> c == it.condition }) }

            is WorkoutLogIntent.SelectEffort ->
                updateMemoChips { it.copy(effort = intent.effort.takeUnless { e -> e == it.effort }) }

            is WorkoutLogIntent.TogglePainPart ->
                updateMemoChips { it.copy(painParts = it.painParts.toggled(intent.bodyPart)) }

            is WorkoutLogIntent.ClickPickComparisonDate -> openComparisonPicker()

            is WorkoutLogIntent.DismissComparisonDatePicker -> closeComparisonPicker()

            is WorkoutLogIntent.ChangeComparisonPickerMonth -> observePickerMonth(intent.yearMonth)

            is WorkoutLogIntent.SelectComparisonDate -> changeComparisonDate(intent.date)

            is WorkoutLogIntent.ClickClearComparisonDate -> changeComparisonDate(null)

            is WorkoutLogIntent.ClickEditMemo -> startEditingMemo()

            is WorkoutLogIntent.ChangeMemoInput ->
                updateState { it.copy(memoInput = intent.text.take(MEMO_MAX_LENGTH)) }

            is WorkoutLogIntent.ClickSaveMemo -> saveMemo()

            is WorkoutLogIntent.ClickCancelMemo -> cancelEditingMemo()

            is WorkoutLogIntent.ClickLoadRoutine -> openRoutineSheet()

            is WorkoutLogIntent.DismissRoutineSheet -> dismissRoutineSheet()

            is WorkoutLogIntent.SelectRoutine -> applyRoutine(intent.id)

            is WorkoutLogIntent.ClickStartSession -> startSession()

            is WorkoutLogIntent.ClickPauseSession -> sessionController.pause()

            is WorkoutLogIntent.ClickResumeSession -> sessionController.resume()

            is WorkoutLogIntent.ClickEndSession -> endSession()

            is WorkoutLogIntent.ToggleSetCompleted -> sessionController.toggleSet(intent.key)

            is WorkoutLogIntent.ClickAdjustRest -> sessionController.adjustRest(intent.deltaSeconds)

            is WorkoutLogIntent.ClickSkipRest -> sessionController.skipRest()
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionController.session.collect { session -> updateState { it.copy(session = session) } }
        }
    }

    private fun startSession() {
        if (!state.value.canStartSession || state.value.isSessionActive) return
        sessionController.start()
    }

    /** 경과는 끝내기 전에 읽는다 — 끝내면 세션이 사라진다. */
    private fun endSession() {
        val session = state.value.session ?: return
        val minutes = session.elapsedSeconds / SECONDS_PER_MINUTE
        val volume = volumeText(state.value.bodyPartVolumes.sumOf { it.weightVolumeKg })
        sessionController.end()
        updateEffect(WorkoutLogEffect.ShowMessage("운동 종료 · ${minutes}분 · 볼륨 $volume"))
    }

    private fun observeLog() {
        loadJob?.cancel()
        updateState { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            observeWorkoutLog(state.value.date)
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { (log, personalRecordExerciseIds) ->
                    val bodyPartVolumes = summarizeBodyPartVolume(log.entries)
                    val comparisons = compare(log.entries, state.value.comparisonDate)
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            entries = log.entries,
                            bodyPartVolumes = bodyPartVolumes,
                            bodyPartVolumeComparisons = comparisons,
                            personalRecordExerciseIds = personalRecordExerciseIds,
                            memo = log.memo,
                        )
                    }
                }
        }
    }

    /** 비교는 덤이라 못 읽어도 일지는 보여야 한다 — 비교 줄만 비운다. */
    private suspend fun compare(
        entries: List<WorkoutEntry>,
        comparisonDate: LocalDate?,
    ): List<BodyPartVolumeComparison> = if (comparisonDate == null) {
        emptyList()
    } else {
        runCatching { compareBodyPartVolumeWithDate(entries, comparisonDate) }.getOrDefault(emptyList())
    }

    /** 달력은 이미 고른 날이 있으면 그 달, 없으면 보고 있는 날의 달에서 연다. */
    private fun openComparisonPicker() {
        updateState { it.copy(isComparisonDatePickerVisible = true) }
        observePickerMonth(YearMonth.from(state.value.comparisonDate ?: state.value.date))
    }

    private fun closeComparisonPicker(comparisonDate: LocalDate? = state.value.comparisonDate) {
        pickerMonthJob?.cancel()
        updateState { it.copy(isComparisonDatePickerVisible = false, comparisonDate = comparisonDate) }
    }

    /** 날짜별 상태는 덤이라 못 읽어도 고르기는 되어야 한다 — 비워 두면 모든 날이 기록 없음으로 그려진다. */
    private fun observePickerMonth(yearMonth: YearMonth) {
        pickerMonthJob?.cancel()
        updateState { it.copy(comparisonPickerMonth = yearMonth) }
        pickerMonthJob = viewModelScope.launch {
            getMonthlyDayStatus(yearMonth)
                .catch { updateState { it.copy(comparisonPickerStatuses = emptyMap()) } }
                .collect { statuses -> updateState { it.copy(comparisonPickerStatuses = statuses) } }
        }
    }

    private fun changeComparisonDate(date: LocalDate?) {
        // 같은 날이나 이후는 견줄 기록이 아니라 고를 수 없게 하지만, 상태 쪽에서도 막는다.
        if (date != null && !date.isBefore(state.value.date)) return
        closeComparisonPicker(comparisonDate = date)
        viewModelScope.launch {
            val comparisons = compare(state.value.entries, date)
            updateState { it.copy(bodyPartVolumeComparisons = comparisons) }
        }
    }

    private fun startEditingMemo() {
        // 조회 전용 날짜에서는 편집 버튼을 그리지 않지만, 상태 쪽에서도 한 번 더 막는다.
        if (!state.value.isEditable) return
        updateState { it.copy(isEditingMemo = true, memoInput = it.memo?.text.orEmpty()) }
    }

    private fun cancelEditingMemo() {
        // 저장 중에는 닫지 않는다 — 닫아 둔 채로 저장이 실패하면 쓰던 내용을 되돌릴 자리가 없다.
        if (state.value.isSavingMemo) return
        updateState { it.copy(isEditingMemo = false, memoInput = "") }
    }

    private fun saveMemo() {
        val target = state.value
        // 편집을 여는 쪽과 같은 방침으로 막는다 — 조회 전용 날짜의 기록은 상태 쪽에서도 바뀌지 않는다.
        // 편집 중이 아니면 memoInput이 비어 있어, 그대로 저장하면 남아 있는 메모를 지워 버린다.
        if (!target.isEditable || !target.isEditingMemo || target.isSavingMemo) return
        updateState { it.copy(isSavingMemo = true) }
        val memo = (target.memo ?: WorkoutMemo()).copy(text = target.memoInput.trim().ifEmpty { null })
        viewModelScope.launch {
            runCatching { memoSaveMutex.withLock { workoutLogRepository.saveMemo(target.date, memo) } }
                // 저장된 메모는 observeLog 스트림이 물어 오므로 여기서는 편집 상태만 닫는다.
                .onSuccess { updateState { it.copy(isSavingMemo = false, isEditingMemo = false, memoInput = "") } }
                .onFailure {
                    // 쓰던 내용을 지우지 않는다 — 같은 자리에서 다시 저장할 수 있어야 한다.
                    updateState { it.copy(isSavingMemo = false) }
                    updateEffect(WorkoutLogEffect.ShowMessage(MEMO_SAVE_ERROR))
                }
        }
    }

    /**
     * 칩을 바꿔 바로 저장한다. 저장 버튼을 두면 탭 한 번으로 끝난다는 이점이 사라진다.
     *
     * 화면은 저장을 기다리지 않고 먼저 바꾸고, 실패하면 눌러 보기 전 값으로 되돌린다 —
     * 저장된 값은 observeLog가 다시 물어 오지만 실패에는 물어 올 변화가 없다.
     */
    private fun updateMemoChips(transform: (WorkoutMemo) -> WorkoutMemo) {
        val target = state.value
        // 조회 전용 날짜에는 칩이 눌리지 않지만, 상태 쪽에서도 막는다.
        if (!target.isEditable) return
        val previous = target.memo
        val updated = transform(previous ?: WorkoutMemo())
        updateState { it.copy(memo = updated.takeUnless { memo -> memo.isEmpty }) }
        viewModelScope.launch {
            runCatching { memoSaveMutex.withLock { workoutLogRepository.saveMemo(target.date, updated) } }
                .onFailure {
                    updateState { it.copy(memo = previous) }
                    updateEffect(WorkoutLogEffect.ShowMessage(MEMO_SAVE_ERROR))
                }
        }
    }

    private fun observeRoutines() {
        viewModelScope.launch {
            routineRepository.observeRoutines()
                // 루틴이 안 읽혀도 일지는 봐야 하므로 화면을 에러로 바꾸지 않고 알리기만 한다.
                .catch { updateEffect(WorkoutLogEffect.ShowMessage(ROUTINE_APPLY_ERROR)) }
                .collect { routines -> updateState { it.copy(routines = routines) } }
        }
    }

    private fun openRoutineSheet() {
        // 버튼을 그리지 않지만 상태 쪽에서도 막는다 — 조회 전용 날짜의 기록은 바뀌지 않는다.
        if (!state.value.isEditable) return
        updateState { it.copy(isRoutineSheetVisible = true) }
    }

    private fun dismissRoutineSheet() {
        // 넣는 중에는 닫지 않는다 — 실패를 알릴 자리가 시트다.
        if (state.value.isApplyingRoutine) return
        updateState { it.copy(isRoutineSheetVisible = false) }
    }

    private fun applyRoutine(id: Long) {
        val target = state.value
        if (!target.isEditable || target.isApplyingRoutine) return
        updateState { it.copy(isApplyingRoutine = true) }
        viewModelScope.launch {
            runCatching {
                val routine = routineRepository.getRoutine(id)
                if (routine == null || routine.entries.isEmpty()) {
                    updateEffect(WorkoutLogEffect.ShowMessage(ROUTINE_EMPTY))
                    return@runCatching false
                }
                workoutLogRepository.addEntries(target.date, routine.entries)
                true
            }.onSuccess { applied ->
                // 넣은 기록은 observeLog 스트림이 물어 오므로 여기서는 시트만 닫는다.
                updateState { it.copy(isApplyingRoutine = false, isRoutineSheetVisible = !applied) }
            }.onFailure {
                updateState { it.copy(isApplyingRoutine = false) }
                updateEffect(WorkoutLogEffect.ShowMessage(ROUTINE_APPLY_ERROR))
            }
        }
    }

    private fun deleteEntry(id: Long) {
        viewModelScope.launch {
            runCatching { workoutLogRepository.deleteEntry(id) }
                .onFailure { updateEffect(WorkoutLogEffect.ShowMessage(DELETE_ERROR)) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: WorkoutLogNavKey): WorkoutLogViewModel
    }
}

private const val SECONDS_PER_MINUTE = 60L

private const val LOAD_ERROR = "불러오지 못했습니다"
private const val DELETE_ERROR = "삭제하지 못했습니다"
private fun Set<BodyPart>.toggled(bodyPart: BodyPart): Set<BodyPart> =
    if (bodyPart in this) this - bodyPart else this + bodyPart

private const val MEMO_SAVE_ERROR = "메모를 저장하지 못했습니다"
private const val ROUTINE_EMPTY = "루틴에 종목이 없습니다"
private const val ROUTINE_APPLY_ERROR = "루틴을 불러오지 못했습니다"

/** 하루 한 줄 남기는 자리라 길이를 묶어 둔다. */
private const val MEMO_MAX_LENGTH = 500
