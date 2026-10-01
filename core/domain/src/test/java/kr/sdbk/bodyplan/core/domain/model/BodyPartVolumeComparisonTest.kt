package kr.sdbk.bodyplan.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyPartVolumeComparisonTest {
    @Test
    fun `changeKg는 현재 볼륨에서 견준 볼륨을 뺀 값이다`() {
        assertEquals(200, BodyPartVolumeComparison(BodyPart.CHEST, 1000, 800).changeKg)
        assertEquals(-300, BodyPartVolumeComparison(BodyPart.CHEST, 500, 800).changeKg)
    }

    @Test
    fun `견준 볼륨이 없으면 changeKg도 null이다`() {
        assertNull(BodyPartVolumeComparison(BodyPart.CHEST, 1000, null).changeKg)
    }
}
