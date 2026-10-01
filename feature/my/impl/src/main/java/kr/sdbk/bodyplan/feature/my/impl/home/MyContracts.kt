package kr.sdbk.bodyplan.feature.my.impl.home

import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class MyState(
    /** 연결된 제공자. 없으면 아직 붙이지 않은 것이다. */
    val connectedProvider: AiProvider? = null,
    val profile: UserProfile = UserProfile(),
    /** 키·체중이 최근 인바디 분석으로 갱신됐는지. 어디서 온 값인지 보이게 한다. */
    val profileFromInbody: Boolean = false,
    /** 가장 최근 인바디 측정값. 프로필에는 체지방률·골격근량 자리가 없어 여기서 가져와 보인다. */
    val latestInbody: InbodyMeasurement? = null,
) : State

internal sealed interface MyIntent : Intent {
    data object ClickAiToken : MyIntent

    data object ClickProfile : MyIntent

    data object ClickInbody : MyIntent

    data object ClickWeight : MyIntent

    data object ClickRoutine : MyIntent
    data object ClickBackup : MyIntent
}

internal sealed interface MyEffect : Effect {
    data object NavigateToAiToken : MyEffect

    data object NavigateToProfile : MyEffect

    data object NavigateToInbody : MyEffect

    data object NavigateToWeight : MyEffect

    data object NavigateToRoutine : MyEffect
    data object NavigateToBackup : MyEffect
}
