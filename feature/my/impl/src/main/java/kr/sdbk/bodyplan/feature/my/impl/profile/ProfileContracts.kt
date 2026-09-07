package kr.sdbk.bodyplan.feature.my.impl.profile

import kr.sdbk.bodyplan.core.domain.model.Gender
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

/**
 * 입력 중인 프로필. 숫자도 문자열로 들고 있다 — 지우는 도중의 빈 칸과 0을 구분해야 하기 때문이다.
 *
 * 온보딩과 프로필 수정이 같은 입력을 쓰므로 두 화면이 이 타입을 함께 쓴다.
 */
internal data class ProfileInput(
    val ageYears: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val gender: Gender? = null,
    val goals: Set<Goal> = emptySet(),
    val targetWeightKg: String = "",
    val targetNote: String = "",
) {
    fun toProfile(): UserProfile = UserProfile(
        ageYears = ageYears.toIntOrNull(),
        heightCm = heightCm.toIntOrNull(),
        weightKg = weightKg.toIntOrNull(),
        gender = gender,
        goals = goals,
        targetWeightKg = targetWeightKg.toIntOrNull(),
        targetNote = targetNote.trim().ifBlank { null },
    )

    companion object {
        fun from(profile: UserProfile) = ProfileInput(
            ageYears = profile.ageYears?.toString().orEmpty(),
            heightCm = profile.heightCm?.toString().orEmpty(),
            weightKg = profile.weightKg?.toString().orEmpty(),
            gender = profile.gender,
            goals = profile.goals,
            targetWeightKg = profile.targetWeightKg?.toString().orEmpty(),
            targetNote = profile.targetNote.orEmpty(),
        )
    }
}

internal data class ProfileState(
    val input: ProfileInput = ProfileInput(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) : State

internal sealed interface ProfileIntent : Intent {
    data class ChangeInput(val input: ProfileInput) : ProfileIntent

    data object ClickSave : ProfileIntent

    data object ClickBack : ProfileIntent
}

internal sealed interface ProfileEffect : Effect {
    data object GoBack : ProfileEffect

    data class ShowMessage(val message: String) : ProfileEffect
}
