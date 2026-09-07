package kr.sdbk.bodyplan.feature.my.impl.onboarding

import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State
import kr.sdbk.bodyplan.feature.my.impl.profile.ProfileInput

internal data class OnboardingState(val input: ProfileInput = ProfileInput(), val isSaving: Boolean = false) : State

internal sealed interface OnboardingIntent : Intent {
    data class ChangeInput(val input: ProfileInput) : OnboardingIntent

    data object ClickStart : OnboardingIntent

    /** 건너뛰기. 아무 값도 저장하지 않고 온보딩만 지나간 것으로 남긴다. */
    data object ClickSkip : OnboardingIntent
}

internal sealed interface OnboardingEffect : Effect {
    data class ShowMessage(val message: String) : OnboardingEffect
}
