package kr.sdbk.bodyplan.core.data.repository

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kr.sdbk.bodyplan.core.data.repository.fake.FakeAnalysisResultDao
import kr.sdbk.bodyplan.core.data.repository.fake.FakeDietImageStore
import kr.sdbk.bodyplan.core.data.repository.fake.FakeInbodyRecordDao
import kr.sdbk.bodyplan.core.local.entity.AnalysisResultEntity
import kr.sdbk.bodyplan.core.local.entity.InbodyRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

internal class InbodyRecordRepositoryImplTest {
    private val day: LocalDate = LocalDate.of(2026, 9, 30)
    private val recordDao = FakeInbodyRecordDao()
    private val resultDao = FakeAnalysisResultDao()
    private val imageStore = FakeDietImageStore()
    private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC)

    private val repository = InbodyRecordRepositoryImpl(recordDao, resultDao, imageStore, Json, clock)

    private fun record(id: Long, file: String = "a.jpg") = InbodyRecordEntity(
        id = id,
        imageFileName = file,
        measuredOnEpochDay = day.toEpochDay(),
        createdAtMillis = 10L,
    )

    private fun result(kind: String, scopeKey: String) = AnalysisResultEntity(
        kind = kind,
        scopeKey = scopeKey,
        summary = "요약",
        sections = "[]",
        createdAtMillis = 20L,
    )

    @Test
    fun `결과는 기록 번호를 열쇠로 붙고 결과가 없는 기록은 결과가 null이다`() = runTest {
        recordDao.seed(record(1))
        recordDao.seed(record(2))
        resultDao.seed(result("INBODY", "1"))

        val byId = repository.observeRecords().first().associateBy { it.record.id }

        assertNotNull(byId.getValue(1L).result)
        assertNull(byId.getValue(2L).result)
    }

    @Test
    fun `종류가 다르거나 열쇠가 다른 결과는 붙지 않는다`() = runTest {
        recordDao.seed(record(1))
        resultDao.seed(result("DIET_DAILY", "1"))
        resultDao.seed(result("INBODY", "9"))

        assertNull(repository.observeRecords().first().single().result)
    }

    @Test
    fun `기록의 사진 경로는 사진 보관소가 푼 경로다`() = runTest {
        recordDao.seed(record(1, "a.jpg"))

        assertEquals(imageStore.pathOf("a.jpg"), repository.observeRecords().first().single().record.imagePath)
    }

    @Test
    fun `getRecords는 기록과 결과를 함께 낸다`() = runTest {
        recordDao.seed(record(1))
        resultDao.seed(result("INBODY", "1"))

        val records = repository.getRecords()

        assertEquals(1, records.size)
        assertNotNull(records.single().result)
    }

    @Test
    fun `getRecord는 있는 번호는 도메인 모델로 없는 번호는 null로 낸다`() = runTest {
        recordDao.seed(record(1))

        assertEquals(1L, repository.getRecord(1L)?.id)
        assertNull(repository.getRecord(99L))
    }

    @Test
    fun `insert는 측정일과 시계의 현재 시각으로 저장하고 새 번호를 낸다`() = runTest {
        val id = repository.insert("c.jpg", day)

        val row = recordDao.stored.single()
        assertEquals(id, row.id)
        assertEquals(1_000L, row.createdAtMillis)
        assertEquals(day.toEpochDay(), row.measuredOnEpochDay)
    }

    @Test
    fun `실패 사유를 저장하고 null로 지운다`() = runTest {
        recordDao.seed(record(1))

        repository.setFailureReason(1L, "사유")
        assertEquals("사유", recordDao.stored.single().failureReason)

        repository.setFailureReason(1L, null)
        assertNull(recordDao.stored.single().failureReason)
    }
}
