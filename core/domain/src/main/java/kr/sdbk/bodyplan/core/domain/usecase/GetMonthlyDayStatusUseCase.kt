package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.DayStatus
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

/** 한 달치 캘린더 셀 상태를 낸다. 그 달의 모든 날짜가 키로 들어간다. */
class GetMonthlyDayStatusUseCase
@Inject
constructor(
    private val workoutLogRepository: WorkoutLogRepository,
    private val isEditableDate: IsEditableDateUseCase,
    private val clock: Clock,
) {
    operator fun invoke(yearMonth: YearMonth): Flow<Map<LocalDate, DayStatus>> {
        // 구독 중 자정을 넘겨도 상태가 흔들리지 않도록 여기서 한 번만 읽고, 모든 판정이 이 값을 쓴다.
        val today = LocalDate.now(clock)
        val from = yearMonth.atDay(1)
        val to = yearMonth.atEndOfMonth()

        return workoutLogRepository.observeBodyPartsInRange(from, to).map { recorded ->
            val dates = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }
            dates.associateWith { date -> resolve(date, recorded[date], today) }
        }
    }

    private fun resolve(date: LocalDate, bodyParts: Set<BodyPart>?, today: LocalDate): DayStatus = when {
        date.isAfter(today) -> DayStatus.Upcoming
        !bodyParts.isNullOrEmpty() -> DayStatus.Recorded(bodyParts)
        isEditableDate(date, today) -> DayStatus.Pending
        else -> DayStatus.Rest
    }
}
