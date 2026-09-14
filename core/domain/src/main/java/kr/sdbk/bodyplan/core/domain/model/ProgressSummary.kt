package kr.sdbk.bodyplan.core.domain.model

/** 요약이 견주는 축. */
enum class ProgressMetricKey {
    WEIGHT,
    WORKOUT_VOLUME,
    WORKOUT_DAYS,
    SKELETAL_MUSCLE,
    BODY_FAT,
}

/**
 * 그 축이 어느 쪽으로 갔는지.
 *
 * [STEADY]와 [UNKNOWN]은 다르다 — 앞은 견줘 보니 그대로인 것이고, 뒤는 견줄 것이 없거나
 * 좋고 나쁨을 정할 근거가 없는 것이다.
 */
enum class ProgressDirection {
    IMPROVING,
    WORSENING,
    STEADY,
    UNKNOWN,
}

/** 지표 전체를 한마디로 줄인 것. */
enum class ProgressHeadline {
    IMPROVING,
    WORSENING,
    STEADY,
    NOT_ENOUGH_DATA,
}

/**
 * 축 하나의 변화.
 *
 * [changeValue]가 `null`이면 견줄 구간이 비었다는 뜻이지 0이 아니다 — 0으로 두면
 * "변화가 없었다"로 읽힌다([WeightTrend]와 같은 판단). 이때 [direction]은 늘 [ProgressDirection.UNKNOWN]이다.
 *
 * 일수·볼륨처럼 정수인 축도 [Double]에 담는다. 축마다 필드를 나누면 화면이 축별로 갈라져야 한다.
 */
data class ProgressMetric(val key: ProgressMetricKey, val changeValue: Double?, val direction: ProgressDirection)

/**
 * 홈이 보여주는 진척 요약.
 *
 * [recentMetrics]는 최근 7일과 그 앞 7일을 견준 것이고, [bodyCompositionMetrics]는 인바디 최근
 * 두 건을 견준 것이다 — 인바디는 매일 찍지 않아 같은 기간으로 묶을 수 없다.
 * 값이 없는 축도 빼지 않고 담는다. 화면이 `-`로 보여야 하기 때문이다.
 *
 * [bodyPartVolumeTrends]는 [recentMetrics]와 같은 두 구간을 부위별로 가른 것이다. 헤드라인에는 넣지 않는다 —
 * 부위 수만큼 표가 늘어 헤드라인이 볼륨에 치우친다.
 */
data class ProgressSummary(
    val headline: ProgressHeadline = ProgressHeadline.NOT_ENOUGH_DATA,
    val recentMetrics: List<ProgressMetric> = emptyList(),
    val bodyCompositionMetrics: List<ProgressMetric> = emptyList(),
    val bodyPartVolumeTrends: List<BodyPartVolumeTrend> = emptyList(),
)
