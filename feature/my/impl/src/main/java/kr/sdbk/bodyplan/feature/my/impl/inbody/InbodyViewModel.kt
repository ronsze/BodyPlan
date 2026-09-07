package kr.sdbk.bodyplan.feature.my.impl.inbody

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.AiRequestFailedException
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.usecase.AiCredentialMissingException
import kr.sdbk.bodyplan.core.domain.usecase.AnalyzeInbodyUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class InbodyViewModel
@Inject
constructor(
    private val analyzeInbody: AnalyzeInbodyUseCase,
    private val analysisResultRepository: AnalysisResultRepository,
    private val aiCredentialRepository: AiCredentialRepository,
) : BaseViewModel<InbodyState, InbodyIntent, InbodyEffect>(initialState = InbodyState()) {
    override suspend fun initializeData() {
        observeHistory()
        observeCredential()
    }

    override fun handleIntent(intent: InbodyIntent) {
        when (intent) {
            is InbodyIntent.PickImage ->
                updateState { it.copy(pickedImageUri = intent.uri, errorMessage = null) }

            is InbodyIntent.ClickAnalyze -> analyze()

            is InbodyIntent.ClickHistory -> showHistory(intent.id)

            is InbodyIntent.ConfirmTokenDialog -> {
                updateState { it.copy(isTokenDialogVisible = false) }
                updateEffect(InbodyEffect.NavigateToAiToken)
            }

            is InbodyIntent.DismissTokenDialog ->
                updateState { it.copy(isTokenDialogVisible = false) }
        }
    }

    private fun observeHistory() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            analysisResultRepository.observeHistory(AnalysisKind.INBODY)
                .catch { updateState { it.copy(isLoading = false, errorMessage = HISTORY_LOAD_FAILED) } }
                .collect { history ->
                    updateState { it.copy(isLoading = false, history = history) }
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

    /** 지난 결과를 보는 동안 고르던 사진이 남아 있으면 무엇을 분석할지가 흐려진다. */
    private fun showHistory(id: Long) {
        if (state.value.history.none { it.id == id }) return
        updateState { it.copy(selectedResultId = id, pickedImageUri = null, errorMessage = null) }
    }

    private fun analyze() {
        val sourceUri = state.value.pickedImageUri ?: return
        if (!state.value.hasCredential) {
            updateState { it.copy(isTokenDialogVisible = true) }
            return
        }
        if (state.value.isAnalyzing) return

        viewModelScope.launch {
            updateState { it.copy(isAnalyzing = true, errorMessage = null) }
            try {
                analyzeInbody(sourceUri)
                // 사진은 결과에 붙어 이력으로 남는다. 고르던 자리에 남길 이유가 없다.
                // 고른 이력을 놓아 새 결과가 들어오면 그것이 보이게 한다.
                updateState { it.copy(isAnalyzing = false, pickedImageUri = null, selectedResultId = null) }
            } catch (cancellation: CancellationException) {
                // 화면을 벗어난 것이지 분석이 실패한 것이 아니다. 실패 문구를 남기지 않는다.
                throw cancellation
            } catch (failure: Throwable) {
                // 고른 사진은 남긴다. 다시 누를 수 있어야 한다.
                updateState { it.copy(isAnalyzing = false, errorMessage = failure.toMessage()) }
                if (failure is AiCredentialMissingException) {
                    updateState { it.copy(isTokenDialogVisible = true) }
                }
            }
        }
    }

    private fun Throwable.toMessage(): String = when (this) {
        is AiCredentialMissingException -> NO_CREDENTIAL
        is AiUnauthorizedException -> INVALID_KEY
        is AiRequestFailedException -> reason?.let { "$FAILED: $it" } ?: FAILED
        else -> FAILED
    }
}

private const val HISTORY_LOAD_FAILED = "지난 분석을 불러오지 못했습니다"
private const val NO_CREDENTIAL = "AI 연결이 필요해요"
private const val INVALID_KEY = "키가 올바르지 않습니다"
private const val FAILED = "분석하지 못했습니다"
