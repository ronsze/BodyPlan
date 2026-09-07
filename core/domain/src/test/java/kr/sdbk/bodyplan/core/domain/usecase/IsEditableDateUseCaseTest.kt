package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsEditableDateUseCaseTest {
    private val today = LocalDate.of(2026, 9, 7)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val useCase = IsEditableDateUseCase(clock)

    @Test
    fun `오늘은 편집 가능하다`() {
        assertTrue(useCase(today))
    }

    @Test
    fun `어제는 편집 가능하다`() {
        assertTrue(useCase(today.minusDays(1)))
    }

    @Test
    fun `그저께는 편집 불가능하다`() {
        assertFalse(useCase(today.minusDays(2)))
    }

    @Test
    fun `내일은 편집 불가능하다`() {
        assertFalse(useCase(today.plusDays(1)))
    }
}
