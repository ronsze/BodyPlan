package kr.sdbk.bodyplan.feature.my.impl.inbody

import kr.sdbk.bodyplan.core.domain.model.InbodyEntry
import kr.sdbk.bodyplan.core.domain.model.InbodyPeriod
import kr.sdbk.bodyplan.core.domain.model.InbodyTrend
import kr.sdbk.bodyplan.core.domain.usecase.InbodyTimeline
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class InbodyState(
    val period: InbodyPeriod = InbodyPeriod.ONE_MONTH,
    /** 측정 순서 내림차순. 기간과 상관없이 모든 기록이다. */
    val entries: List<InbodyEntry> = emptyList(),
    /** 선택한 기간의 그래프 값. 기록을 읽기 전에는 `null`이다. */
    val trend: InbodyTrend? = null,
    val hasCredential: Boolean = false,
    val isLoading: Boolean = false,
    val isTokenDialogVisible: Boolean = false,
    val errorMessage: String? = null,
) : State {
    /** 「최근 측정」을 붙일 기록의 번호. */
    val latestMeasuredId: Long? get() = InbodyTimeline.latestMeasured(entries)?.record?.id
}

internal sealed interface InbodyIntent : Intent {
    data class SelectPeriod(val period: InbodyPeriod) : InbodyIntent

    data class ClickAnalyze(val recordId: Long) : InbodyIntent

    data object ConfirmTokenDialog : InbodyIntent

    data object DismissTokenDialog : InbodyIntent
}

internal sealed interface InbodyEffect : Effect {
    data object NavigateToAiToken : InbodyEffect
}
