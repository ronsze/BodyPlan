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
    /** 한 주에 운동할 날 수. 홈이 달성 여부와 연속 주를 센다. */
    val weeklyWorkoutGoal: Int? = null,
) {
    val isEmpty: Boolean
        get() = ageYears == null &&
            heightCm == null &&
            weightKg == null &&
            gender == null &&
            goals.isEmpty() &&
            targetWeightKg == null &&
            targetNote.isNullOrBlank() &&
            weeklyWorkoutGoal == null

    companion object {
        /** 주에 운동할 수 있는 날은 7일뿐이다. */
        val WEEKLY_GOAL_RANGE: IntRange = 1..7
    }
}
