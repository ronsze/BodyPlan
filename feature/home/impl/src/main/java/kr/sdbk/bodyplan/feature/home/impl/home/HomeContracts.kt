package kr.sdbk.bodyplan.feature.home.impl.home

import kr.sdbk.bodyplan.core.domain.model.ProgressSummary
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class HomeState(
    val summary: ProgressSummary? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : State

internal sealed interface HomeIntent : Intent {
    data object ClickRetry : HomeIntent
}

/** 홈은 다른 화면으로 나가지도 알림을 띄우지도 않는다. 베이스가 타입을 요구해 선언만 둔다. */
internal sealed interface HomeEffect : Effect
