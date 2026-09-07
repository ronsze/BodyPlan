package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.DietDayStatus
import kr.sdbk.bodyplan.core.domain.repository.DietLogRepository

/** 한 달치 식단 캘린더 셀 상태를 낸다. 그 달의 모든 날짜가 키로 들어간다. */
class GetMonthlyDietStatusUseCase
@Inject
constructor(
    private val dietLogRepository: DietLogRepository,
    private val isEditableDate: IsEditableDateUseCase,
    private val clock: Clock,
) {
    operator fun invoke(yearMonth: YearMonth): Flow<Map<LocalDate, DietDayStatus>> {
        // 구독 중 자정을 넘겨도 흔들리지 않도록 여기서 한 번만 읽고, 모든 판정이 이 값을 쓴다.
        val today = LocalDate.now(clock)
        val from = yearMonth.atDay(1)
        val to = yearMonth.atEndOfMonth()

        return dietLogRepository.observeFirstImageInRange(from, to).map { images ->
            val dates = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }
            dates.associateWith { date -> resolve(date, images[date], today) }
        }
    }

    private fun resolve(date: LocalDate, imagePath: String?, today: LocalDate): DietDayStatus = when {
        date.isAfter(today) -> DietDayStatus.Upcoming
        imagePath != null -> DietDayStatus.Recorded(imagePath)
        isEditableDate(date, today) -> DietDayStatus.Pending
        else -> DietDayStatus.Missed
    }
}
