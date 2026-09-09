package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrendResult
import kr.sdbk.bodyplan.core.domain.model.RecordedExercise
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

/**
 * 최근 몇 주의 기록에서 고를 수 있는 종목과, 고른 종목의 추이를 낸다.
 *
 * 종목 목록을 [kr.sdbk.bodyplan.core.domain.repository.ExerciseRepository]가 아니라 기록에서 뽑는 것은
 * 두 가지 때문이다 — 기록이 없는 종목을 골라 빈 그래프를 보는 일이 없고, 종목 목록에서 지운 종목도
 * 과거 기록이 있으면 볼 수 있다(기록이 이름·부위·축을 스냅샷으로 들고 있다).
 */
class GetExerciseTrendUseCase
@Inject
constructor(
    private val workoutLogRepository: WorkoutLogRepository,
    private val summarizeExerciseTrend: SummarizeExerciseTrendUseCase,
    private val clock: Clock,
) {
    /** [exerciseId]가 `null`이면 종목 목록만 낸다. */
    operator fun invoke(exerciseId: Long?): Flow<ExerciseTrendResult> {
        // 구독 중 자정을 넘겨도 구간이 흔들리지 않도록 여기서 한 번만 읽는다.
        val today = LocalDate.now(clock)
        val thisWeek = AnalysisScopeKey.weekStart(today)
        val weekStarts = (TREND_WEEKS - 1 downTo 0).map { thisWeek.minusWeeks(it.toLong()) }

        return workoutLogRepository.observeEntriesInRange(weekStarts.first(), today).map { entriesByDate ->
            ExerciseTrendResult(
                exercises = entriesByDate.toRecordedExercises(),
                trend = exerciseId?.let { summarizeExerciseTrend(entriesByDate, it, weekStarts) },
            )
        }
    }

    /** 같은 종목이 여러 번 나오면 가장 최근 기록의 스냅샷을 쓴다 — 이름을 고쳤을 수 있다. */
    private fun Map<LocalDate, List<WorkoutEntry>>.toRecordedExercises(): List<RecordedExercise> =
        entries.sortedBy { it.key }
            .flatMap { (_, entries) -> entries }
            .associate { entry ->
                entry.exerciseId to RecordedExercise(
                    id = entry.exerciseId,
                    name = entry.exerciseName,
                    bodyPart = entry.bodyPart,
                    intensityType = entry.intensityType,
                )
            }
            .values
            .sortedBy { it.name }
}

/** 견주는 주의 수. 두 달 남짓이라 흐름이 보이면서도 한 화면에 들어간다. */
private const val TREND_WEEKS = 8
