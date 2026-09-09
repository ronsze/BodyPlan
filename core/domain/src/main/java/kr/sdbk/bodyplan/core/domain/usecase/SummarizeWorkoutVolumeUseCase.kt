package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import javax.inject.Inject
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutVolume

/**
 * 이미 읽은 기록을 한 기간의 운동량으로 줄인다.
 *
 * 저장소를 스스로 구독하지 않고 받는다 — 같은 구간을 두 번 열지 않게 하려는 것이고,
 * 종목별 추이처럼 다른 화면이 같은 셈을 쓸 수 있게 하려는 것이다([GetWeightTrendUseCase]와 같은 짜임새).
 */
class SummarizeWorkoutVolumeUseCase
@Inject
constructor() {
    operator fun invoke(entriesByDate: Map<LocalDate, List<WorkoutEntry>>): WorkoutVolume = WorkoutVolume(
        weightVolumeKg = entriesByDate.values.sumOf { entries -> entries.sumOf { it.weightVolume() } },
        // 기록이 빈 목록으로 들어온 날짜는 운동한 날로 세지 않는다.
        workoutDays = entriesByDate.count { (_, entries) -> entries.isNotEmpty() },
    )

    private fun WorkoutEntry.weightVolume(): Int = if (intensityType != IntensityType.WEIGHT) {
        0
    } else {
        sets.sumOf { it.intensity.value * it.repeatCount }
    }
}
