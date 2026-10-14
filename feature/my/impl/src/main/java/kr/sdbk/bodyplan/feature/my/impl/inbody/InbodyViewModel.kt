package kr.sdbk.bodyplan.feature.my.impl.inbody

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.InbodyEntryStatus
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.usecase.AiCredentialMissingException
import kr.sdbk.bodyplan.core.domain.usecase.BuildInbodyTrendUseCase
import kr.sdbk.bodyplan.core.domain.usecase.ObserveInbodyEntriesUseCase
import kr.sdbk.bodyplan.core.domain.usecase.RequestInbodyAnalysisUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class InbodyViewModel
@Inject
constructor(
    private val observeInbodyEntries: ObserveInbodyEntriesUseCase,
    private val buildInbodyTrend: BuildInbodyTrendUseCase,
    private val requestInbodyAnalysis: RequestInbodyAnalysisUseCase,
    private val aiCredentialRepository: AiCredentialRepository,
    private val clock: Clock,
) : BaseViewModel<InbodyState, InbodyIntent, InbodyEffect>(initialState = InbodyState()) {
    override suspend fun initializeData() {
        observeEntries()
        observeCredential()
    }

    override fun handleIntent(intent: InbodyIntent) {
        when (intent) {
            is InbodyIntent.SelectPeriod -> updateState { current ->
                current.copy(
                    period = intent.period,
                    trend = buildInbodyTrend(current.entries, intent.period, today()),
                )
            }

            is InbodyIntent.ClickAnalyze -> analyze(intent.recordId)

            is InbodyIntent.ConfirmTokenDialog -> {
                updateState { it.copy(isTokenDialogVisible = false) }
                updateEffect(InbodyEffect.NavigateToAiToken)
            }

            is InbodyIntent.DismissTokenDialog -> updateState { it.copy(isTokenDialogVisible = false) }
        }
    }

    private fun observeEntries() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            observeInbodyEntries()
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_FAILED) } }
                .collect { entries ->
                    updateState { current ->
                        current.copy(
                            isLoading = false,
                            entries = entries,
                            trend = buildInbodyTrend(entries, current.period, today()),
                        )
                    }
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

    /** 오늘은 읽을 때마다 시계에서 구한다. 화면을 오래 켜 두고 자정을 넘겨도 기간이 따라간다. */
    private fun today(): LocalDate = LocalDate.now(clock)

    private fun analyze(recordId: Long) {
        val entry = state.value.entries.firstOrNull { it.record.id == recordId } ?: return
        if (entry.status == InbodyEntryStatus.RUNNING) return
        if (!state.value.hasCredential) {
            updateState { it.copy(isTokenDialogVisible = true) }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(errorMessage = null) }
            try {
                requestInbodyAnalysis(recordId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (missing: AiCredentialMissingException) {
                // 연결이 그 사이에 사라진 경우다. 안내를 열어 다시 연결하게 한다.
                updateState { it.copy(isTokenDialogVisible = true) }
            } catch (failure: Throwable) {
                updateState { it.copy(errorMessage = ANALYZE_FAILED) }
            }
        }
    }
}

private const val LOAD_FAILED = "인바디 기록을 불러오지 못했습니다"
private const val ANALYZE_FAILED = "분석을 요청하지 못했습니다"
