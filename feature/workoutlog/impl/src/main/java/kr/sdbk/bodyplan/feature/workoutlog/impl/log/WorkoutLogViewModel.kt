package kr.sdbk.bodyplan.feature.workoutlog.impl.log

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.repository.RoutineRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import kr.sdbk.bodyplan.core.domain.usecase.IsEditableDateUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutLogNavKey

@HiltViewModel(assistedFactory = WorkoutLogViewModel.Factory::class)
internal class WorkoutLogViewModel
@AssistedInject
constructor(
    private val workoutLogRepository: WorkoutLogRepository,
    private val routineRepository: RoutineRepository,
    private val isEditableDate: IsEditableDateUseCase,
    @Assisted navKey: WorkoutLogNavKey,
) : BaseViewModel<WorkoutLogState, WorkoutLogIntent, WorkoutLogEffect>(
    initialState = WorkoutLogState(date = LocalDate.ofEpochDay(navKey.dateEpochDay)),
) {
    private var loadJob: Job? = null

    override suspend fun initializeData() {
        // 자정을 넘겨도 화면 안에서 편집 가능 여부가 바뀌지 않도록 진입 시점에 한 번만 판정한다.
        updateState { it.copy(isEditable = isEditableDate(it.date)) }
        observeLog()
        // 조회 전용 날짜에는 불러올 일이 없어 루틴을 읽지 않는다.
        if (state.value.isEditable) observeRoutines()
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

            is WorkoutLogIntent.ClickEditMemo -> startEditingMemo()

            is WorkoutLogIntent.ChangeMemoInput ->
                updateState { it.copy(memoInput = intent.text.take(MEMO_MAX_LENGTH)) }

            is WorkoutLogIntent.ClickSaveMemo -> saveMemo()

            is WorkoutLogIntent.ClickCancelMemo -> cancelEditingMemo()

            is WorkoutLogIntent.ClickLoadRoutine -> openRoutineSheet()

            is WorkoutLogIntent.DismissRoutineSheet -> dismissRoutineSheet()

            is WorkoutLogIntent.SelectRoutine -> applyRoutine(intent.id)
        }
    }

    private fun observeLog() {
        loadJob?.cancel()
        updateState { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            workoutLogRepository.observeLog(state.value.date)
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { log ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            entries = log.entries,
                            memo = log.memo,
                        )
                    }
                }
        }
    }

    private fun startEditingMemo() {
        // 조회 전용 날짜에서는 편집 버튼을 그리지 않지만, 상태 쪽에서도 한 번 더 막는다.
        if (!state.value.isEditable) return
        updateState { it.copy(isEditingMemo = true, memoInput = it.memo.orEmpty()) }
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
        viewModelScope.launch {
            runCatching { workoutLogRepository.saveMemo(target.date, target.memoInput) }
                // 저장된 메모는 observeLog 스트림이 물어 오므로 여기서는 편집 상태만 닫는다.
                .onSuccess { updateState { it.copy(isSavingMemo = false, isEditingMemo = false, memoInput = "") } }
                .onFailure {
                    // 쓰던 내용을 지우지 않는다 — 같은 자리에서 다시 저장할 수 있어야 한다.
                    updateState { it.copy(isSavingMemo = false) }
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

private const val LOAD_ERROR = "불러오지 못했습니다"
private const val DELETE_ERROR = "삭제하지 못했습니다"
private const val MEMO_SAVE_ERROR = "메모를 저장하지 못했습니다"
private const val ROUTINE_EMPTY = "루틴에 종목이 없습니다"
private const val ROUTINE_APPLY_ERROR = "루틴을 불러오지 못했습니다"

/** 하루 한 줄 남기는 자리라 길이를 묶어 둔다. */
private const val MEMO_MAX_LENGTH = 500
