package kr.sdbk.bodyplan.feature.my.impl.aitoken

import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

/**
 * 화면이 세 모습 중 하나다 — 고르는 중, 키를 넣는 중, 연결된 뒤.
 *
 * [connected]가 있으면 연결된 뒤, 없고 [connectingProvider]가 있으면 키를 넣는 중,
 * 둘 다 없으면 고르는 중이다. 한 번에 하나만 연결되므로 이 셋으로 충분하다.
 */
internal data class AiTokenState(
    val connected: AiCredential? = null,
    val connectingProvider: AiProvider? = null,
    val input: String = "",
    val isLoading: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
) : State {
    /** 저장된 키는 그대로 보여주지 않는다. 어깨너머로 읽히는 것을 막는다. */
    val connectedTokenMask: String? get() = connected?.token?.mask()

    val canConnect: Boolean get() = input.isNotBlank() && !isConnecting

    val canDisconnect: Boolean get() = connected != null && !isConnecting
}

private const val VISIBLE_CHARS = 4

private fun String.mask(): String {
    if (length <= VISIBLE_CHARS * 2) return "*".repeat(length)
    return take(VISIBLE_CHARS) + "*".repeat(6) + takeLast(VISIBLE_CHARS)
}

internal sealed interface AiTokenIntent : Intent {
    /** 제공자 버튼을 눌러 연동을 시작한다. */
    data class ClickProvider(val provider: AiProvider) : AiTokenIntent

    data class ChangeInput(val value: String) : AiTokenIntent

    /** 키를 검증하고, 통과할 때만 저장한다. */
    data object ClickConnect : AiTokenIntent

    data object ClickCancelConnect : AiTokenIntent

    data object ClickDisconnect : AiTokenIntent

    data object ClickBack : AiTokenIntent
}

internal sealed interface AiTokenEffect : Effect {
    data object GoBack : AiTokenEffect

    data class ShowMessage(val message: String) : AiTokenEffect
}
