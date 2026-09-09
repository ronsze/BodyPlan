package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.core.domain.model.WeightTrend
import kr.sdbk.bodyplan.core.domain.repository.WeightLogRepository

/**
 * 체중이 얼마나 움직였는지 낸다.
 *
 * 주·월은 달력 경계가 아니라 오늘에서 뒤로 센 이동 구간이다. 달력 경계를 쓰면 주 초·월 초에
 * 표본이 하루 이틀뿐이라 평균이 크게 흔들린다.
 *
 * 평균은 기록이 있는 날만 더해 그 날 수로 나눈다 — 빠진 날을 0으로 세면 굶은 날이
 * 체중 급감으로 보인다.
 */
class GetWeightTrendUseCase
@Inject
constructor(
    private val weightLogRepository: WeightLogRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<WeightTrend> {
        // 구독 중 자정을 넘겨도 흔들리지 않도록 여기서 한 번만 읽고, 모든 판정이 이 값을 쓴다.
        val today = LocalDate.now(clock)

        return weightLogRepository.observeRecordsInRange(today.minusDays(TREND_DAYS - 1), today)
            .map { records -> trendOf(records, today) }
    }

    private fun trendOf(records: List<WeightRecord>, today: LocalDate): WeightTrend {
        val byDate = records.associate { it.date to it.weightKg }
        return WeightTrend(
            dailyChangeKg = dailyChange(byDate, today),
            weeklyAverageChangeKg = averageChange(byDate, today, WEEK_DAYS),
            monthlyAverageChangeKg = averageChange(byDate, today, MONTH_DAYS),
        )
    }

    private fun dailyChange(byDate: Map<LocalDate, Double>, today: LocalDate): Double? {
        val todayValue = byDate[today] ?: return null
        val yesterdayValue = byDate[today.minusDays(1)] ?: return null
        return todayValue - yesterdayValue
    }

    /** 최근 [days]일 평균에서 그 앞 같은 길이 구간의 평균을 뺀다. 한쪽 구간이 비면 낼 값이 없다. */
    private fun averageChange(byDate: Map<LocalDate, Double>, today: LocalDate, days: Long): Double? {
        val recent = average(byDate, today.minusDays(days - 1), today) ?: return null
        val previous = average(byDate, today.minusDays(days * 2 - 1), today.minusDays(days)) ?: return null
        return recent - previous
    }

    private fun average(byDate: Map<LocalDate, Double>, from: LocalDate, to: LocalDate): Double? {
        val values = byDate.filterKeys { !it.isBefore(from) && !it.isAfter(to) }.values
        return if (values.isEmpty()) null else values.average()
    }
}

private const val WEEK_DAYS = 7L
private const val MONTH_DAYS = 30L

/** 월간 변동이 최근 30일과 그 앞 30일을 견주므로 60일이 필요하다. */
private const val TREND_DAYS = 60L
