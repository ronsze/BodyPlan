package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import javax.inject.Inject
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrend
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrendMetric
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrendPoint
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.RecordedExercise
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.model.trendMetrics

/**
 * 이미 읽은 기록에서 종목 하나의 주간 추이를 낸다.
 *
 * 저장소를 스스로 구독하지 않고 받는다 — [SummarizeWorkoutVolumeUseCase]·[GetWeightTrendUseCase]와 같은 짜임새다.
 */
class SummarizeExerciseTrendUseCase
@Inject
constructor() {
    /**
     * [weekStarts]는 오래된 주부터이며 여기 없는 주의 기록은 버린다.
     * 그 종목의 기록이 하나도 없으면 `null`이다.
     */
    operator fun invoke(
        entriesByDate: Map<LocalDate, List<WorkoutEntry>>,
        exerciseId: Long,
        weekStarts: List<LocalDate>,
    ): ExerciseTrend? {
        val recorded = entriesByDate
            .flatMap { (date, entries) -> entries.filter { it.exerciseId == exerciseId }.map { date to it } }
            .filter { (date, _) -> AnalysisScopeKey.weekStart(date) in weekStarts }
        if (recorded.isEmpty()) return null

        val byWeek = recorded.groupBy({ AnalysisScopeKey.weekStart(it.first) }, { it.second })

        // 이름을 고친 종목은 최근 이름으로 보인다. 기록마다 그때의 이름이 박혀 있어 골라야 한다.
        val exercise = recorded.sortedBy { it.first }.last().second.toRecordedExercise()

        return ExerciseTrend(
            exercise = exercise,
            points = weekStarts.map { weekStart ->
                ExerciseTrendPoint(
                    weekStart = weekStart,
                    // 축은 종목이 정한다. 주마다 다시 읽으면 축을 고친 종목에서 화면이 못 찾는 키가 생긴다.
                    values = valuesOf(byWeek[weekStart].orEmpty(), exercise.intensityType),
                )
            },
        )
    }

    private fun valuesOf(entries: List<WorkoutEntry>, intensityType: IntensityType): Map<ExerciseTrendMetric, Int> {
        val sets = entries.flatMap { it.sets }
        if (sets.isEmpty()) return emptyMap()

        return intensityType.trendMetrics.associateWith { metric -> metric.of(sets) }
    }

    private fun ExerciseTrendMetric.of(sets: List<WorkoutSet>): Int = when (this) {
        ExerciseTrendMetric.MAX_WEIGHT -> sets.maxOf { it.intensity.value }

        ExerciseTrendMetric.TOTAL_VOLUME -> sets.sumOf { it.intensity.value * it.repeatCount }

        ExerciseTrendMetric.TOTAL_REPS -> sets.sumOf { it.repeatCount }

        // 한 세트가 한 회차라 횟수를 곱하지 않는다.
        ExerciseTrendMetric.TOTAL_MINUTES -> sets.sumOf { it.intensity.value }
    }

    private fun WorkoutEntry.toRecordedExercise(): RecordedExercise = RecordedExercise(
        id = exerciseId,
        name = exerciseName,
        bodyPart = bodyPart,
        intensityType = intensityType,
    )
}
