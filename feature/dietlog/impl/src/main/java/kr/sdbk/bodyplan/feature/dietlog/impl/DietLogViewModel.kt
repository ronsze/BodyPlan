package kr.sdbk.bodyplan.feature.dietlog.impl

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.repository.DietLogRepository
import kr.sdbk.bodyplan.core.domain.usecase.IsEditableDateUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel
import kr.sdbk.bodyplan.feature.dietlog.api.DietLogNavKey

@HiltViewModel(assistedFactory = DietLogViewModel.Factory::class)
internal class DietLogViewModel
@AssistedInject
constructor(
    private val dietLogRepository: DietLogRepository,
    private val isEditableDate: IsEditableDateUseCase,
    @Assisted navKey: DietLogNavKey,
) : BaseViewModel<DietLogState, DietLogIntent, DietLogEffect>(
    initialState = DietLogState(date = LocalDate.ofEpochDay(navKey.dateEpochDay)),
) {
    private var loadJob: Job? = null

    override suspend fun initializeData() {
        // 자정을 넘겨도 화면 안에서 편집 가능 여부가 바뀌지 않도록 진입 시점에 한 번만 판정한다.
        updateState { it.copy(isEditable = isEditableDate(it.date)) }
        observeLog()
    }

    override fun handleIntent(intent: DietLogIntent) {
        when (intent) {
            is DietLogIntent.ClickAddEntry ->
                updateEffect(DietLogEffect.NavigateToEntryEdit(state.value.date, null))

            is DietLogIntent.ClickEntry ->
                updateEffect(DietLogEffect.NavigateToEntryEdit(state.value.date, intent.id))

            is DietLogIntent.ClickDeleteEntry -> deleteEntry(intent.id)

            is DietLogIntent.ClickBack -> updateEffect(DietLogEffect.GoBack)

            is DietLogIntent.ClickRetry -> observeLog()
        }
    }

    private fun observeLog() {
        loadJob?.cancel()
        updateState { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            dietLogRepository.observeLog(state.value.date)
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { log ->
                    updateState { it.copy(isLoading = false, errorMessage = null, entries = log.entries) }
                }
        }
    }

    private fun deleteEntry(id: Long) {
        viewModelScope.launch {
            runCatching { dietLogRepository.deleteEntry(id) }
                .onFailure { updateEffect(DietLogEffect.ShowMessage(DELETE_ERROR)) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: DietLogNavKey): DietLogViewModel
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
private const val DELETE_ERROR = "삭제하지 못했습니다"
