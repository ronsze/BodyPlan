package kr.sdbk.bodyplan.feature.my.impl.aitoken

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AiRequestFailedException
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.core.domain.model.OnDeviceDownload
import kr.sdbk.bodyplan.core.domain.model.OnDeviceModelStatus
import kr.sdbk.bodyplan.core.domain.model.requiresToken
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.OnDeviceAiRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class AiTokenViewModel
@Inject
constructor(
    private val aiCredentialRepository: AiCredentialRepository,
    private val aiAnalysisRepository: AiAnalysisRepository,
    private val onDeviceAiRepository: OnDeviceAiRepository,
) : BaseViewModel<AiTokenState, AiTokenIntent, AiTokenEffect>(initialState = AiTokenState()) {
    private var connectJob: Job? = null

    override suspend fun initializeData() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            // 지원 여부를 못 알아내면 미지원으로 본다. 선택지가 안 보이는 쪽이 눌렀다가 실패하는 쪽보다 낫다.
            val status = runCatching { onDeviceAiRepository.getStatus() }.getOrDefault(OnDeviceModelStatus.UNSUPPORTED)
            updateState { it.copy(onDeviceStatus = status) }
        }
        viewModelScope.launch {
            aiCredentialRepository.observeCredential().collect { credential ->
                updateState { it.copy(isLoading = false, connected = credential) }
            }
        }
    }

    override fun handleIntent(intent: AiTokenIntent) {
        when (intent) {
            is AiTokenIntent.ClickProvider -> clickProvider(intent.provider)

            is AiTokenIntent.ChangeInput ->
                updateState { it.copy(input = intent.value, errorMessage = null) }

            is AiTokenIntent.ClickConnect -> connect()

            is AiTokenIntent.ClickDownloadModel -> downloadModel()

            is AiTokenIntent.ClickCancelConnect -> cancelConnect()

            is AiTokenIntent.ClickDisconnect -> disconnect()

            is AiTokenIntent.ClickBack -> updateEffect(AiTokenEffect.GoBack)
        }
    }

    /**
     * 온디바이스는 모델이 이미 있으면 넣을 것이 없어 바로 연결한다. 없으면 내려받기 화면으로 간다.
     *
     * 바로 연결할 때는 연결 중 화면을 거치지 않는다 — 그 화면은 내려받기 진행을 보이는 곳이라
     * 검증 중에 뜨면 내려받는 것처럼 보인다. 실패했을 때만 그 화면으로 옮겨 사유를 보인다.
     */
    private fun clickProvider(provider: AiProvider) {
        // 바로 연결이 도는 동안 다른 제공자를 고르면 두 연결이 한 화면을 다툰다.
        if (state.value.isConnecting) return
        val connectsImmediately = !provider.requiresToken && state.value.onDeviceStatus == OnDeviceModelStatus.READY
        updateState {
            it.copy(connectingProvider = provider.takeUnless { connectsImmediately }, input = "", errorMessage = null)
        }
        if (connectsImmediately) saveAfterVerify(AiCredential.onDevice())
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

        val credential = if (provider.requiresToken) {
            AiCredential(
                provider,
                current.input.trim(),
            )
        } else {
            AiCredential.onDevice()
        }
        saveAfterVerify(credential)
    }

    private fun saveAfterVerify(credential: AiCredential) {
        connectJob?.cancel()
        updateState { it.copy(isConnecting = true, errorMessage = null) }
        connectJob = viewModelScope.launch { verifyAndSave(credential) }
    }

    private suspend fun verifyAndSave(credential: AiCredential) {
        runCatching {
            aiAnalysisRepository.verifyCredential(credential)
            aiCredentialRepository.save(credential)
        }.onSuccess {
            // 저장된 키를 보는 흐름의 다음 방출을 기다리면 그 사이 화면이 제공자 목록으로 한 번 튄다.
            // 온디바이스가 연결됐다는 것은 모델이 준비됐다는 뜻이다. 해제하고 다시 누르면 내려받기를 건너뛰어야 한다.
            updateState {
                it.copy(
                    isConnecting = false,
                    connected = credential,
                    connectingProvider = null,
                    input = "",
                    errorMessage = null,
                    downloadTotalBytes = 0,
                    downloadedBytes = 0,
                    onDeviceStatus = if (credential.provider.requiresToken) {
                        it.onDeviceStatus
                    } else {
                        OnDeviceModelStatus.READY
                    },
                )
            }
            updateEffect(AiTokenEffect.ShowMessage(CONNECTED))
        }.onFailure { failure ->
            // 취소는 실패가 아니다. cancelConnect가 이미 되돌린 화면에 사유를 덧쓰지 않는다.
            if (failure is CancellationException) throw failure
            updateState {
                it.copy(
                    isConnecting = false,
                    errorMessage = failure.toMessage(),
                    connectingProvider = it.connectingProvider ?: credential.provider,
                )
            }
        }
    }

    /** 내려받기가 끝나야 연결이다. 끝나기 전에 저장하면 준비 안 된 모델이 연결된 것으로 남는다. */
    private fun downloadModel() {
        val current = state.value
        if (current.connectingProvider != AiProvider.ON_DEVICE || !current.canConnect) return

        // 실패한 뒤 다시 누르면 이전 수집이 살아 있을 수 있다. 둘이 같은 상태를 갱신하지 않게 먼저 끊는다.
        connectJob?.cancel()
        updateState { it.copy(isConnecting = true, errorMessage = null, downloadTotalBytes = 0, downloadedBytes = 0) }
        connectJob = viewModelScope.launch {
            runCatching {
                onDeviceAiRepository.download().collect { progress ->
                    when (progress) {
                        is OnDeviceDownload.Started -> updateState { it.copy(downloadTotalBytes = progress.totalBytes) }

                        is OnDeviceDownload.Progress -> updateState {
                            it.copy(downloadedBytes = progress.downloadedBytes)
                        }

                        is OnDeviceDownload.Completed -> verifyAndSave(AiCredential.onDevice())

                        is OnDeviceDownload.Failed ->
                            updateState { it.copy(isConnecting = false, errorMessage = progress.reason) }
                    }
                }
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
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
            it.copy(
                isConnecting = false,
                connectingProvider = null,
                input = "",
                errorMessage = null,
                downloadTotalBytes = 0,
                downloadedBytes = 0,
            )
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
