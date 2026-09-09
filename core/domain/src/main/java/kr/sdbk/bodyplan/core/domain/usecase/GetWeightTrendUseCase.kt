package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import javax.inject.Inject
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.core.domain.model.WeightTrend

/**
 * 체중이 얼마나 움직였는지 낸다.
 *
 * 저장소를 스스로 구독하지 않고 이미 읽은 [records]를 받는다. 스스로 구독하면 화면이 목록에 쓰는
 * 구독과 같은 구간을 두 번 열게 되고, 조회가 실패하면 같은 문구가 두 번 뜬다.
 * [today]도 호출부가 읽은 것을 받아, 목록과 변동이 같은 기준일을 쓴다.
 *
 * [records]는 `today`부터 59일 전까지를 담아야 한다 — 월간 변동이 최근 30일과 그 앞 30일을 견준다.
 * 그보다 짧으면 앞 구간이 비어 월간 값이 나오지 않는다.
 *
 * 주·월은 달력 경계가 아니라 오늘에서 뒤로 센 이동 구간이다. 달력 경계를 쓰면 주 초·월 초에
 * 표본이 하루 이틀뿐이라 평균이 크게 흔들린다.
 *
 * 평균은 기록이 있는 날만 더해 그 날 수로 나눈다 — 빠진 날을 0으로 세면 기록을 걸러 쓴 날이
 * 체중 급감으로 보인다.
 */
class GetWeightTrendUseCase
@Inject
constructor() {
    operator fun invoke(records: List<WeightRecord>, today: LocalDate): WeightTrend {
        val byDate = records.associate { it.date to it.weightKg }
        return WeightTrend(
            dailyChangeKg = dailyChange(byDate, today),
            weeklyAverageChangeKg = averageChange(byDate, today, WEEK_DAYS),
            monthlyAverageChangeKg = averageChange(byDate, today, MONTH_DAYS),
            recentWeeklyAverageKg = average(byDate, today.minusDays(WEEK_DAYS - 1), today),
            previousWeeklyAverageKg = average(
                byDate,
                today.minusDays(WEEK_DAYS * 2 - 1),
                today.minusDays(WEEK_DAYS),
            ),
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
