package kr.sdbk.bodyplan.feature.workoutlog.impl.calendar

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.usecase.GetMonthlyDayStatusUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class WorkoutCalendarViewModel
@Inject
constructor(
    private val getMonthlyDayStatus: GetMonthlyDayStatusUseCase,
    clock: Clock,
) : BaseViewModel<WorkoutCalendarState, WorkoutCalendarIntent, WorkoutCalendarEffect>(
    initialState = WorkoutCalendarState(
        yearMonth = YearMonth.now(clock),
        today = LocalDate.now(clock),
    ),
) {
    private var loadJob: Job? = null

    override suspend fun initializeData() {
        observeMonth()
    }

    override fun handleIntent(intent: WorkoutCalendarIntent) {
        when (intent) {
            is WorkoutCalendarIntent.ChangeMonth -> changeMonth(intent.yearMonth)

            is WorkoutCalendarIntent.ClickDate ->
                updateEffect(WorkoutCalendarEffect.NavigateToLog(intent.date))

            is WorkoutCalendarIntent.ClickManageExercise ->
                updateEffect(WorkoutCalendarEffect.NavigateToExerciseManage)

            is WorkoutCalendarIntent.ClickExerciseTrend ->
                updateEffect(WorkoutCalendarEffect.NavigateToExerciseTrend)

            is WorkoutCalendarIntent.ClickRetry -> observeMonth()
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
        loadJob = viewModelScope.launch {
            getMonthlyDayStatus(state.value.yearMonth)
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { statuses ->
                    updateState { it.copy(isLoading = false, errorMessage = null, dayStatuses = statuses) }
                }
        }
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
