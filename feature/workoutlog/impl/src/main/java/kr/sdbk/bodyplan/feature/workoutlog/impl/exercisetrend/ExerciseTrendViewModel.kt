package kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.usecase.GetExerciseTrendUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class ExerciseTrendViewModel
@Inject
constructor(private val getExerciseTrend: GetExerciseTrendUseCase) :
    BaseViewModel<ExerciseTrendState, ExerciseTrendIntent, ExerciseTrendEffect>(
        initialState = ExerciseTrendState(),
    ) {
    private var loadJob: Job? = null

    override suspend fun initializeData() {
        observeTrend()
    }

    override fun handleIntent(intent: ExerciseTrendIntent) {
        when (intent) {
            is ExerciseTrendIntent.SelectBodyPart -> selectBodyPart(intent.bodyPart)
            is ExerciseTrendIntent.SelectExercise -> selectExercise(intent.id)
            is ExerciseTrendIntent.ClickBack -> updateEffect(ExerciseTrendEffect.GoBack)
            is ExerciseTrendIntent.ClickRetry -> observeTrend()
        }
    }

    /**
     * 부위를 바꾸면 고른 종목이 풀린다. 구독도 함께 갈아끼운다 —
     * 이전 종목으로 열린 스트림이 살아 있으면 다음 방출에 그 추이가 되살아난다.
     */
    private fun selectBodyPart(bodyPart: BodyPart) {
        if (state.value.selectedBodyPart == bodyPart) return
        updateState { it.copy(selectedBodyPart = bodyPart, selectedExerciseId = null, trend = null) }
        observeTrend()
    }

    /** 이전 차트를 지우지 않는다 — 새 값이 올 때까지 빈 자리를 보이지 않게 한다. */
    private fun selectExercise(id: Long) {
        if (state.value.selectedExerciseId == id) return
        updateState { it.copy(selectedExerciseId = id) }
        observeTrend()
    }

    private fun observeTrend() {
        loadJob?.cancel()
        updateState { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            getExerciseTrend(state.value.selectedExerciseId)
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { result ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            exercises = result.exercises,
                            trend = result.trend,
                        )
                    }
                }
        }
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
