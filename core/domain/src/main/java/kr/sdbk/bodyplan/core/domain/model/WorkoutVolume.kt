package kr.sdbk.bodyplan.core.domain.model

/**
 * 한 기간의 운동량.
 *
 * [weightVolumeKg]는 무게로 재는 종목만 센다 — 각도는 횟수를 곱해도 뜻이 없고, 시간은 단위가 달라
 * 한 숫자로 합치면 틀린 수가 된다.
 */
data class WorkoutVolume(val weightVolumeKg: Int, val workoutDays: Int)

/**
 * 기록 한 건의 무게 볼륨. 무게 종목이 아니면 0이다.
 *
 * 기간 합·부위별 합·추이가 전부 같은 셈을 써야 하므로 UseCase 안이 아니라 모델 옆에 둔다.
 */
val WorkoutEntry.weightVolumeKg: Int
    get() = if (intensityType != IntensityType.WEIGHT) {
        0
    } else {
        sets.sumOf { it.intensity.value * it.repeatCount }
    }
