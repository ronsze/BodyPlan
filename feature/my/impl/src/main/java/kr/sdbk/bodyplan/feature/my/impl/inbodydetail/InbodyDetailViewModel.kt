package kr.sdbk.bodyplan.feature.my.impl.inbodydetail

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.usecase.AiCredentialMissingException
import kr.sdbk.bodyplan.core.domain.usecase.BuildInbodyDetailUseCase
import kr.sdbk.bodyplan.core.domain.usecase.ObserveInbodyEntriesUseCase
import kr.sdbk.bodyplan.core.domain.usecase.RequestInbodyAnalysisUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel
import kr.sdbk.bodyplan.feature.my.api.InbodyDetailNavKey

@HiltViewModel(assistedFactory = InbodyDetailViewModel.Factory::class)
internal class InbodyDetailViewModel
@AssistedInject
constructor(
    private val observeInbodyEntries: ObserveInbodyEntriesUseCase,
    private val buildInbodyDetail: BuildInbodyDetailUseCase,
    private val requestInbodyAnalysis: RequestInbodyAnalysisUseCase,
    private val aiCredentialRepository: AiCredentialRepository,
    @Assisted private val navKey: InbodyDetailNavKey,
) : BaseViewModel<InbodyDetailState, InbodyDetailIntent, InbodyDetailEffect>(initialState = InbodyDetailState()) {
    override suspend fun initializeData() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            observeInbodyEntries()
                .catch { updateState { it.copy(isLoading = false) } }
                .collect { entries ->
                    updateState {
                        it.copy(isLoading = false, detail = buildInbodyDetail(entries, navKey.recordId))
                    }
                }
        }
        viewModelScope.launch {
            aiCredentialRepository.observeCredential().collect { credential ->
                updateState { it.copy(hasCredential = credential != null) }
            }
        }
    }

    override fun handleIntent(intent: InbodyDetailIntent) {
        when (intent) {
            is InbodyDetailIntent.ClickAnalyze -> analyze()

            is InbodyDetailIntent.ClickImage -> updateState { it.copy(isImageViewerVisible = true) }

            is InbodyDetailIntent.DismissImageViewer -> updateState { it.copy(isImageViewerVisible = false) }

            is InbodyDetailIntent.ConfirmTokenDialog -> {
                updateState { it.copy(isTokenDialogVisible = false) }
                updateEffect(InbodyDetailEffect.NavigateToAiToken)
            }

            is InbodyDetailIntent.DismissTokenDialog -> updateState { it.copy(isTokenDialogVisible = false) }
        }
    }

    /**
     * 요청 자체가 실패하면(연결이 사라진 경우는 안내, 그 밖은 문구) 작업이 돌지 않아 기록에는 아무것도
     * 남지 않는다. 분석이 돌다 실패한 것은 기록의 실패 사유로 상세가 그대로 보여 준다.
     */
    private fun analyze() {
        val current = state.value
        if (!current.canAnalyze) return
        if (!current.hasCredential) {
            updateState { it.copy(isTokenDialogVisible = true) }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(errorMessage = null) }
            try {
                requestInbodyAnalysis(navKey.recordId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (missing: AiCredentialMissingException) {
                updateState { it.copy(isTokenDialogVisible = true) }
            } catch (failure: Throwable) {
                updateState { it.copy(errorMessage = ANALYZE_FAILED) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: InbodyDetailNavKey): InbodyDetailViewModel
    }
}

private const val ANALYZE_FAILED = "분석을 요청하지 못했습니다"
