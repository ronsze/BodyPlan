package kr.sdbk.bodyplan.feature.workoutlog.impl.routinedetail

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.repository.RoutineRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel
import kr.sdbk.bodyplan.feature.workoutlog.api.RoutineDetailNavKey

@HiltViewModel(assistedFactory = RoutineDetailViewModel.Factory::class)
internal class RoutineDetailViewModel
@AssistedInject
constructor(
    private val routineRepository: RoutineRepository,
    @Assisted navKey: RoutineDetailNavKey,
) : BaseViewModel<RoutineDetailState, RoutineDetailIntent, RoutineDetailEffect>(
    initialState = RoutineDetailState(routineId = navKey.routineId),
) {
    private var loadJob: Job? = null

    override suspend fun initializeData() {
        observeRoutine()
    }

    override fun handleIntent(intent: RoutineDetailIntent) {
        when (intent) {
            is RoutineDetailIntent.ClickAddEntry ->
                updateEffect(RoutineDetailEffect.NavigateToEntryEdit(state.value.routineId, null))

            is RoutineDetailIntent.ClickEntry ->
                updateEffect(RoutineDetailEffect.NavigateToEntryEdit(state.value.routineId, intent.id))

            is RoutineDetailIntent.ClickDeleteEntry -> deleteEntry(intent.id)

            is RoutineDetailIntent.ClickBack -> updateEffect(RoutineDetailEffect.GoBack)

            is RoutineDetailIntent.ClickRetry -> observeRoutine()
        }
    }

    private fun observeRoutine() {
        loadJob?.cancel()
        updateState { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            routineRepository.observeRoutine(state.value.routineId)
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { routine ->
                    // 없는 루틴은 조회 실패와 같이 다룬다 — 화면이 보여 줄 것이 없다.
                    updateState {
                        it.copy(
                            isLoading = false,
                            routine = routine,
                            errorMessage = if (routine == null) LOAD_ERROR else null,
                        )
                    }
                }
        }
    }

    private fun deleteEntry(id: Long) {
        viewModelScope.launch {
            runCatching { routineRepository.deleteEntry(id) }
                .onFailure { updateEffect(RoutineDetailEffect.ShowMessage(DELETE_ERROR)) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: RoutineDetailNavKey): RoutineDetailViewModel
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
private const val DELETE_ERROR = "삭제하지 못했습니다"
