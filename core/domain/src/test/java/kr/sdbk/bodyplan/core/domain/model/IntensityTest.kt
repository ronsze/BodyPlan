package kr.sdbk.bodyplan.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntensityTest {
    @Test
    fun `of는 무게 종목이면 Weight를 만든다`() {
        assertEquals(Intensity.Weight(60), Intensity.of(IntensityType.WEIGHT, 60))
    }

    @Test
    fun `of는 각도 종목이면 Angle을 만든다`() {
        assertEquals(Intensity.Angle(45), Intensity.of(IntensityType.ANGLE, 45))
    }

    @Test
    fun `of는 유산소 종목이면 Duration을 만든다`() {
        assertEquals(Intensity.Duration(30), Intensity.of(IntensityType.DURATION, 30))
    }

    @Test
    fun `무게와 각도 종목은 횟수를 센다`() {
        assertTrue(IntensityType.WEIGHT.countsRepeats)
        assertTrue(IntensityType.ANGLE.countsRepeats)
    }

    @Test
    fun `유산소 종목은 횟수를 세지 않는다`() {
        assertFalse(IntensityType.DURATION.countsRepeats)
    }
}
