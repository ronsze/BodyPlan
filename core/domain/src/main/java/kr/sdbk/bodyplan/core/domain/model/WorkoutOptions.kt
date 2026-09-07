package kr.sdbk.bodyplan.core.domain.model

/** 기록 작성 화면이 제시하는 선택지. 화면이 임의 값을 만들지 않도록 한 곳에 둔다. */
object WorkoutOptions {
    val weightKilograms: List<Int> = (WEIGHT_STEP..MAX_WEIGHT step WEIGHT_STEP).toList()

    val angleDegrees: List<Int> = (0..MAX_ANGLE step ANGLE_STEP).toList()

    val repeatCounts: List<Int> = (REPEAT_STEP..MAX_REPEAT step REPEAT_STEP).toList()

    val durationMinutes: List<Int> = (DURATION_STEP..MAX_DURATION step DURATION_STEP).toList()

    const val MAX_SET_COUNT: Int = 10

    fun defaultIntensity(type: IntensityType): Intensity = when (type) {
        IntensityType.WEIGHT -> Intensity.Weight(weightKilograms.first())
        IntensityType.ANGLE -> Intensity.Angle(angleDegrees.first())
        IntensityType.DURATION -> Intensity.Duration(durationMinutes.first())
    }

    /** 시간으로 재는 종목은 한 세트가 한 회차다. 횟수를 고르게 하지 않는다. */
    const val SINGLE_REPEAT: Int = 1
}

private const val WEIGHT_STEP = 5
private const val MAX_WEIGHT = 100
private const val ANGLE_STEP = 15
private const val MAX_ANGLE = 60
private const val REPEAT_STEP = 4
private const val MAX_REPEAT = 20
private const val DURATION_STEP = 5
private const val MAX_DURATION = 120
