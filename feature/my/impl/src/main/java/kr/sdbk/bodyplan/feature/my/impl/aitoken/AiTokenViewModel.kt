package kr.sdbk.bodyplan.feature.my.impl.aitoken

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiRequestFailedException
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class AiTokenViewModel
@Inject
constructor(
    private val aiCredentialRepository: AiCredentialRepository,
    private val aiAnalysisRepository: AiAnalysisRepository,
) : BaseViewModel<AiTokenState, AiTokenIntent, AiTokenEffect>(initialState = AiTokenState()) {
    private var connectJob: Job? = null

    override suspend fun initializeData() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            aiCredentialRepository.observeCredential().collect { credential ->
                updateState { it.copy(isLoading = false, connected = credential) }
            }
        }
    }

    override fun handleIntent(intent: AiTokenIntent) {
        when (intent) {
            is AiTokenIntent.ClickProvider ->
                updateState { it.copy(connectingProvider = intent.provider, input = "", errorMessage = null) }

            is AiTokenIntent.ChangeInput ->
                updateState { it.copy(input = intent.value, errorMessage = null) }

            is AiTokenIntent.ClickConnect -> connect()

            is AiTokenIntent.ClickCancelConnect -> cancelConnect()

            is AiTokenIntent.ClickDisconnect -> disconnect()

            is AiTokenIntent.ClickBack -> updateEffect(AiTokenEffect.GoBack)
        }
    }

    /**
     * 검증을 통과할 때만 저장한다.
     *
     * 저장부터 하면 쓸 수 없는 키가 연결된 것으로 남아, 분석할 때마다 실패한다.
     * 실패하면 넣은 키를 지우지 않는다 — 오타 하나 고치려고 다시 붙여넣게 하지 않는다.
     */
    private fun connect() {
        val current = state.value
        val provider = current.connectingProvider ?: return
        if (!current.canConnect) return

        val credential = AiCredential(provider, current.input.trim())
        updateState { it.copy(isConnecting = true, errorMessage = null) }
        connectJob = viewModelScope.launch {
            runCatching {
                aiAnalysisRepository.verifyCredential(credential)
                aiCredentialRepository.save(credential)
            }.onSuccess {
                // 저장된 키를 보는 흐름의 다음 방출을 기다리면 그 사이 화면이 제공자 목록으로 한 번 튄다.
                updateState {
                    it.copy(
                        isConnecting = false,
                        connected = credential,
                        connectingProvider = null,
                        input = "",
                        errorMessage = null,
                    )
                }
                updateEffect(AiTokenEffect.ShowMessage(CONNECTED))
            }.onFailure { failure ->
                updateState { it.copy(isConnecting = false, errorMessage = failure.toMessage()) }
            }
        }
    }

    /**
     * 돌고 있는 검증을 실제로 멈춘다.
     *
     * 화면만 되돌리면 검증이 통과하는 순간 저장까지 진행돼, 취소했는데 연동되는 일이 생긴다.
     */
    private fun cancelConnect() {
        connectJob?.cancel()
        connectJob = null
        updateState {
            it.copy(isConnecting = false, connectingProvider = null, input = "", errorMessage = null)
        }
    }

    private fun disconnect() {
        if (!state.value.canDisconnect) return
        viewModelScope.launch {
            runCatching { aiCredentialRepository.clear() }
                .onSuccess {
                    updateState { it.copy(connectingProvider = null, input = "", errorMessage = null) }
                    updateEffect(AiTokenEffect.ShowMessage(DISCONNECTED))
                }
                .onFailure { updateEffect(AiTokenEffect.ShowMessage(DISCONNECT_FAILED)) }
        }
    }

    private fun Throwable.toMessage(): String = when {
        this is AiUnauthorizedException -> INVALID_KEY
        this is AiRequestFailedException && reason != null -> "$CALL_FAILED: $reason"
        else -> CALL_FAILED
    }
}

private const val CONNECTED = "연결했습니다"
private const val DISCONNECTED = "연결을 해제했습니다"
private const val DISCONNECT_FAILED = "해제하지 못했습니다"
private const val INVALID_KEY = "키가 올바르지 않습니다"
private const val CALL_FAILED = "연결하지 못했습니다"
