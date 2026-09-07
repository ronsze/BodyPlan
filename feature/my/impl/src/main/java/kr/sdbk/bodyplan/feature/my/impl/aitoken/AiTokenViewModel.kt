package kr.sdbk.bodyplan.feature.my.impl.aitoken

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.AiCredential
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
    override suspend fun initializeData() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            aiCredentialRepository.observeCredential().collect { credential ->
                updateState { current ->
                    current.copy(
                        isLoading = false,
                        saved = credential,
                        // 이미 연결된 제공자가 있으면 그것을 고른 채로 연다.
                        selectedProvider = credential?.provider ?: current.selectedProvider,
                    )
                }
            }
        }
    }

    override fun handleIntent(intent: AiTokenIntent) {
        when (intent) {
            is AiTokenIntent.SelectProvider ->
                updateState { it.copy(selectedProvider = intent.provider, errorMessage = null) }

            is AiTokenIntent.ChangeInput ->
                updateState { it.copy(input = intent.value, errorMessage = null) }

            is AiTokenIntent.ClickSave -> save()

            is AiTokenIntent.ClickVerify -> verify()

            is AiTokenIntent.ClickDisconnect -> disconnect()

            is AiTokenIntent.ClickBack -> updateEffect(AiTokenEffect.GoBack)
        }
    }

    private fun save() {
        val current = state.value
        if (!current.canSave) return
        val credential = AiCredential(current.selectedProvider, current.input.trim())
        viewModelScope.launch {
            runCatching { aiCredentialRepository.save(credential) }
                .onSuccess {
                    updateState { it.copy(input = "", errorMessage = null) }
                    updateEffect(AiTokenEffect.ShowMessage(SAVED))
                }
                .onFailure { updateEffect(AiTokenEffect.ShowMessage(SAVE_FAILED)) }
        }
    }

    /** 확인은 저장된 한 벌로 한다. 입력 중인 값으로 하면 저장하지 않은 키가 통과했다고 오해한다. */
    private fun verify() {
        val credential = state.value.saved ?: return
        updateState { it.copy(isVerifying = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { aiAnalysisRepository.verifyCredential(credential) }
                .onSuccess {
                    updateState { it.copy(isVerifying = false) }
                    updateEffect(AiTokenEffect.ShowMessage(VERIFIED))
                }
                .onFailure { failure ->
                    val message = if (failure is AiUnauthorizedException) INVALID_KEY else CALL_FAILED
                    updateState { it.copy(isVerifying = false, errorMessage = message) }
                }
        }
    }

    private fun disconnect() {
        if (!state.value.canDisconnect) return
        viewModelScope.launch {
            runCatching { aiCredentialRepository.clear() }
                .onSuccess {
                    updateState { it.copy(input = "", errorMessage = null) }
                    updateEffect(AiTokenEffect.ShowMessage(DISCONNECTED))
                }
                .onFailure { updateEffect(AiTokenEffect.ShowMessage(SAVE_FAILED)) }
        }
    }
}

private const val SAVED = "저장했습니다"
private const val SAVE_FAILED = "저장하지 못했습니다"
private const val DISCONNECTED = "연결을 해제했습니다"
private const val VERIFIED = "쓸 수 있는 키입니다"
private const val INVALID_KEY = "키가 올바르지 않습니다"
private const val CALL_FAILED = "연결하지 못했습니다"
