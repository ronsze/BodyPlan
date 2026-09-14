package kr.sdbk.bodyplan.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutOptionsTest {
    @Test
    fun `각도는 0부터 60까지 15단위다`() {
        assertEquals((0..60 step 15).toList(), WorkoutOptions.angleDegrees)
    }

    @Test
    fun `시간은 5부터 120까지 5단위다`() {
        assertEquals((5..120 step 5).toList(), WorkoutOptions.durationMinutes)
    }

    @Test
    fun `최대 세트 수는 10이다`() {
        assertEquals(10, WorkoutOptions.MAX_SET_COUNT)
    }

    @Test
    fun `무게 범위는 0부터 999까지다`() {
        assertEquals(0, WorkoutOptions.MIN_WEIGHT_KG)
        assertEquals(999, WorkoutOptions.MAX_WEIGHT_KG)
    }

    @Test
    fun `횟수 범위는 1부터 999까지다`() {
        assertEquals(1, WorkoutOptions.MIN_REPEAT_COUNT)
        assertEquals(999, WorkoutOptions.MAX_REPEAT_COUNT)
    }

    @Test
    fun `입력칸은 세 자리까지 받는다`() {
        assertEquals(3, WorkoutOptions.INPUT_MAX_DIGITS)
    }

    @Test
    fun `범위 안의 무게는 유효하다`() {
        assertTrue(WorkoutOptions.isValidWeight(0))
        assertTrue(WorkoutOptions.isValidWeight(999))
        assertTrue(WorkoutOptions.isValidWeight(500))
    }

    @Test
    fun `범위 밖의 무게는 유효하지 않다`() {
        assertFalse(WorkoutOptions.isValidWeight(-1))
        assertFalse(WorkoutOptions.isValidWeight(1000))
    }

    @Test
    fun `범위 안의 횟수는 유효하다`() {
        assertTrue(WorkoutOptions.isValidRepeatCount(1))
        assertTrue(WorkoutOptions.isValidRepeatCount(999))
        assertTrue(WorkoutOptions.isValidRepeatCount(500))
    }

    @Test
    fun `범위 밖의 횟수는 유효하지 않다`() {
        assertFalse(WorkoutOptions.isValidRepeatCount(0))
        assertFalse(WorkoutOptions.isValidRepeatCount(1000))
    }

    @Test
    fun `무게 종목은 기본 강도가 없다`() {
        assertNull(WorkoutOptions.defaultIntensity(IntensityType.WEIGHT))
    }

    @Test
    fun `각도 종목의 기본 강도는 최소 각도다`() {
        assertEquals(Intensity.Angle(0), WorkoutOptions.defaultIntensity(IntensityType.ANGLE))
    }

    @Test
    fun `유산소 종목의 기본 강도는 최소 시간이다`() {
        assertEquals(Intensity.Duration(5), WorkoutOptions.defaultIntensity(IntensityType.DURATION))
    }
}
