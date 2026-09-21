package kr.sdbk.bodyplan.feature.my.impl.aitoken

import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.OnDeviceModelStatus
import kr.sdbk.bodyplan.core.domain.model.requiresToken
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

/**
 * 화면이 세 모습 중 하나다 — 고르는 중, 연결하는 중(키 넣기 또는 모델 내려받기), 연결된 뒤.
 *
 * [connected]가 있으면 연결된 뒤, 없고 [connectingProvider]가 있으면 연결하는 중,
 * 둘 다 없으면 고르는 중이다. 한 번에 하나만 연결되므로 이 셋으로 충분하다.
 */
internal data class AiTokenState(
    val connected: AiCredential? = null,
    val connectingProvider: AiProvider? = null,
    val input: String = "",
    val isLoading: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    /** `null`은 아직 확인 전. 확인 전과 미지원은 둘 다 선택지를 숨긴다. */
    val onDeviceStatus: OnDeviceModelStatus? = null,
    val downloadTotalBytes: Long = 0,
    val downloadedBytes: Long = 0,
) : State {
    /** 고를 수 있는 제공자. 온디바이스는 이 기기가 지원할 때만 들어간다. */
    val providers: List<AiProvider>
        get() = AiProvider.entries.filter { provider ->
            provider.requiresToken || (onDeviceStatus != null && onDeviceStatus != OnDeviceModelStatus.UNSUPPORTED)
        }

    /** 저장된 키는 그대로 보여주지 않는다. 어깨너머로 읽히는 것을 막는다. 키가 없는 제공자는 `null`. */
    val connectedTokenMask: String? get() = connected?.takeIf { it.provider.requiresToken }?.token?.mask()

    val canConnect: Boolean
        get() = when (connectingProvider?.requiresToken) {
            true -> input.isNotBlank() && !isConnecting
            false -> !isConnecting
            null -> false
        }

    val canDisconnect: Boolean get() = connected != null && !isConnecting

    /** 0..1 진행률. 전체 크기를 아직 모르면 `null`이라 화면은 무한 표시를 쓴다. */
    val downloadRatio: Float?
        get() = downloadTotalBytes.takeIf { it > 0 }?.let { (downloadedBytes.toFloat() / it).coerceIn(0f, 1f) }
}

private const val VISIBLE_CHARS = 4

private fun String.mask(): String {
    if (length <= VISIBLE_CHARS * 2) return "*".repeat(length)
    return take(VISIBLE_CHARS) + "*".repeat(6) + takeLast(VISIBLE_CHARS)
}

internal sealed interface AiTokenIntent : Intent {
    /** 제공자 버튼을 눌러 연동을 시작한다. 온디바이스는 모델이 준비돼 있으면 바로 연결된다. */
    data class ClickProvider(val provider: AiProvider) : AiTokenIntent

    data class ChangeInput(val value: String) : AiTokenIntent

    /** 키를 검증하고, 통과할 때만 저장한다. */
    data object ClickConnect : AiTokenIntent

    /** 온디바이스 모델을 내려받고, 끝나면 연결한다. */
    data object ClickDownloadModel : AiTokenIntent

    data object ClickCancelConnect : AiTokenIntent

    data object ClickDisconnect : AiTokenIntent

    data object ClickBack : AiTokenIntent
}

internal sealed interface AiTokenEffect : Effect {
    data object GoBack : AiTokenEffect

    data class ShowMessage(val message: String) : AiTokenEffect
}
