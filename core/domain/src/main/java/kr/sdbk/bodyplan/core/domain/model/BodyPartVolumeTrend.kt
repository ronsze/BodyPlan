package kr.sdbk.bodyplan.core.domain.model

/**
 * 한 부위의 두 구간 볼륨 비교. [changeKg]는 최근에서 그 앞을 뺀 값이다.
 *
 * 두 구간 모두 무게 기록이 없는 부위는 만들어지지 않고, 한쪽만 있으면 없는 쪽을 0으로 본다 —
 * 지난주에 하던 부위를 이번 주에 안 했으면 그것도 "줄었다"다.
 * [direction]은 부호 그대로다. 볼륨은 정수라 1kg 차이도 변화이고, 견줄 것이 없는 경우는 애초에 담기지 않아
 * [ProgressDirection.UNKNOWN]을 쓰지 않는다.
 */
data class BodyPartVolumeTrend(
    val bodyPart: BodyPart,
    val recentVolumeKg: Int,
    val changeKg: Int,
    val direction: ProgressDirection,
)
