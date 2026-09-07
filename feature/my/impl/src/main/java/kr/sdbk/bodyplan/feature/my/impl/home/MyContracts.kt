package kr.sdbk.bodyplan.feature.my.impl.home

import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class MyState(
    /** 연결된 제공자. 없으면 아직 붙이지 않은 것이다. */
    val connectedProvider: AiProvider? = null,
) : State

internal sealed interface MyIntent : Intent {
    data object ClickAiToken : MyIntent
}

internal sealed interface MyEffect : Effect {
    data object NavigateToAiToken : MyEffect
}
