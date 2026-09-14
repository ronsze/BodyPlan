package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.model.previousOf
import kr.sdbk.bodyplan.core.domain.model.recordValue
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

/**
 * 방금 저장한 기록이 지난 최고값을 넘겼는지. 저장 직후 알림이 쓴다.
 *
 * 기준은 [FindPersonalRecordsUseCase]와 같다 — 배지가 붙는 기록에만 알려야 한다.
 */
class IsPersonalRecordUseCase
@Inject
constructor(private val workoutLogRepository: WorkoutLogRepository) {
    suspend operator fun invoke(
        date: LocalDate,
        exerciseId: Long,
        type: IntensityType,
        sets: List<WorkoutSet>,
    ): Boolean {
        val previous = workoutLogRepository.observeBestBefore(date).first().previousOf(exerciseId, type) ?: return false
        return sets.recordValue(type) > previous
    }
}
