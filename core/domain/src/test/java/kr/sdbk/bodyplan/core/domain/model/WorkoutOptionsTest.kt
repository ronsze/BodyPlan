package kr.sdbk.bodyplan.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutOptionsTest {
    @Test
    fun `무게는 5부터 100까지 5단위다`() {
        assertEquals((5..100 step 5).toList(), WorkoutOptions.weightKilograms)
    }

    @Test
    fun `각도는 0부터 60까지 15단위다`() {
        assertEquals((0..60 step 15).toList(), WorkoutOptions.angleDegrees)
    }

    @Test
    fun `횟수는 4부터 20까지 4단위다`() {
        assertEquals((4..20 step 4).toList(), WorkoutOptions.repeatCounts)
    }

    @Test
    fun `최대 세트 수는 10이다`() {
        assertEquals(10, WorkoutOptions.MAX_SET_COUNT)
    }

    @Test
    fun `무게 종목의 기본 강도는 최소 무게다`() {
        assertEquals(Intensity.Weight(5), WorkoutOptions.defaultIntensity(IntensityType.WEIGHT))
    }

    @Test
    fun `각도 종목의 기본 강도는 최소 각도다`() {
        assertEquals(Intensity.Angle(0), WorkoutOptions.defaultIntensity(IntensityType.ANGLE))
    }

    @Test
    fun `시간은 5부터 120까지 5단위다`() {
        assertEquals((5..120 step 5).toList(), WorkoutOptions.durationMinutes)
    }

    @Test
    fun `유산소 종목의 기본 강도는 최소 시간이다`() {
        assertEquals(Intensity.Duration(5), WorkoutOptions.defaultIntensity(IntensityType.DURATION))
    }
}
