package kr.sdbk.bodyplan.feature.dietlog.impl

import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class DietLogState(val isLoading: Boolean = false) : State

internal sealed interface DietLogIntent : Intent

internal sealed interface DietLogEffect : Effect
