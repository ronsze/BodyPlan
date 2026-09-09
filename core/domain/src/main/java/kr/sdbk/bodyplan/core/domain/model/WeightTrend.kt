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
    /**
     * 주간 변동을 이루는 두 구간의 평균. 화면은 쓰지 않고, 이 평균으로 판정하는 곳이 쓴다.
     *
     * 변동만 내면 호출부가 `최근평균 - 변동`으로 이전 평균을 역산하게 되는데, 그 유도는 평균법이
     * 같을 때만 성립한다. 평균 규약이 여러 곳에 흩어지지 않게 여기서 함께 낸다.
     */
    val recentWeeklyAverageKg: Double? = null,
    val previousWeeklyAverageKg: Double? = null,
)
