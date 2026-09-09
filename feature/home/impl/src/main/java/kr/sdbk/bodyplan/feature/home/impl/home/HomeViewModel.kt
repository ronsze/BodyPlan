package kr.sdbk.bodyplan.feature.home.impl.home

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.usecase.GetProgressSummaryUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class HomeViewModel
@Inject
constructor(private val getProgressSummary: GetProgressSummaryUseCase) :
    BaseViewModel<HomeState, HomeIntent, HomeEffect>(initialState = HomeState()) {
    private var loadJob: Job? = null

    override suspend fun initializeData() {
        observeSummary()
    }

    override fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.ClickRetry -> observeSummary()
        }
    }

    private fun observeSummary() {
        loadJob?.cancel()
        updateState { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            getProgressSummary()
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { summary ->
                    updateState { it.copy(isLoading = false, errorMessage = null, summary = summary) }
                }
        }
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
