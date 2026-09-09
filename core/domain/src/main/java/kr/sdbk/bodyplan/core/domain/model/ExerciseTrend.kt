package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate

/** 추이로 그릴 수 있는 값의 종류. */
enum class ExerciseTrendMetric {
    MAX_WEIGHT,
    TOTAL_VOLUME,
    TOTAL_REPS,
    TOTAL_MINUTES,
}

/**
 * 강도 축이 그릴 지표를 정한다.
 *
 * 각도는 곱해도 뜻이 없고 시간은 횟수를 세지 않아, 축마다 셀 수 있는 것이 다르다.
 * 축이 늘면 여기만 고친다.
 */
val IntensityType.trendMetrics: List<ExerciseTrendMetric>
    get() = when (this) {
        IntensityType.WEIGHT -> listOf(ExerciseTrendMetric.MAX_WEIGHT, ExerciseTrendMetric.TOTAL_VOLUME)
        IntensityType.ANGLE -> listOf(ExerciseTrendMetric.TOTAL_REPS)
        IntensityType.DURATION -> listOf(ExerciseTrendMetric.TOTAL_MINUTES)
    }

/**
 * 한 주의 값.
 *
 * [values]는 그 주에 기록이 없으면 비어 있다. 주는 빠짐없이 담기고, 값이 없는 주가 그래프에서 끊긴 자리가 된다.
 */
data class ExerciseTrendPoint(val weekStart: LocalDate, val values: Map<ExerciseTrendMetric, Int>)

/**
 * 기록에 남은 종목.
 *
 * [Exercise]와 다르다 — 그것은 지금 등록된 종목이고 이것은 기록에 박힌 그때의 스냅샷이다.
 * 종목 목록에서 지워도 과거 기록에는 남으므로 `isDeleted`가 없다.
 */
data class RecordedExercise(val id: Long, val name: String, val bodyPart: BodyPart, val intensityType: IntensityType)

/** 종목 하나의 주간 추이. [points]는 오래된 주부터다. */
data class ExerciseTrend(val exercise: RecordedExercise, val points: List<ExerciseTrendPoint>)

/** 추이 화면이 한 번에 받는 것 — 고를 수 있는 종목과, 고른 종목의 추이. */
data class ExerciseTrendResult(val exercises: List<RecordedExercise> = emptyList(), val trend: ExerciseTrend? = null)
