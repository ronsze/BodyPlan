package kr.sdbk.bodyplan.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSessionTest {
    @Test
    fun `754초는 12분34초로 적힌다`() {
        assertEquals("12:34", clockText(754))
    }

    @Test
    fun `한 시간은 1시간0분0초로 적힌다`() {
        assertEquals("1:00:00", clockText(3_600))
    }

    @Test
    fun `0초는 0분0초로 적힌다`() {
        assertEquals("0:00", clockText(0))
    }
}
