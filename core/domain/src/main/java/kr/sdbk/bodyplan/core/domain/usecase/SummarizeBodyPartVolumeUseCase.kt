package kr.sdbk.bodyplan.core.domain.usecase

import javax.inject.Inject
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolume
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.weightVolumeKg

/**
 * 이미 읽은 기록을 부위별 무게 볼륨으로 줄인다.
 *
 * 무게 종목 기록이 없는 부위는 내지 않는다 — 각도·시간·유산소만 한 부위에 0kg을 적으면
 * "무게 운동을 했는데 0"으로 읽힌다. 순서는 [BodyPart] 선언 순서다.
 */
class SummarizeBodyPartVolumeUseCase
@Inject
constructor() {
    operator fun invoke(entries: List<WorkoutEntry>): List<BodyPartVolume> {
        val weightEntries = entries.filter { it.intensityType == IntensityType.WEIGHT }
        return BodyPart.entries.mapNotNull { bodyPart ->
            val own = weightEntries.filter { it.bodyPart == bodyPart }
            if (own.isEmpty()) null else BodyPartVolume(bodyPart, own.sumOf { it.weightVolumeKg })
        }
    }
}
