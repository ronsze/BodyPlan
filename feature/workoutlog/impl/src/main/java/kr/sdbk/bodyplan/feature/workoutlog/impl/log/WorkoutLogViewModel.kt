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
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import kr.sdbk.bodyplan.core.domain.usecase.IsEditableDateUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutLogNavKey

@HiltViewModel(assistedFactory = WorkoutLogViewModel.Factory::class)
internal class WorkoutLogViewModel
@AssistedInject
constructor(
    private val workoutLogRepository: WorkoutLogRepository,
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

/** 하루 한 줄 남기는 자리라 길이를 묶어 둔다. */
private const val MEMO_MAX_LENGTH = 500
