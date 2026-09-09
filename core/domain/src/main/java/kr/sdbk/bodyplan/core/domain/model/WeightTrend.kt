package kr.sdbk.bodyplan.core.domain.model

/**
 * 체중이 어느 쪽으로 얼마나 움직였는지. 단위는 kg이고 음수면 줄어든 것이다.
 *
 * 값마다 없을 수 있다. 비교할 두 쪽 중 하나라도 기록이 없으면 뺄 것이 없기 때문이다 —
 * 그 경우 0이 아니라 `null`이다. 0으로 두면 "변화가 없었다"로 읽힌다.
 */
data class WeightTrend(
    val dailyChangeKg: Double? = null,
    val weeklyAverageChangeKg: Double? = null,
    val monthlyAverageChangeKg: Double? = null,
)
