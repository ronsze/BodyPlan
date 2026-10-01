package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolumeComparison
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

/**
 * 그날의 부위별 볼륨을 사용자가 고른 다른 날의 같은 부위 볼륨과 견준다.
 *
 * 견줄 날은 호출부가 정한다 — "직전 운동"을 자동으로 찾으면 사용자가 보고 싶은 날과 어긋난다.
 * 부위별 셈은 [SummarizeBodyPartVolumeUseCase]가 한다.
 */
class CompareBodyPartVolumeWithDateUseCase
@Inject
constructor(
    private val workoutLogRepository: WorkoutLogRepository,
    private val summarizeBodyPartVolume: SummarizeBodyPartVolumeUseCase,
) {
    suspend operator fun invoke(entries: List<WorkoutEntry>, comparedDate: LocalDate): List<BodyPartVolumeComparison> {
        val current = summarizeBodyPartVolume(entries)
        if (current.isEmpty()) return emptyList()

        val comparedEntries = workoutLogRepository
            .observeEntriesInRange(comparedDate, comparedDate)
            .first()[comparedDate]
            .orEmpty()
        val comparedByPart = summarizeBodyPartVolume(comparedEntries).associate { it.bodyPart to it.weightVolumeKg }

        return current.map { volume ->
            BodyPartVolumeComparison(
                bodyPart = volume.bodyPart,
                volumeKg = volume.weightVolumeKg,
                comparedVolumeKg = comparedByPart[volume.bodyPart],
            )
        }
    }
}
