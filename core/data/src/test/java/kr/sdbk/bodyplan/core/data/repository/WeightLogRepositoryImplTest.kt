package kr.sdbk.bodyplan.core.data.repository

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.data.repository.fake.FakeWeightRecordDao
import kr.sdbk.bodyplan.core.local.entity.WeightRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

internal class WeightLogRepositoryImplTest {
    private val date: LocalDate = LocalDate.of(2026, 9, 10)
    private val dao = FakeWeightRecordDao()
    private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC)

    private val repository = WeightLogRepositoryImpl(dao, clock)

    @Test
    fun `저장한 값을 구간 조회로 다시 읽는다`() = runTest {
        repository.save(date, 72.4)

        val records = repository.observeRecordsInRange(date.minusDays(6), date).first()
        assertEquals(1, records.size)
        assertEquals(date, records.first().date)
        assertEquals(72.4, records.first().weightKg, 0.0001)
    }

    @Test
    fun `같은 날짜에 다시 저장하면 행이 늘지 않고 값만 바뀐다`() = runTest {
        repository.save(date, 72.4)
        repository.save(date, 71.9)

        assertEquals(1, dao.stored.size)
        assertEquals(71.9, dao.stored.first().weightKg, 0.0001)
    }

    @Test
    fun `날짜는 epochDay로 담고 고친 시각은 시계에서 읽는다`() = runTest {
        repository.save(date, 72.4)

        val entity = dao.stored.first()
        assertEquals(date.toEpochDay(), entity.dateEpochDay)
        assertEquals(1_000L, entity.updatedAtMillis)
    }

    @Test
    fun `구간 밖의 기록은 담기지 않는다`() = runTest {
        dao.seed(WeightRecordEntity(date.minusDays(10).toEpochDay(), 75.0, 0L))
        dao.seed(WeightRecordEntity(date.toEpochDay(), 72.0, 0L))

        val records = repository.observeRecordsInRange(date.minusDays(6), date).first()
        assertEquals(listOf(date), records.map { it.date })
    }

    @Test
    fun `구간 조회는 오래된 날짜부터 낸다`() = runTest {
        dao.seed(WeightRecordEntity(date.toEpochDay(), 72.0, 0L))
        dao.seed(WeightRecordEntity(date.minusDays(3).toEpochDay(), 73.0, 0L))
        dao.seed(WeightRecordEntity(date.minusDays(1).toEpochDay(), 72.5, 0L))

        val records = repository.observeRecordsInRange(date.minusDays(6), date).first()
        assertEquals(listOf(date.minusDays(3), date.minusDays(1), date), records.map { it.date })
    }

    @Test
    fun `기록이 없는 구간은 빈 목록이다`() = runTest {
        val records = repository.observeRecordsInRange(date.minusDays(6), date).first()

        assertTrue(records.isEmpty())
    }

    @Test
    fun `저장 실패는 삼키지 않고 던진다`() = runTest {
        dao.upsertFailure = IllegalStateException("disk full")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { repository.save(date, 72.4) }
        }
    }
}
