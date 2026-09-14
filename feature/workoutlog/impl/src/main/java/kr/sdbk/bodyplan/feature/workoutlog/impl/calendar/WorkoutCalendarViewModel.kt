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
import kr.sdbk.bodyplan.core.domain.usecase.GetWeeklyBodyPartVolumeUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class WorkoutCalendarViewModel
@Inject
constructor(
    private val getMonthlyDayStatus: GetMonthlyDayStatusUseCase,
    private val getWeeklyBodyPartVolume: GetWeeklyBodyPartVolumeUseCase,
    clock: Clock,
) : BaseViewModel<WorkoutCalendarState, WorkoutCalendarIntent, WorkoutCalendarEffect>(
    initialState = WorkoutCalendarState(
        yearMonth = YearMonth.now(clock),
        today = LocalDate.now(clock),
    ),
) {
    private var loadJob: Job? = null
    private var weeklyVolumeJob: Job? = null

    override suspend fun initializeData() {
        observeMonth()
        observeWeeklyVolume()
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

            is WorkoutCalendarIntent.ToggleCalendarExpansion -> toggleCalendarExpansion()

            is WorkoutCalendarIntent.ClickRetry -> observeMonth()

            is WorkoutCalendarIntent.ClickRetryWeeklyVolume -> observeWeeklyVolume()
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
        loadJob = viewModelScope.launch {
            getMonthlyDayStatus(state.value.yearMonth)
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { statuses ->
                    updateState { it.copy(isLoading = false, errorMessage = null, dayStatuses = statuses) }
                }
        }
    }

    /** 이번 주 볼륨은 보고 있는 달과 무관하다. 달 조회가 실패해도 카드는 살고, 카드가 실패해도 캘린더는 산다. */
    private fun observeWeeklyVolume() {
        weeklyVolumeJob?.cancel()
        updateState { it.copy(weeklyVolumeErrorMessage = null) }
        weeklyVolumeJob = viewModelScope.launch {
            getWeeklyBodyPartVolume()
                .catch { updateState { it.copy(weeklyVolumeErrorMessage = LOAD_ERROR) } }
                .collect { volumes -> updateState { it.copy(weeklyVolumes = volumes) } }
        }
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
