package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.core.domain.repository.WeightLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetWeightTrendUseCaseTest {
    private val today = LocalDate.of(2026, 9, 10)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun useCaseWith(vararg records: Pair<LocalDate, Double>): GetWeightTrendUseCase = GetWeightTrendUseCase(
        weightLogRepository = FakeWeightLogRepository(records.map { WeightRecord(it.first, it.second) }),
        clock = clock,
    )

    private fun daysAgo(days: Long): LocalDate = today.minusDays(days)

    @Test
    fun `오늘과 어제가 모두 있으면 일간 변동은 그 차이다`() = runTest {
        val trend = useCaseWith(today to 72.0, daysAgo(1) to 72.5)().first()

        assertEquals(-0.5, trend.dailyChangeKg!!, DELTA)
    }

    @Test
    fun `오늘 기록이 없으면 일간 변동이 없다`() = runTest {
        val trend = useCaseWith(daysAgo(1) to 72.5, daysAgo(2) to 73.0)().first()

        assertNull(trend.dailyChangeKg)
    }

    @Test
    fun `어제 기록이 없으면 일간 변동이 없다`() = runTest {
        val trend = useCaseWith(today to 72.0, daysAgo(2) to 73.0)().first()

        assertNull(trend.dailyChangeKg)
    }

    @Test
    fun `주간 평균 변동은 최근 7일 평균에서 그 앞 7일 평균을 뺀 값이다`() = runTest {
        val trend = useCaseWith(
            today to 70.0,
            daysAgo(6) to 72.0,
            daysAgo(7) to 74.0,
            daysAgo(13) to 76.0,
        )().first()

        // 최근 7일(오늘~6일 전) 평균 71.0, 그 앞 7일(7~13일 전) 평균 75.0
        assertEquals(-4.0, trend.weeklyAverageChangeKg!!, DELTA)
    }

    @Test
    fun `주간 평균 변동은 앞 구간에 기록이 없으면 없다`() = runTest {
        val trend = useCaseWith(today to 70.0, daysAgo(6) to 72.0)().first()

        assertNull(trend.weeklyAverageChangeKg)
    }

    @Test
    fun `월간 평균 변동은 최근 30일 평균에서 그 앞 30일 평균을 뺀 값이다`() = runTest {
        val trend = useCaseWith(
            today to 70.0,
            daysAgo(29) to 72.0,
            daysAgo(30) to 75.0,
            daysAgo(59) to 79.0,
        )().first()

        // 최근 30일 평균 71.0, 그 앞 30일(30~59일 전) 평균 77.0
        assertEquals(-6.0, trend.monthlyAverageChangeKg!!, DELTA)
    }

    @Test
    fun `60일보다 오래된 기록은 조회 구간에 들어오지 않는다`() = runTest {
        val useCase = GetWeightTrendUseCase(
            weightLogRepository = FakeWeightLogRepository(
                listOf(
                    WeightRecord(today, 70.0),
                    WeightRecord(daysAgo(30), 75.0),
                    WeightRecord(daysAgo(60), 90.0),
                ),
            ),
            clock = clock,
        )

        useCase().first()

        // 60일 전은 최근 60일 구간(오늘~59일 전) 밖이다.
        assertEquals(daysAgo(59), FakeWeightLogRepository.lastRequestedFrom)
        assertEquals(today, FakeWeightLogRepository.lastRequestedTo)
    }

    @Test
    fun `기록이 하나도 없으면 세 값이 모두 없다`() = runTest {
        val trend = useCaseWith()().first()

        assertNull(trend.dailyChangeKg)
        assertNull(trend.weeklyAverageChangeKg)
        assertNull(trend.monthlyAverageChangeKg)
    }

    @Test
    fun `빠진 날은 0으로 세지 않고 기록이 있는 날만 평균 낸다`() = runTest {
        val trend = useCaseWith(
            today to 70.0,
            daysAgo(1) to 70.0,
            daysAgo(7) to 74.0,
        )().first()

        // 최근 7일에 기록 2건뿐이어도 평균은 70.0이다. 나머지 5일을 0으로 세지 않는다.
        assertEquals(-4.0, trend.weeklyAverageChangeKg!!, DELTA)
    }
}

private const val DELTA = 0.0001

private class FakeWeightLogRepository(private val records: List<WeightRecord>) : WeightLogRepository {
    override fun observeRecordsInRange(from: LocalDate, to: LocalDate): Flow<List<WeightRecord>> {
        lastRequestedFrom = from
        lastRequestedTo = to
        return flowOf(records.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }.sortedBy { it.date })
    }

    override suspend fun save(date: LocalDate, weightKg: Double) = Unit

    /** 조회 구간을 검증하려고 남긴다. 테스트마다 새 인스턴스를 쓰므로 마지막 호출만 보면 된다. */
    companion object {
        var lastRequestedFrom: LocalDate? = null
        var lastRequestedTo: LocalDate? = null
    }
}
