package kr.sdbk.bodyplan.feature.dietlog.impl.log

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.DietEntry
import kr.sdbk.bodyplan.core.domain.usecase.IsEditableDateUseCase
import kr.sdbk.bodyplan.feature.dietlog.api.DietLogNavKey
import kr.sdbk.bodyplan.feature.dietlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.dietlog.impl.fake.FakeDietLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class DietLogViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today: LocalDate = LocalDate.of(2026, 9, 7)
    private val clock: Clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun viewModel(date: LocalDate = today, repository: FakeDietLogRepository = FakeDietLogRepository()) =
        DietLogViewModel(
            dietLogRepository = repository,
            isEditableDate = IsEditableDateUseCase(clock),
            navKey = DietLogNavKey(date.toEpochDay()),
        )

    @Test
    fun `오늘과 어제는 편집할 수 있다`() = runTest {
        val todayViewModel = viewModel(date = today)
        val yesterdayViewModel = viewModel(date = today.minusDays(1))

        subscribe(todayViewModel)
        subscribe(yesterdayViewModel)

        assertTrue(todayViewModel.uiState.value.isEditable)
        assertTrue(yesterdayViewModel.uiState.value.isEditable)
    }

    @Test
    fun `그저께는 편집할 수 없다`() = runTest {
        val viewModel = viewModel(date = today.minusDays(2))

        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.isEditable)
    }

    @Test
    fun `진입하면 그 날의 기록이 실린다`() = runTest {
        val repository = FakeDietLogRepository(listOf(entry(1L), entry(2L)))
        val viewModel = viewModel(repository = repository)

        subscribe(viewModel)

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.entries.map { it.id })
    }

    @Test
    fun `항목을 지우면 목록이 즉시 갱신된다`() = runTest {
        val repository = FakeDietLogRepository(listOf(entry(1L), entry(2L)))
        val viewModel = viewModel(repository = repository)
        subscribe(viewModel)

        viewModel.handleIntent(DietLogIntent.ClickDeleteEntry(1L))
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.uiState.value.entries.map { it.id })
    }

    @Test
    fun `추가와 수정이 각각 다른 이동을 낸다`() = runTest {
        val repository = FakeDietLogRepository(listOf(entry(7L)))
        val viewModel = viewModel(repository = repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(DietLogIntent.ClickAddEntry)
        viewModel.handleIntent(DietLogIntent.ClickEntry(7L))
        advanceUntilIdle()

        val navigations = effects.filterIsInstance<DietLogEffect.NavigateToEntryEdit>()
        assertEquals(listOf(null, 7L), navigations.map { it.entryId })
        assertTrue(navigations.all { it.date == today })
    }

    @Test
    fun `사진을 길게 누르면 내보내고 알린다`() = runTest {
        val repository = FakeDietLogRepository(listOf(entry(1L)))
        val viewModel = viewModel(repository = repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(DietLogIntent.LongClickEntry(1L))
        advanceUntilIdle()

        assertEquals(listOf(1L), repository.exportedEntryIds)
        assertTrue(effects.any { it is DietLogEffect.ShowMessage })
    }

    @Test
    fun `내보내기가 실패해도 알린다`() = runTest {
        val repository = FakeDietLogRepository(listOf(entry(1L)))
        val viewModel = viewModel(repository = repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        repository.mutateFailure = IllegalStateException("boom")
        viewModel.handleIntent(DietLogIntent.LongClickEntry(1L))
        advanceUntilIdle()

        assertTrue(effects.any { it is DietLogEffect.ShowMessage })
        assertTrue(repository.exportedEntryIds.isEmpty())
    }

    @Test
    fun `조회가 실패하면 에러가 실리고 재시도가 다시 조회한다`() = runTest {
        val repository = FakeDietLogRepository(listOf(entry(1L)))
        repository.observeFailure = IllegalStateException("boom")
        val viewModel = viewModel(repository = repository)
        subscribe(viewModel)

        assertNotNull(viewModel.uiState.value.errorMessage)

        repository.observeFailure = null
        viewModel.handleIntent(DietLogIntent.ClickRetry)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(1L), viewModel.uiState.value.entries.map { it.id })
    }

    @Test
    fun `삭제가 실패하면 안내하고 목록은 그대로다`() = runTest {
        val repository = FakeDietLogRepository(listOf(entry(1L)))
        val viewModel = viewModel(repository = repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        repository.mutateFailure = IllegalStateException("boom")
        viewModel.handleIntent(DietLogIntent.ClickDeleteEntry(1L))
        advanceUntilIdle()

        assertTrue(effects.any { it is DietLogEffect.ShowMessage })
        assertEquals(listOf(1L), viewModel.uiState.value.entries.map { it.id })
    }

    private fun entry(id: Long) = DietEntry(id = id, imagePath = "/files/diet_images/$id.jpg", memo = null)

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: DietLogViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun kotlinx.coroutines.test.TestScope.collectEffects(viewModel: DietLogViewModel): List<DietLogEffect> {
        val effects = mutableListOf<DietLogEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        return effects
    }
}
