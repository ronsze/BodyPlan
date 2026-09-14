package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolume
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

/** 이번 주(월요일부터 오늘까지)의 부위별 무게 볼륨을 낸다. 주의 시작은 분석과 같은 기준을 쓴다. */
class GetWeeklyBodyPartVolumeUseCase
@Inject
constructor(
    private val workoutLogRepository: WorkoutLogRepository,
    private val summarizeBodyPartVolume: SummarizeBodyPartVolumeUseCase,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<BodyPartVolume>> {
        // 구독 중 자정을 넘겨도 구간이 흔들리지 않도록 여기서 한 번만 읽는다.
        val today = LocalDate.now(clock)
        return workoutLogRepository.observeEntriesInRange(AnalysisScopeKey.weekStart(today), today)
            .map { entriesByDate -> summarizeBodyPartVolume(entriesByDate.values.flatten()) }
    }
}
