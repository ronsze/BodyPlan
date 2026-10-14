package kr.sdbk.bodyplan.feature.my.impl.inbodydetail

import kr.sdbk.bodyplan.core.domain.model.InbodyDetail
import kr.sdbk.bodyplan.core.domain.model.InbodyEntryStatus
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class InbodyDetailState(
    /** 기록을 읽기 전이나 지워진 기록이면 `null`이다. */
    val detail: InbodyDetail? = null,
    val isLoading: Boolean = false,
    val hasCredential: Boolean = false,
    val isTokenDialogVisible: Boolean = false,
    val isImageViewerVisible: Boolean = false,
) : State {
    /** 분석 중이면 같은 기록을 다시 요청할 수 없다. */
    val canAnalyze: Boolean get() = detail != null && detail.entry.status != InbodyEntryStatus.RUNNING
}

internal sealed interface InbodyDetailIntent : Intent {
    data object ClickAnalyze : InbodyDetailIntent

    data object ClickImage : InbodyDetailIntent

    data object DismissImageViewer : InbodyDetailIntent

    data object ConfirmTokenDialog : InbodyDetailIntent

    data object DismissTokenDialog : InbodyDetailIntent
}

internal sealed interface InbodyDetailEffect : Effect {
    data object NavigateToAiToken : InbodyDetailEffect
}
