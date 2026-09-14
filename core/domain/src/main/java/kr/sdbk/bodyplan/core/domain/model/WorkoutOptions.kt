package kr.sdbk.bodyplan.core.domain.model

/**
 * 기록 작성 화면이 제시하는 선택지와 받아들이는 범위. 화면이 임의 값을 만들지 않도록 한 곳에 둔다.
 *
 * 무게·횟수는 목록이 아니라 범위다 — 손으로 넣는 값이라 눈금에 묶을 이유가 없다. 각도·시간은 선택지가 적어 목록으로 남긴다.
 */
object WorkoutOptions {
    val angleDegrees: List<Int> = (0..MAX_ANGLE step ANGLE_STEP).toList()

    val durationMinutes: List<Int> = (DURATION_STEP..MAX_DURATION step DURATION_STEP).toList()

    const val MAX_SET_COUNT: Int = 10

    /** 0은 맨몸이다. */
    const val MIN_WEIGHT_KG: Int = 0
    const val MAX_WEIGHT_KG: Int = 999
    const val MIN_REPEAT_COUNT: Int = 1
    const val MAX_REPEAT_COUNT: Int = 999

    /** 입력칸이 받는 자릿수. 세 자리면 위 최댓값이 다 들어간다. */
    const val INPUT_MAX_DIGITS: Int = 3

    fun isValidWeight(kilograms: Int): Boolean = kilograms in MIN_WEIGHT_KG..MAX_WEIGHT_KG

    fun isValidRepeatCount(count: Int): Boolean = count in MIN_REPEAT_COUNT..MAX_REPEAT_COUNT

    /** 목록에서 고르는 축의 기본값. 무게는 손으로 넣으므로 기본값이 없다 — `null`. */
    fun defaultIntensity(type: IntensityType): Intensity? = when (type) {
        IntensityType.WEIGHT -> null
        IntensityType.ANGLE -> Intensity.Angle(angleDegrees.first())
        IntensityType.DURATION -> Intensity.Duration(durationMinutes.first())
    }

    /** 시간으로 재는 종목은 한 세트가 한 회차다. 횟수를 고르게 하지 않는다. */
    const val SINGLE_REPEAT: Int = 1
}

private const val ANGLE_STEP = 15
private const val MAX_ANGLE = 60
private const val DURATION_STEP = 5
private const val MAX_DURATION = 120
