package kr.sdbk.bodyplan.feature.dietlog.impl.calendar

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.usecase.GetMonthlyDietStatusUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class DietCalendarViewModel
@Inject
constructor(
    private val getMonthlyDietStatus: GetMonthlyDietStatusUseCase,
    clock: Clock,
) : BaseViewModel<DietCalendarState, DietCalendarIntent, DietCalendarEffect>(
    initialState = DietCalendarState(
        yearMonth = YearMonth.now(clock),
        today = LocalDate.now(clock),
    ),
) {
    private var loadJob: Job? = null

    override suspend fun initializeData() {
        observeMonth()
    }

    override fun handleIntent(intent: DietCalendarIntent) {
        when (intent) {
            is DietCalendarIntent.ChangeMonth -> changeMonth(intent.yearMonth)

            is DietCalendarIntent.ClickDate ->
                updateEffect(DietCalendarEffect.NavigateToLog(intent.date))

            is DietCalendarIntent.ToggleCalendarExpansion -> toggleCalendarExpansion()

            is DietCalendarIntent.ClickRetry -> observeMonth()
        }
    }

    /** 접으면 이번 주만 남으므로, 다른 달을 보고 있었다면 이번 달로 되돌린다. */
    private fun toggleCalendarExpansion() {
        val isExpanding = !state.value.isCalendarExpanded
        updateState { it.copy(isCalendarExpanded = isExpanding) }
        if (!isExpanding) changeMonth(YearMonth.from(state.value.today))
    }

    private fun changeMonth(yearMonth: YearMonth) {
        if (yearMonth == state.value.yearMonth) return
        updateState { it.copy(yearMonth = yearMonth) }
        observeMonth()
    }

    private fun observeMonth() {
        loadJob?.cancel()
        updateState { it.copy(isLoading = true, errorMessage = null) }
        val yearMonth = state.value.yearMonth
        loadJob = viewModelScope.launch {
            getMonthlyDietStatus(yearMonth)
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { statuses ->
                    updateState { it.copy(isLoading = false, errorMessage = null, dayStatuses = statuses) }
                }
        }
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
