package kr.sdbk.bodyplan.core.domain.usecase

import javax.inject.Inject
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolumeTrend
import kr.sdbk.bodyplan.core.domain.model.ProgressDirection
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry

/** 두 구간의 기록을 부위별로 견준다. 부위별 셈은 [SummarizeBodyPartVolumeUseCase]가 하고 여기는 짝만 맞춘다. */
class SummarizeBodyPartVolumeTrendUseCase
@Inject
constructor(
    private val summarizeBodyPartVolume: SummarizeBodyPartVolumeUseCase,
) {
    operator fun invoke(recent: List<WorkoutEntry>, previous: List<WorkoutEntry>): List<BodyPartVolumeTrend> {
        val recentByPart = summarizeBodyPartVolume(recent).associate { it.bodyPart to it.weightVolumeKg }
        val previousByPart = summarizeBodyPartVolume(previous).associate { it.bodyPart to it.weightVolumeKg }
        return BodyPart.entries.mapNotNull { bodyPart ->
            val recentVolume = recentByPart[bodyPart]
            val previousVolume = previousByPart[bodyPart]
            if (recentVolume == null && previousVolume == null) return@mapNotNull null
            val change = (recentVolume ?: 0) - (previousVolume ?: 0)
            BodyPartVolumeTrend(
                bodyPart = bodyPart,
                recentVolumeKg = recentVolume ?: 0,
                changeKg = change,
                direction = when {
                    change > 0 -> ProgressDirection.IMPROVING
                    change < 0 -> ProgressDirection.WORSENING
                    else -> ProgressDirection.STEADY
                },
            )
        }
    }
}
