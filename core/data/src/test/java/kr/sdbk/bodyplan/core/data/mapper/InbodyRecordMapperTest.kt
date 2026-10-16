package kr.sdbk.bodyplan.core.data.mapper

import java.time.LocalDate
import kr.sdbk.bodyplan.core.local.entity.InbodyRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InbodyRecordMapperTest {
    private val day = LocalDate.of(2026, 9, 30)

    private fun entity(failureReason: String?) = InbodyRecordEntity(
        id = 3L,
        imageFileName = "a.jpg",
        measuredOnEpochDay = day.toEpochDay(),
        createdAtMillis = 100L,
        failureReason = failureReason,
    )

    @Test
    fun `기록 엔티티는 사진 경로와 측정일과 실패 사유를 담은 도메인 모델이 된다`() {
        val record = entity("실패").toDomain { "/dir/$it" }

        assertEquals(3L, record.id)
        assertEquals("/dir/a.jpg", record.imagePath)
        assertEquals(day, record.measuredOn)
        assertEquals(100L, record.createdAtMillis)
        assertEquals("실패", record.failureReason)
    }

    @Test
    fun `실패 사유가 없으면 도메인 모델도 null이다`() {
        assertNull(entity(null).toDomain { "/dir/$it" }.failureReason)
    }

    @Test
    fun `새 기록 엔티티는 번호 0과 측정일의 epochDay와 시각과 빈 실패 사유를 담는다`() {
        val created = newInbodyRecordEntity("b.jpg", day, 500L)

        assertEquals(0L, created.id)
        assertEquals("b.jpg", created.imageFileName)
        assertEquals(day.toEpochDay(), created.measuredOnEpochDay)
        assertEquals(500L, created.createdAtMillis)
        assertNull(created.failureReason)
    }
}
