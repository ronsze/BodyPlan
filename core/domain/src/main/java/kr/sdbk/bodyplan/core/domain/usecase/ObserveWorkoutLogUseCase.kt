package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kr.sdbk.bodyplan.core.domain.model.WorkoutLogWithRecords
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

/**
 * 하루치 기록을 PR 판정과 함께 낸다.
 *
 * 과거 기록을 고치면 다른 날의 PR도 바뀌어야 하므로 지난 최고값을 저장하지 않고 함께 구독한다.
 */
class ObserveWorkoutLogUseCase
@Inject
constructor(
    private val workoutLogRepository: WorkoutLogRepository,
    private val findPersonalRecords: FindPersonalRecordsUseCase,
) {
    operator fun invoke(date: LocalDate): Flow<WorkoutLogWithRecords> = combine(
        workoutLogRepository.observeLog(date),
        workoutLogRepository.observeBestBefore(date),
    ) { log, bestBefore ->
        WorkoutLogWithRecords(log = log, personalRecordExerciseIds = findPersonalRecords(log.entries, bestBefore))
    }
}
