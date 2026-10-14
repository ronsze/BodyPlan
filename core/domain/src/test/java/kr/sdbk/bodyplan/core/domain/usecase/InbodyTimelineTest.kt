package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InbodyTimelineTest {
    private val sep1 = LocalDate.of(2026, 9, 1)
    private val sep8 = LocalDate.of(2026, 9, 8)
    private val weight = InbodyMeasurement(weightKg = 70.0)

    @Test
    fun `목록은 측정일 내림차순이고 같은 날은 나중에 추가한 기록이 위에 온다`() {
        val entries = listOf(
            inbodyEntry(1, sep1, weight),
            inbodyEntry(2, sep1, weight),
            inbodyEntry(3, sep8, weight),
        )

        val ids = InbodyTimeline.descending(entries).map { it.record.id }

        assertEquals(listOf(3L, 2L, 1L), ids)
    }

    @Test
    fun `가장 최근 측정은 측정 기록 중 측정 순서가 가장 나중인 기록이다`() {
        val entries = listOf(
            inbodyEntry(1, sep1, weight),
            inbodyEntry(2, LocalDate.of(2026, 9, 5), weight),
            inbodyEntry(3, LocalDate.of(2026, 9, 20)),
        )

        assertEquals(2L, InbodyTimeline.latestMeasured(entries)?.record?.id)
    }

    @Test
    fun `측정 기록이 없으면 가장 최근 측정은 없다`() {
        val entries = listOf(inbodyEntry(1, sep1), inbodyEntry(2, sep8))

        assertNull(InbodyTimeline.latestMeasured(entries))
    }

    @Test
    fun `측정일이 같고 번호만 다른 두 기록에서 가장 최근 측정은 번호가 큰 기록이다`() {
        val entries = listOf(inbodyEntry(2, sep1, weight), inbodyEntry(1, sep1, weight))

        assertEquals(2L, InbodyTimeline.latestMeasured(entries)?.record?.id)
    }

    @Test
    fun `이전 측정은 사이에 낀 분석 전 기록을 건너뛴다`() {
        val entries = listOf(
            inbodyEntry(1, sep1, weight),
            inbodyEntry(2, LocalDate.of(2026, 9, 4)),
            inbodyEntry(3, sep8, weight),
        )

        assertEquals(1L, InbodyTimeline.previousMeasured(entries, 3)?.record?.id)
    }

    @Test
    fun `같은 측정일 두 기록에서 나중 기록의 이전 측정은 먼저 추가한 기록이다`() {
        val entries = listOf(inbodyEntry(1, sep1, weight), inbodyEntry(2, sep1, weight))

        assertEquals(1L, InbodyTimeline.previousMeasured(entries, 2)?.record?.id)
    }

    @Test
    fun `앞에 측정 기록이 없으면 이전 측정은 없다`() {
        val entries = listOf(inbodyEntry(1, sep1, weight), inbodyEntry(2, sep8, weight))

        assertNull(InbodyTimeline.previousMeasured(entries, 1))
    }

    @Test
    fun `키만 읽힌 결과도 측정 기록이다`() {
        val entry = inbodyEntry(1, sep1, InbodyMeasurement(heightCm = 175.0))

        assertTrue(InbodyTimeline.isMeasured(entry))
    }
}
