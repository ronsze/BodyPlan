package kr.sdbk.bodyplan.feature.dietlog.impl

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.repository.DietLogRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class DietCalendarViewModel
@Inject
constructor(
    private val dietLogRepository: DietLogRepository,
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

            is DietCalendarIntent.ClickRetry -> observeMonth()
        }
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
            dietLogRepository.observeFirstImageInRange(yearMonth.atDay(1), yearMonth.atEndOfMonth())
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { images ->
                    updateState { it.copy(isLoading = false, errorMessage = null, imagesByDate = images) }
                }
        }
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
