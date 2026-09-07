package kr.sdbk.bodyplan.feature.my.impl.aitoken

import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class AiTokenState(
    val selectedProvider: AiProvider = AiProvider.CLAUDE,
    val input: String = "",
    val saved: AiCredential? = null,
    val isLoading: Boolean = false,
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
) : State {
    /** 저장된 키는 그대로 보여주지 않는다. 어깨너머로 읽히는 것을 막는다. */
    val savedTokenMask: String? get() = saved?.token?.mask()

    val canSave: Boolean get() = input.isNotBlank() && !isVerifying

    val canVerify: Boolean get() = saved != null && !isVerifying

    val canDisconnect: Boolean get() = saved != null && !isVerifying
}

private const val VISIBLE_CHARS = 4

private fun String.mask(): String {
    if (length <= VISIBLE_CHARS * 2) return "*".repeat(length)
    return take(VISIBLE_CHARS) + "*".repeat(6) + takeLast(VISIBLE_CHARS)
}

internal sealed interface AiTokenIntent : Intent {
    data class SelectProvider(val provider: AiProvider) : AiTokenIntent

    data class ChangeInput(val value: String) : AiTokenIntent

    data object ClickSave : AiTokenIntent

    data object ClickVerify : AiTokenIntent

    data object ClickDisconnect : AiTokenIntent

    data object ClickBack : AiTokenIntent
}

internal sealed interface AiTokenEffect : Effect {
    data object GoBack : AiTokenEffect

    data class ShowMessage(val message: String) : AiTokenEffect
}
