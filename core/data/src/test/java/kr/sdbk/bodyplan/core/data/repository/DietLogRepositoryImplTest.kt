package kr.sdbk.bodyplan.core.data.repository

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.data.repository.fake.FakeDietEntryDao
import kr.sdbk.bodyplan.core.data.repository.fake.FakeDietImageStore
import kr.sdbk.bodyplan.core.local.entity.DietEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

internal class DietLogRepositoryImplTest {
    private val date: LocalDate = LocalDate.of(2026, 9, 7)
    private val dao = FakeDietEntryDao()
    private val imageStore = FakeDietImageStore()
    private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC)

    private val repository = DietLogRepositoryImpl(dao, imageStore, clock)

    @Test
    fun `사진과 텍스트를 저장하고 다시 읽는다`() = runTest {
        val id = repository.addEntry(date, "content://photo/1", "닭가슴살")

        val entry = repository.getEntry(id)
        assertEquals("닭가슴살", entry?.memo)
        assertEquals("/files/diet_images/image-1.jpg", entry?.imagePath)
        assertEquals(listOf("content://photo/1"), imageStore.savedUris)
    }

    @Test
    fun `텍스트 없이 사진만으로도 저장된다`() = runTest {
        val id = repository.addEntry(date, "content://photo/1", null)

        assertNull(repository.getEntry(id)?.memo)
    }

    @Test
    fun `데이터베이스에는 파일명만 남는다`() = runTest {
        repository.addEntry(date, "content://photo/1", null)

        assertEquals("image-1.jpg", dao.stored.single().imageFileName)
    }

    @Test
    fun `같은 날 항목이 추가한 순서대로 나온다`() = runTest {
        dao.seed(entity(id = 1L, fileName = "a.jpg", createdAtMillis = 20L))
        dao.seed(entity(id = 2L, fileName = "b.jpg", createdAtMillis = 10L))

        val entries = repository.observeLog(date).first().entries

        assertEquals(listOf(2L, 1L), entries.map { it.id })
    }

    @Test
    fun `사진 복사가 실패하면 행이 남지 않고 던진다`() = runTest {
        imageStore.saveFailure = IllegalStateException("복사 실패")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { repository.addEntry(date, "content://photo/1", null) }
        }
        assertTrue(dao.stored.isEmpty())
    }

    @Test
    fun `행 저장이 실패하면 복사한 사진을 지운다`() = runTest {
        dao.insertFailure = IllegalStateException("저장 실패")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { repository.addEntry(date, "content://photo/1", null) }
        }
        assertEquals(listOf("image-1.jpg"), imageStore.deletedFileNames)
    }

    @Test
    fun `내보내기는 그 항목의 사진을 갤러리로 넘긴다`() = runTest {
        val id = dao.seed(entity(id = 0L, fileName = "a.jpg", createdAtMillis = 10L))

        repository.exportEntryImage(id)

        assertEquals(listOf("a.jpg"), imageStore.exportedFileNames)
    }

    @Test
    fun `없는 항목을 내보내려 하면 던진다`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.exportEntryImage(999L) }
        }
    }

    @Test
    fun `항목을 지우면 사진 파일도 지운다`() = runTest {
        val id = dao.seed(entity(id = 0L, fileName = "a.jpg", createdAtMillis = 10L))

        repository.deleteEntry(id)

        assertTrue(dao.stored.isEmpty())
        assertEquals(listOf("a.jpg"), imageStore.deletedFileNames)
    }

    @Test
    fun `사진을 바꾸면 새 파일이 생기고 이전 파일을 지운다`() = runTest {
        val id = dao.seed(entity(id = 0L, fileName = "old.jpg", createdAtMillis = 10L))

        repository.updateEntry(id, "content://photo/2", "수정")

        assertEquals("image-1.jpg", dao.stored.single().imageFileName)
        assertEquals("수정", dao.stored.single().memo)
        assertEquals(listOf("old.jpg"), imageStore.deletedFileNames)
    }

    @Test
    fun `사진을 바꾸지 않으면 파일이 그대로다`() = runTest {
        val id = dao.seed(entity(id = 0L, fileName = "old.jpg", createdAtMillis = 10L))

        repository.updateEntry(id, null, "수정")

        assertEquals("old.jpg", dao.stored.single().imageFileName)
        assertEquals("수정", dao.stored.single().memo)
        assertTrue(imageStore.savedUris.isEmpty())
        assertTrue(imageStore.deletedFileNames.isEmpty())
    }

    @Test
    fun `수정 중 행 갱신이 실패하면 이전 사진을 지우지 않는다`() = runTest {
        val id = dao.seed(entity(id = 0L, fileName = "old.jpg", createdAtMillis = 10L))
        dao.updateFailure = IllegalStateException("갱신 실패")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { repository.updateEntry(id, "content://photo/2", null) }
        }
        assertEquals(listOf("image-1.jpg"), imageStore.deletedFileNames)
    }

    @Test
    fun `캘린더 조회는 날짜마다 첫 사진만 낸다`() = runTest {
        dao.seed(entity(id = 0L, fileName = "first.jpg", createdAtMillis = 10L))
        dao.seed(entity(id = 0L, fileName = "second.jpg", createdAtMillis = 20L))
        dao.seed(entity(id = 0L, fileName = "other.jpg", createdAtMillis = 5L, date = date.plusDays(1)))

        val images = repository.observeFirstImageInRange(date, date.plusDays(2)).first()

        assertEquals(
            mapOf(
                date to "/files/diet_images/first.jpg",
                date.plusDays(1) to "/files/diet_images/other.jpg",
            ),
            images,
        )
    }

    @Test
    fun `기록이 없는 날짜는 캘린더 조회 결과에서 빠진다`() = runTest {
        dao.seed(entity(id = 0L, fileName = "first.jpg", createdAtMillis = 10L))

        val images = repository.observeFirstImageInRange(date.minusDays(3), date).first()

        assertEquals(setOf(date), images.keys)
    }

    private fun entity(
        id: Long,
        fileName: String,
        createdAtMillis: Long,
        date: LocalDate = this.date,
        memo: String? = null,
    ) = DietEntryEntity(
        id = id,
        dateEpochDay = date.toEpochDay(),
        imageFileName = fileName,
        memo = memo,
        createdAtMillis = createdAtMillis,
    )
}
