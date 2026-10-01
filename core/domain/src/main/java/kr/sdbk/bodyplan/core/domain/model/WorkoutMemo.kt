package kr.sdbk.bodyplan.core.domain.model

/** 그날의 컨디션. 운동 전후로 느낀 몸 상태를 세 단계로만 가른다 — 더 잘게 나누면 고르는 데 망설이게 된다. */
enum class Condition { GOOD, NORMAL, BAD }

/** 그날 운동의 체감 강도. 무게·횟수와 별개로 몸이 느낀 정도다. */
enum class Effort { LIGHT, MODERATE, HARD }

/**
 * 하루치 운동 소감. 탭 한 번으로 고르는 칩([condition]·[effort]·[painParts])과 덧붙이는 글([text])로 이룬다.
 *
 * 모두 비어 있을 수 있다. 칩만 고르고 글을 안 쓰는 날이 기본 사용 방식이다.
 */
data class WorkoutMemo(
    val condition: Condition? = null,
    val effort: Effort? = null,
    val painParts: Set<BodyPart> = emptySet(),
    val text: String? = null,
) {
    val isEmpty: Boolean
        get() = condition == null && effort == null && painParts.isEmpty() && text.isNullOrBlank()
}
