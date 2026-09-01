package kr.sdbk.bodyplan.feature.home.impl

import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class HomeState(val isLoading: Boolean = false) : State

internal sealed interface HomeIntent : Intent

internal sealed interface HomeEffect : Effect
