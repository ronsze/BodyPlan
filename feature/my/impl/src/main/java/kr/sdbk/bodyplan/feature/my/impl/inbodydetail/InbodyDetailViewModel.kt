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
     * 요청이 실패해도 여기서 문구를 내지 않는다. 실패는 기록의 실패 사유로 남아 상세가 그대로 보여 주고,
     * 요청 자체가 던지는 경우는 연결이 사라진 때뿐이라 안내를 연다.
     */
    private fun analyze() {
        val current = state.value
        if (!current.canAnalyze) return
        if (!current.hasCredential) {
            updateState { it.copy(isTokenDialogVisible = true) }
            return
        }

        viewModelScope.launch {
            try {
                requestInbodyAnalysis(navKey.recordId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (missing: AiCredentialMissingException) {
                updateState { it.copy(isTokenDialogVisible = true) }
            } catch (ignored: Throwable) {
                // 실패 사유는 기록에 남는다.
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: InbodyDetailNavKey): InbodyDetailViewModel
    }
}
