package kr.sdbk.bodyplan.feature.home.impl.home

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.usecase.GetProgressSummaryUseCase
import kr.sdbk.bodyplan.core.domain.usecase.GetWeeklyGoalProgressUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class HomeViewModel
@Inject
constructor(
    private val getProgressSummary: GetProgressSummaryUseCase,
    private val getWeeklyGoalProgress: GetWeeklyGoalProgressUseCase,
) : BaseViewModel<HomeState, HomeIntent, HomeEffect>(initialState = HomeState()) {
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
            // 실패와 재시도 경로가 하나여야 화면 분기가 갈라지지 않는다.
            combine(getProgressSummary(), getWeeklyGoalProgress()) { summary, weeklyGoal -> summary to weeklyGoal }
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { (summary, weeklyGoal) ->
                    updateState {
                        it.copy(isLoading = false, errorMessage = null, summary = summary, weeklyGoal = weeklyGoal)
                    }
                }
        }
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
