package kr.sdbk.bodyplan.feature.my.impl.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class WeightFormatTest {
    @Test
    fun `weightText는 소수 한 자리로 표기한다`() {
        assertEquals("72.4", weightText(72.4))
        assertEquals("72.0", weightText(72.0))
    }

    @Test
    fun `weightChangeText는 늘어난 값에 양의 부호를 붙인다`() {
        assertEquals("+0.5kg", weightChangeText(0.5))
    }

    @Test
    fun `weightChangeText는 줄어든 값에 음의 부호를 붙인다`() {
        assertEquals("-0.5kg", weightChangeText(-0.5))
    }

    @Test
    fun `weightChangeText는 값이 없으면 대시다`() {
        assertEquals("-", weightChangeText(null))
    }

    @Test
    fun `isAcceptableWeightInput은 빈 문자열을 받아들인다`() {
        assertTrue(isAcceptableWeightInput(""))
    }

    @Test
    fun `isAcceptableWeightInput은 정수 세 자리에 소수 한 자리까지 받아들인다`() {
        assertTrue(isAcceptableWeightInput("72"))
        assertTrue(isAcceptableWeightInput("72."))
        assertTrue(isAcceptableWeightInput("72.4"))
        assertTrue(isAcceptableWeightInput("123.4"))
    }

    @Test
    fun `isAcceptableWeightInput은 소수 둘째 자리·문자·네 자리 정수를 거부한다`() {
        assertFalse(isAcceptableWeightInput("72.44"))
        assertFalse(isAcceptableWeightInput("abc"))
        assertFalse(isAcceptableWeightInput("1234"))
    }
}
