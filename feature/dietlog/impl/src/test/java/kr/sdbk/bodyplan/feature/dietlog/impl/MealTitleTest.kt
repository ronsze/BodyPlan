package kr.sdbk.bodyplan.feature.dietlog.impl

import org.junit.Assert.assertEquals
import org.junit.Test

internal class MealTitleTest {
    @Test
    fun `열째까지는 우리말 서수로 읽는다`() {
        assertEquals("첫째 끼니", mealTitle(1))
        assertEquals("다섯째 끼니", mealTitle(5))
        assertEquals("열째 끼니", mealTitle(10))
    }

    @Test
    fun `열한째부터는 숫자로 읽는다`() {
        assertEquals("11번째 끼니", mealTitle(11))
        assertEquals("25번째 끼니", mealTitle(25))
    }
}
