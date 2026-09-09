package kr.sdbk.bodyplan.feature.my.impl.weight

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class WeightStateTest {
    private val today = LocalDate.of(2026, 9, 10)

    @Test
    fun `editableDates는 오늘과 어제뿐이다`() {
        val state = WeightState(today = today)

        assertEquals(listOf(today, today.minusDays(1)), state.editableDates)
    }

    @Test
    fun `today가 없으면 editableDates가 비어 있다`() {
        assertEquals(emptyList<LocalDate>(), WeightState().editableDates)
    }

    @Test
    fun `canSave는 빈 칸이면 거짓이다`() {
        assertFalse(WeightState(input = "").canSave)
    }

    @Test
    fun `canSave는 숫자가 아니면 거짓이다`() {
        assertFalse(WeightState(input = "abc").canSave)
    }

    @Test
    fun `canSave는 20 미만이면 거짓이다`() {
        assertFalse(WeightState(input = "19.9").canSave)
    }

    @Test
    fun `canSave는 300 초과면 거짓이다`() {
        assertFalse(WeightState(input = "300.1").canSave)
    }

    @Test
    fun `canSave는 정상 범위면 참이다`() {
        assertTrue(WeightState(input = "72.4").canSave)
    }

    @Test
    fun `canSave는 저장 중이면 거짓이다`() {
        assertFalse(WeightState(input = "72.4", isSaving = true).canSave)
    }

    @Test
    fun `recentRecords는 최신순 최대 14개다`() {
        // 저장소가 내는 순서와 같게 오래된 날짜부터 담는다 — recentRecords는 그 전제로 되집는다.
        val records = (0 until 20).map { WeightRecord(today.minusDays((19 - it).toLong()), 70.0 + it) }
        val state = WeightState(records = records)

        val recent = state.recentRecords

        assertEquals(14, recent.size)
        assertEquals(today, recent.first().date)
        assertEquals(today.minusDays(13), recent.last().date)
    }

    @Test
    fun `inputTextOn은 그 날짜 기록이 없으면 빈 칸이다`() {
        val state = WeightState(records = listOf(WeightRecord(today, 72.4)))

        assertEquals("", state.inputTextOn(today.minusDays(1)))
    }

    @Test
    fun `inputTextOn은 그 날짜 기록 값을 돌려준다`() {
        val state = WeightState(records = listOf(WeightRecord(today, 72.4)))

        assertEquals("72.4", state.inputTextOn(today))
    }
}
