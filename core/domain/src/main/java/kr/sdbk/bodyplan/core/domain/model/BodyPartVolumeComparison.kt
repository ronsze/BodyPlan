package kr.sdbk.bodyplan.core.domain.model

/**
 * 한 부위의 볼륨을 사용자가 고른 날의 같은 부위와 견준 결과.
 *
 * 그날 그 부위의 무게 기록이 없으면 [comparedVolumeKg]·[changeKg]가 `null`이다 —
 * 견줄 것이 없는 것을 0kg에서의 증가로 읽히지 않게 한다.
 */
data class BodyPartVolumeComparison(val bodyPart: BodyPart, val volumeKg: Int, val comparedVolumeKg: Int?) {
    val changeKg: Int? get() = comparedVolumeKg?.let { volumeKg - it }
}
