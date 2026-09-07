package kr.sdbk.bodyplan.feature.dietlog.impl.analysis

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.usecase.AiCredentialMissingException
import kr.sdbk.bodyplan.core.domain.usecase.AnalyzeDietUseCase
import kr.sdbk.bodyplan.core.domain.usecase.NoDailyAnalysisException
import kr.sdbk.bodyplan.core.domain.usecase.NoRecordToAnalyzeException
import kr.sdbk.bodyplan.core.ui.components.AnalysisPeriodInfo
import kr.sdbk.bodyplan.core.ui.components.analysisPeriodInfo
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel
import kr.sdbk.bodyplan.feature.dietlog.api.DietAnalysisNavKey

@HiltViewModel(assistedFactory = DietAnalysisViewModel.Factory::class)
internal class DietAnalysisViewModel
@AssistedInject
constructor(
    private val analyzeDiet: AnalyzeDietUseCase,
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

    private fun observeCredential() {
        viewModelScope.launch {
            aiCredentialRepository.observeCredential().collect { credential ->
                updateState { it.copy(hasCredential = credential != null) }
            }
        }
    }

    /**
     * 토큰이 없으면 부르지 않고 팝업만 띄운다. 사용자의 키로 호출되므로 헛되이 부르지 않는다.
     * 실패해도 [DietAnalysisState.result]는 건드리지 않는다 — 이전 결과가 남는 편이 빈 화면보다 낫다.
     */
    private fun analyze() {
        if (!state.value.hasCredential) {
            updateState { it.copy(isTokenDialogVisible = true) }
            return
        }
        if (!state.value.canAnalyze) return

        viewModelScope.launch {
            updateState { it.copy(isAnalyzing = true, errorMessage = null) }
            runCatching {
                analyzeDiet(
                    kind = period.kind,
                    scopeKey = period.scopeKey,
                    periodLabel = period.label,
                    from = period.from,
                    to = period.to,
                )
            }.onSuccess {
                // 저장된 결과를 보는 흐름이 이미 돌고 있어 여기서 result를 넣지 않는다.
                updateState { it.copy(isAnalyzing = false) }
            }.onFailure { failure ->
                updateState { it.copy(isAnalyzing = false, errorMessage = failure.toMessage()) }
                if (failure is AiCredentialMissingException) {
                    updateState { it.copy(isTokenDialogVisible = true) }
                }
            }
        }
    }

    private fun Throwable.toMessage(): String = when (this) {
        is NoRecordToAnalyzeException -> NO_RECORD
        is NoDailyAnalysisException -> NO_DAILY_ANALYSIS
        is AiCredentialMissingException -> NO_CREDENTIAL
        is AiUnauthorizedException -> INVALID_KEY
        else -> FAILED
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: DietAnalysisNavKey): DietAnalysisViewModel
    }
}

private fun periodInfoOf(navKey: DietAnalysisNavKey): AnalysisPeriodInfo =
    analysisPeriodInfo(navKey.period.analysisKind, LocalDate.ofEpochDay(navKey.dateEpochDay))

private const val NO_RECORD = "분석할 기록이 없습니다"
private const val NO_DAILY_ANALYSIS = "먼저 날짜별로 분석해 주세요"
private const val NO_CREDENTIAL = "AI 연결이 필요해요"
private const val INVALID_KEY = "키가 올바르지 않습니다"
private const val FAILED = "분석하지 못했습니다"
