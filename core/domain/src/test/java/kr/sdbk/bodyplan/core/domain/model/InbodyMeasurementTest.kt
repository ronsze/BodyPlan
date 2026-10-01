package kr.sdbk.bodyplan.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InbodyMeasurementTest {
    @Test
    fun `체지방률은 체지방량을 체중으로 나눈 백분율이다`() {
        val measurement = InbodyMeasurement(weightKg = 80.0, bodyFatKg = 16.0)

        assertEquals(20.0, measurement.bodyFatPercent!!, 0.0001)
    }

    @Test
    fun `체중이나 체지방량이 없으면 체지방률도 없다`() {
        assertNull(InbodyMeasurement(weightKg = 80.0).bodyFatPercent)
        assertNull(InbodyMeasurement(bodyFatKg = 16.0).bodyFatPercent)
    }

    @Test
    fun `체중이 0이면 체지방률을 내지 않는다`() {
        assertNull(InbodyMeasurement(weightKg = 0.0, bodyFatKg = 1.0).bodyFatPercent)
    }
}
