package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.InbodyRecord
import kr.sdbk.bodyplan.core.domain.model.InbodyRecordWithResult
import kr.sdbk.bodyplan.core.domain.repository.InbodyImageRepository
import kr.sdbk.bodyplan.core.domain.repository.InbodyRecordRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AddInbodyRecordUseCaseTest {
    private val today = LocalDate.of(2026, 9, 10)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val sourceUri = "content://picked/image.jpg"

    private fun useCaseWith(
        imageRepository: FakeInbodyImageRepository = FakeInbodyImageRepository(),
        recordRepository: FakeInbodyRecordRepository = FakeInbodyRecordRepository(),
    ): AddInbodyRecordUseCase = AddInbodyRecordUseCase(
        inbodyImageRepository = imageRepository,
        inbodyRecordRepository = recordRepository,
        clock = clock,
    )

    @Test
    fun `사진과 측정일로 기록이 만들어진다`() = runTest {
        val recordRepository = FakeInbodyRecordRepository(nextId = 42L)
        val useCase = useCaseWith(recordRepository = recordRepository)
        val measuredOn = today.minusDays(3)

        val id = useCase(sourceUri, measuredOn)

        assertEquals(42L, id)
        assertEquals(listOf("saved-$sourceUri" to measuredOn), recordRepository.inserted)
    }

    @Test
    fun `측정일이 오늘이면 그대로 저장한다`() = runTest {
        val recordRepository = FakeInbodyRecordRepository()

        useCaseWith(recordRepository = recordRepository)(sourceUri, today)

        assertEquals(today, recordRepository.inserted.single().second)
    }

    @Test
    fun `측정일이 오늘보다 뒤이면 오늘로 바꿔 저장한다`() = runTest {
        val recordRepository = FakeInbodyRecordRepository()

        useCaseWith(recordRepository = recordRepository)(sourceUri, today.plusDays(1))

        assertEquals(today, recordRepository.inserted.single().second)
    }

    @Test
    fun `같은 측정일에 기록을 둘 만들 수 있다`() = runTest {
        val recordRepository = FakeInbodyRecordRepository()
        val useCase = useCaseWith(recordRepository = recordRepository)

        val first = useCase(sourceUri, today)
        val second = useCase(sourceUri, today)

        assertEquals(2, recordRepository.inserted.size)
        assertTrue(first != second)
    }

    @Test
    fun `기록 저장이 실패하면 복사한 사진을 지운다`() = runTest {
        val imageRepository = FakeInbodyImageRepository()
        val recordRepository = FakeInbodyRecordRepository(insertFailure = IllegalStateException("boom"))
        val useCase = useCaseWith(imageRepository, recordRepository)

        try {
            useCase(sourceUri, today)
            fail("예외가 그대로 나야 한다")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }

        assertEquals(listOf("saved-$sourceUri"), imageRepository.deleted)
    }

    @Test
    fun `사진 복사가 실패하면 기록을 만들지 않는다`() = runTest {
        val imageRepository = FakeInbodyImageRepository(saveFailure = IllegalStateException("copy"))
        val recordRepository = FakeInbodyRecordRepository()
        val useCase = useCaseWith(imageRepository, recordRepository)

        try {
            useCase(sourceUri, today)
            fail("예외가 그대로 나야 한다")
        } catch (e: IllegalStateException) {
            assertEquals("copy", e.message)
        }

        assertTrue(recordRepository.inserted.isEmpty())
    }

    private class FakeInbodyImageRepository(private val saveFailure: Throwable? = null) : InbodyImageRepository {
        val deleted: MutableList<String> = mutableListOf()

        override suspend fun save(sourceUri: String): String {
            saveFailure?.let { throw it }
            return "saved-$sourceUri"
        }

        override suspend fun delete(fileName: String) {
            deleted.add(fileName)
        }

        override fun pathOf(fileName: String): String = "/path/$fileName"
    }

    private class FakeInbodyRecordRepository(
        private var nextId: Long = 1L,
        private val insertFailure: Throwable? = null,
    ) : InbodyRecordRepository {
        val inserted: MutableList<Pair<String, LocalDate>> = mutableListOf()

        override fun observeRecords(): Flow<List<InbodyRecordWithResult>> = error("사용하지 않음")

        override suspend fun getRecords(): List<InbodyRecordWithResult> = error("사용하지 않음")

        override suspend fun getRecord(id: Long): InbodyRecord? = error("사용하지 않음")

        override suspend fun insert(imageFileName: String, measuredOn: LocalDate): Long {
            insertFailure?.let { throw it }
            inserted.add(imageFileName to measuredOn)
            return nextId++
        }

        override suspend fun setFailureReason(id: Long, reason: String?) = error("사용하지 않음")
    }
}
