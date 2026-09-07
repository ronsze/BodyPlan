package kr.sdbk.bodyplan.feature.dietlog.impl.analysis

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunRequest
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisRunner
import kr.sdbk.bodyplan.core.ui.components.AnalysisPeriodInfo
import kr.sdbk.bodyplan.core.ui.components.analysisPeriodInfo
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel
import kr.sdbk.bodyplan.feature.dietlog.api.DietAnalysisNavKey

@HiltViewModel(assistedFactory = DietAnalysisViewModel.Factory::class)
internal class DietAnalysisViewModel
@AssistedInject
constructor(
    private val analysisRunner: AnalysisRunner,
    private val analysisResultRepository: AnalysisResultRepository,
    private val aiCredentialRepository: AiCredentialRepository,
    @Assisted navKey: DietAnalysisNavKey,
) : BaseViewModel<DietAnalysisState, DietAnalysisIntent, DietAnalysisEffect>(
    initialState = DietAnalysisState(periodLabel = periodInfoOf(navKey).label),
) {
    private val period = periodInfoOf(navKey)

    override suspend fun initializeData() {
        observeSavedResult()
        observeCredential()
        observeRun()
    }

    override fun handleIntent(intent: DietAnalysisIntent) {
        when (intent) {
            is DietAnalysisIntent.ClickAnalyze -> analyze()

            is DietAnalysisIntent.ClickBack -> updateEffect(DietAnalysisEffect.GoBack)

            is DietAnalysisIntent.ConfirmTokenDialog -> {
                updateState { it.copy(isTokenDialogVisible = false) }
                updateEffect(DietAnalysisEffect.NavigateToAiToken)
            }

            is DietAnalysisIntent.DismissTokenDialog ->
                updateState { it.copy(isTokenDialogVisible = false) }
        }
    }

    private fun observeSavedResult() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            analysisResultRepository.observeLatest(period.kind, period.scopeKey)
                .catch { updateState { it.copy(isLoading = false) } }
                .collect { saved -> updateState { it.copy(isLoading = false, result = saved) } }
        }
    }

    /** 화면 밖에서 도는 작업을 본다. 화면을 나갔다 와도 여기서 다시 붙는다. */
    private fun observeRun() {
        viewModelScope.launch {
            analysisRunner.observe(period.kind, period.scopeKey).collect { run ->
                updateState { it.copy(run = run) }
            }
        }
    }

    private fun observeCredential() {
        viewModelScope.launch {
            aiCredentialRepository.observeCredential().collect { credential ->
                updateState { it.copy(hasCredential = credential != null) }
            }
        }
    }

    /**
     * 토큰이 없으면 넣지 않고 팝업만 띄운다. 사용자의 키로 호출되므로 헛되이 부르지 않는다.
     *
     * 작업은 화면 밖에서 돈다. 여기서 결과를 기다리지 않으므로 화면을 나가도 요청이 살아 있다.
     */
    private fun analyze() {
        if (!state.value.hasCredential) {
            updateState { it.copy(isTokenDialogVisible = true) }
            return
        }
        if (!state.value.canAnalyze) return

        viewModelScope.launch {
            updateState { it.copy(errorMessage = null) }
            analysisRunner.start(
                AnalysisRunRequest(
                    kind = period.kind,
                    scopeKey = period.scopeKey,
                    periodLabel = period.label,
                    from = period.from,
                    to = period.to,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: DietAnalysisNavKey): DietAnalysisViewModel
    }
}

private fun periodInfoOf(navKey: DietAnalysisNavKey): AnalysisPeriodInfo =
    analysisPeriodInfo(navKey.period.analysisKind, LocalDate.ofEpochDay(navKey.dateEpochDay))
