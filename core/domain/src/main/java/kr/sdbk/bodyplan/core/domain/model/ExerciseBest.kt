package kr.sdbk.bodyplan.core.domain.model

/**
 * 한 종목이 한 축으로 잰 지난 최고값.
 *
 * 축마다 무엇이 최고인지가 다르다 — 각도 종목은 각도를 올리는 게 아니라 횟수를 늘리는 것이 성장이라 횟수를 본다.
 * 종목의 축을 바꿀 수 있어 [intensityType]을 함께 든다. 다른 축으로 잰 기록은 견줄 대상이 아니다.
 */
data class ExerciseBest(
    val exerciseId: Long,
    val intensityType: IntensityType,
    val maxIntensityValue: Int,
    val maxRepeatCount: Int,
)

val ExerciseBest.recordValue: Int
    get() = if (intensityType == IntensityType.ANGLE) maxRepeatCount else maxIntensityValue

/** 세트 목록에서 PR을 가르는 값. [ExerciseBest.recordValue]와 같은 기준이어야 배지와 토스트가 어긋나지 않는다. */
fun List<WorkoutSet>.recordValue(type: IntensityType): Int = if (type == IntensityType.ANGLE) {
    maxOfOrNull { it.repeatCount } ?: 0
} else {
    maxOfOrNull { it.intensity.value } ?: 0
}

val WorkoutEntry.recordValue: Int get() = sets.recordValue(intensityType)

/** 그 종목을 지금 축으로 잰 지난 최고값. 다른 축의 기록만 있으면 견줄 것이 없어 `null`이다. */
fun List<ExerciseBest>.previousOf(exerciseId: Long, type: IntensityType): Int? =
    firstOrNull { it.exerciseId == exerciseId && it.intensityType == type }?.recordValue
