package kr.sdbk.bodyplan.feature.my.impl.inbody

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunRequest
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunState
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisRunner
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class InbodyViewModel
@Inject
constructor(
    private val analysisRunner: AnalysisRunner,
    private val analysisResultRepository: AnalysisResultRepository,
    private val aiCredentialRepository: AiCredentialRepository,
) : BaseViewModel<InbodyState, InbodyIntent, InbodyEffect>(initialState = InbodyState()) {
    /** 직전에 본 작업 상태. 끝나는 "순간"을 알려면 이전 값이 있어야 한다. */
    private var lastRunState: AnalysisRunState? = null

    override suspend fun initializeData() {
        observeHistory()
        observeCredential()
        observeRun()
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

    /** 인바디는 대상 열쇠가 없어 빈 문자열 하나를 함께 쓴다. 한 번에 하나만 돈다. */
    private fun observeRun() {
        viewModelScope.launch {
            analysisRunner.observe(AnalysisKind.INBODY, INBODY_SCOPE).collect { run ->
                // 끝난 작업의 정보는 다음 분석까지 남아 있어, "지금 성공 상태"로 판단하면
                // 화면에 들어올 때마다 지난 성공이 되풀이돼 방금 고른 사진을 지운다.
                // 돌던 것이 끝나는 순간에만 놓는다.
                val justFinished = run?.state == AnalysisRunState.SUCCEEDED &&
                    lastRunState == AnalysisRunState.RUNNING
                lastRunState = run?.state
                updateState { current ->
                    current.copy(
                        run = run,
                        // 사진은 결과에 붙어 이력으로 남는다. 고르던 자리에 남길 이유가 없다.
                        pickedImageUri = if (justFinished) null else current.pickedImageUri,
                        selectedResultId = if (justFinished) null else current.selectedResultId,
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

    /** 지난 결과를 보는 동안 고르던 사진이 남아 있으면 무엇을 분석할지가 흐려진다. */
    private fun showHistory(id: Long) {
        if (state.value.history.none { it.id == id }) return
        updateState { it.copy(selectedResultId = id, pickedImageUri = null, errorMessage = null) }
    }

    /**
     * 작업은 화면 밖에서 돈다. 여기서 결과를 기다리지 않으므로 화면을 나가도 요청이 살아 있다.
     *
     * 성공하면 이력 흐름이 새 결과를 내려주므로 고르던 사진을 여기서 지우지 않는다 —
     * 화면을 나가 있는 동안 끝날 수 있어, 지우는 일은 결과가 들어온 것을 보고 한다.
     */
    private fun analyze() {
        val sourceUri = state.value.pickedImageUri ?: return
        if (!state.value.hasCredential) {
            updateState { it.copy(isTokenDialogVisible = true) }
            return
        }
        if (!state.value.canAnalyze) return

        viewModelScope.launch {
            updateState { it.copy(errorMessage = null) }
            analysisRunner.start(
                AnalysisRunRequest(
                    kind = AnalysisKind.INBODY,
                    scopeKey = INBODY_SCOPE,
                    periodLabel = "",
                    sourceUri = sourceUri,
                ),
            )
        }
    }
}

private const val HISTORY_LOAD_FAILED = "지난 분석을 불러오지 못했습니다"
private const val INBODY_SCOPE = ""
