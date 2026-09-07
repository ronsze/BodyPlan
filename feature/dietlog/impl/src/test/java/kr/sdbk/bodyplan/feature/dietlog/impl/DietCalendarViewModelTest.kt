package kr.sdbk.bodyplan.feature.dietlog.impl

import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.feature.dietlog.impl.fake.FakeDietLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class DietCalendarViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today: LocalDate = LocalDate.of(2026, 9, 7)
    private val clock: Clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun viewModel(repository: FakeDietLogRepository) =
        DietCalendarViewModel(dietLogRepository = repository, clock = clock)

    @Test
    fun `진입하면 이번 달과 오늘이 실린다`() = runTest {
        val viewModel = viewModel(FakeDietLogRepository())

        subscribe(viewModel)

        assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.yearMonth)
        assertEquals(today, viewModel.uiState.value.today)
    }

    @Test
    fun `기록이 있는 날만 사진이 실린다`() = runTest {
        val repository = FakeDietLogRepository(
            imagesByDate = mapOf(LocalDate.of(2026, 9, 3) to "/files/diet_images/a.jpg"),
        )
        val viewModel = viewModel(repository)

        subscribe(viewModel)

        assertEquals(
            mapOf(LocalDate.of(2026, 9, 3) to "/files/diet_images/a.jpg"),
            viewModel.uiState.value.imagesByDate,
        )
    }

    @Test
    fun `달을 옮기면 그 달만 읽는다`() = runTest {
        val repository = FakeDietLogRepository(
            imagesByDate = mapOf(
                LocalDate.of(2026, 9, 3) to "/files/diet_images/a.jpg",
                LocalDate.of(2026, 8, 10) to "/files/diet_images/b.jpg",
            ),
        )
        val viewModel = viewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(DietCalendarIntent.ChangeMonth(YearMonth.of(2026, 8)))
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 8), viewModel.uiState.value.yearMonth)
        assertEquals(setOf(LocalDate.of(2026, 8, 10)), viewModel.uiState.value.imagesByDate.keys)
    }

    @Test
    fun `같은 달로 옮기면 다시 읽지 않는다`() = runTest {
        val viewModel = viewModel(FakeDietLogRepository())
        subscribe(viewModel)

        viewModel.handleIntent(DietCalendarIntent.ChangeMonth(YearMonth.of(2026, 9)))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.yearMonth)
    }

    @Test
    fun `날짜를 누르면 그 날짜의 기록으로 이동한다`() = runTest {
        val viewModel = viewModel(FakeDietLogRepository())
        val effects = mutableListOf<DietCalendarEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(DietCalendarIntent.ClickDate(LocalDate.of(2026, 9, 3)))
        advanceUntilIdle()

        val effect = effects.filterIsInstance<DietCalendarEffect.NavigateToLog>().single()
        assertEquals(LocalDate.of(2026, 9, 3), effect.date)
    }

    @Test
    fun `조회가 실패하면 에러가 실리고 재시도가 다시 읽는다`() = runTest {
        val repository = FakeDietLogRepository(
            imagesByDate = mapOf(LocalDate.of(2026, 9, 3) to "/files/diet_images/a.jpg"),
        )
        repository.observeFailure = IllegalStateException("boom")
        val viewModel = viewModel(repository)
        subscribe(viewModel)

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)

        repository.observeFailure = null
        viewModel.handleIntent(DietCalendarIntent.ClickRetry)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.imagesByDate.isNotEmpty())
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: DietCalendarViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }
}
