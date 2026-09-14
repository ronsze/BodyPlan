package kr.sdbk.bodyplan.core.domain.usecase

import javax.inject.Inject
import kr.sdbk.bodyplan.core.domain.model.ExerciseBest
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.previousOf
import kr.sdbk.bodyplan.core.domain.model.recordValue

/**
 * 하루 기록 중 지난 최고값을 넘긴 종목을 고른다.
 *
 * 이전 기록이 없는 종목은 PR이 아니다 — "갱신"은 견줄 것이 있어야 하고, 처음 쓰는 사용자의 모든 카드에
 * 배지가 붙으면 배지가 뜻을 잃는다. 같은 값도 갱신이 아니다.
 */
class FindPersonalRecordsUseCase
@Inject
constructor() {
    operator fun invoke(entries: List<WorkoutEntry>, bestBefore: List<ExerciseBest>): Set<Long> =
        // 축까지 갈라 묶는다 — 같은 날 축을 바꿔 다시 적으면 한 종목에 두 축의 기록이 섞여 있다.
        entries.groupBy { it.exerciseId to it.intensityType }
            .filter { (key, own) ->
                val (exerciseId, type) = key
                val previous = bestBefore.previousOf(exerciseId, type) ?: return@filter false
                own.maxOf { it.recordValue } > previous
            }
            .keys
            .mapTo(mutableSetOf()) { (exerciseId, _) -> exerciseId }
}
