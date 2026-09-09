package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetWeightTrendUseCaseTest {
    private val today = LocalDate.of(2026, 9, 10)
    private val useCase = GetWeightTrendUseCase()

    private fun trendOf(vararg records: Pair<LocalDate, Double>) =
        useCase(records.map { WeightRecord(it.first, it.second) }, today)

    private fun daysAgo(days: Long): LocalDate = today.minusDays(days)

    @Test
    fun `오늘과 어제가 모두 있으면 일간 변동은 그 차이다`() {
        val trend = trendOf(today to 72.0, daysAgo(1) to 72.5)

        assertEquals(-0.5, trend.dailyChangeKg!!, DELTA)
    }

    @Test
    fun `오늘 기록이 없으면 일간 변동이 없다`() {
        val trend = trendOf(daysAgo(1) to 72.5, daysAgo(2) to 73.0)

        assertNull(trend.dailyChangeKg)
    }

    @Test
    fun `어제 기록이 없으면 일간 변동이 없다`() {
        val trend = trendOf(today to 72.0, daysAgo(2) to 73.0)

        assertNull(trend.dailyChangeKg)
    }

    @Test
    fun `주간 평균 변동은 최근 7일 평균에서 그 앞 7일 평균을 뺀 값이다`() {
        val trend = trendOf(
            today to 70.0,
            daysAgo(6) to 72.0,
            daysAgo(7) to 74.0,
            daysAgo(13) to 76.0,
        )

        // 최근 7일(오늘~6일 전) 평균 71.0, 그 앞 7일(7~13일 전) 평균 75.0
        assertEquals(-4.0, trend.weeklyAverageChangeKg!!, DELTA)
    }

    @Test
    fun `주간 평균 변동은 앞 구간에 기록이 없으면 없다`() {
        val trend = trendOf(today to 70.0, daysAgo(6) to 72.0)

        assertNull(trend.weeklyAverageChangeKg)
    }

    @Test
    fun `월간 평균 변동은 최근 30일 평균에서 그 앞 30일 평균을 뺀 값이다`() {
        val trend = trendOf(
            today to 70.0,
            daysAgo(29) to 72.0,
            daysAgo(30) to 75.0,
            daysAgo(59) to 79.0,
        )

        // 최근 30일 평균 71.0, 그 앞 30일(30~59일 전) 평균 77.0
        assertEquals(-6.0, trend.monthlyAverageChangeKg!!, DELTA)
    }

    @Test
    fun `60일보다 오래된 기록은 어느 구간에도 들지 않는다`() {
        val trend = trendOf(
            today to 70.0,
            daysAgo(30) to 75.0,
            daysAgo(60) to 90.0,
        )

        // 60일 전은 앞 구간(30~59일 전) 밖이라 평균을 흔들지 않는다.
        assertEquals(-5.0, trend.monthlyAverageChangeKg!!, DELTA)
    }

    @Test
    fun `기록이 하나도 없으면 세 값이 모두 없다`() {
        val trend = trendOf()

        assertNull(trend.dailyChangeKg)
        assertNull(trend.weeklyAverageChangeKg)
        assertNull(trend.monthlyAverageChangeKg)
    }

    @Test
    fun `빠진 날은 0으로 세지 않고 기록이 있는 날만 평균 낸다`() {
        val trend = trendOf(
            today to 70.0,
            daysAgo(1) to 70.0,
            daysAgo(7) to 74.0,
        )

        // 최근 7일에 기록 2건뿐이어도 평균은 70.0이다. 나머지 5일을 0으로 세지 않는다.
        assertEquals(-4.0, trend.weeklyAverageChangeKg!!, DELTA)
    }
}

private const val DELTA = 0.0001
