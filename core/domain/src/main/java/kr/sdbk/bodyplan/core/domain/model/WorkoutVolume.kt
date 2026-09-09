package kr.sdbk.bodyplan.core.domain.model

/**
 * 한 기간의 운동량.
 *
 * [weightVolumeKg]는 무게로 재는 종목만 센다 — 각도는 횟수를 곱해도 뜻이 없고, 시간은 단위가 달라
 * 한 숫자로 합치면 틀린 수가 된다.
 */
data class WorkoutVolume(val weightVolumeKg: Int, val workoutDays: Int)
