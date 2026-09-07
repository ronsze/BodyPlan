package kr.sdbk.bodyplan.core.domain.model

enum class Gender { MALE, FEMALE }

/** 운동과 식단의 목적. 여럿 고를 수 있다. */
enum class Goal { DIET, MUSCLE_GAIN, TARGET_WEIGHT, TARGET_STRENGTH }

/**
 * 분석의 바탕이 되는 사용자 정보.
 *
 * 모든 값이 비어 있을 수 있다. 온보딩을 건너뛰거나 일부만 채울 수 있기 때문이다.
 * 분석은 채워진 것만 근거로 삼는다.
 */
data class UserProfile(
    val ageYears: Int? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val gender: Gender? = null,
    val goals: Set<Goal> = emptySet(),
    val targetWeightKg: Int? = null,
    val targetNote: String? = null,
) {
    val isEmpty: Boolean
        get() = ageYears == null &&
            heightCm == null &&
            weightKg == null &&
            gender == null &&
            goals.isEmpty() &&
            targetWeightKg == null &&
            targetNote.isNullOrBlank()
}
