package kr.sdbk.bodyplan.feature.workoutlog.impl.routinelist

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.repository.RoutineRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class RoutineListViewModel
@Inject
constructor(private val routineRepository: RoutineRepository) :
    BaseViewModel<RoutineListState, RoutineListIntent, RoutineListEffect>(initialState = RoutineListState()) {
    private var loadJob: Job? = null

    override suspend fun initializeData() {
        observeRoutines()
    }

    override fun handleIntent(intent: RoutineListIntent) {
        when (intent) {
            is RoutineListIntent.ClickAddRoutine ->
                updateState { it.copy(isDialogVisible = true, dialogName = "", dialogBodyPart = BodyPart.CHEST) }

            is RoutineListIntent.ChangeDialogName ->
                updateState { it.copy(dialogName = intent.name.take(ROUTINE_NAME_MAX_LENGTH)) }

            is RoutineListIntent.SelectDialogBodyPart -> updateState { it.copy(dialogBodyPart = intent.bodyPart) }

            is RoutineListIntent.ConfirmDialog -> createRoutine()

            is RoutineListIntent.DismissDialog -> dismissDialog()

            is RoutineListIntent.ClickRoutine -> updateEffect(RoutineListEffect.NavigateToRoutineDetail(intent.id))

            is RoutineListIntent.ClickDeleteRoutine -> deleteRoutine(intent.id)

            is RoutineListIntent.ClickBack -> updateEffect(RoutineListEffect.GoBack)

            is RoutineListIntent.ClickRetry -> observeRoutines()
        }
    }

    private fun observeRoutines() {
        loadJob?.cancel()
        updateState { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            routineRepository.observeRoutines()
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { routines ->
                    updateState { it.copy(isLoading = false, errorMessage = null, routines = routines) }
                }
        }
    }

    private fun dismissDialog() {
        // 만드는 중에는 닫지 않는다 — 닫아 둔 채로 실패하면 쓰던 이름을 되돌릴 자리가 없다.
        if (state.value.isCreating) return
        updateState { it.copy(isDialogVisible = false) }
    }

    private fun createRoutine() {
        val current = state.value
        if (current.isCreating) return
        val name = current.dialogName.trim()
        if (name.isEmpty()) {
            updateEffect(RoutineListEffect.ShowMessage(NAME_REQUIRED))
            return
        }
        updateState { it.copy(isCreating = true) }
        viewModelScope.launch {
            runCatching { routineRepository.addRoutine(name, current.dialogBodyPart) }
                .onSuccess { id ->
                    updateState { it.copy(isCreating = false, isDialogVisible = false) }
                    updateEffect(RoutineListEffect.NavigateToRoutineDetail(id))
                }
                .onFailure {
                    // 입력은 지우지 않는다 — 같은 자리에서 다시 만들 수 있어야 한다.
                    updateState { it.copy(isCreating = false) }
                    updateEffect(RoutineListEffect.ShowMessage(CREATE_ERROR))
                }
        }
    }

    private fun deleteRoutine(id: Long) {
        viewModelScope.launch {
            runCatching { routineRepository.deleteRoutine(id) }
                .onFailure { updateEffect(RoutineListEffect.ShowMessage(DELETE_ERROR)) }
        }
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
private const val DELETE_ERROR = "삭제하지 못했습니다"
private const val NAME_REQUIRED = "루틴 이름을 입력하세요"
private const val CREATE_ERROR = "루틴을 만들지 못했습니다"

/** 카드 제목 한 줄에 들어갈 길이로 묶어 둔다. */
private const val ROUTINE_NAME_MAX_LENGTH = 20
