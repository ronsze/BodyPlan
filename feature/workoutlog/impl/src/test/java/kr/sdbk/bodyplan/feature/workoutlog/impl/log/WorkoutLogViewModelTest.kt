package kr.sdbk.bodyplan.feature.workoutlog.impl.log

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.usecase.IsEditableDateUseCase
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutLogNavKey
import kr.sdbk.bodyplan.feature.workoutlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeWorkoutLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class WorkoutLogViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today: LocalDate = LocalDate.of(2026, 9, 7)
    private val clock: Clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun viewModel(date: LocalDate = today, repository: FakeWorkoutLogRepository = FakeWorkoutLogRepository()) =
        WorkoutLogViewModel(
            workoutLogRepository = repository,
            isEditableDate = IsEditableDateUseCase(clock),
            navKey = WorkoutLogNavKey(date.toEpochDay()),
        )

    @Test
    fun `오늘 날짜로 진입하면 편집 가능하다`() = runTest {
        val viewModel = viewModel(date = today)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditable)
    }

    @Test
    fun `어제 날짜로 진입하면 편집 가능하다`() = runTest {
        val viewModel = viewModel(date = today.minusDays(1))

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditable)
    }

    @Test
    fun `그저께 날짜로 진입하면 편집할 수 없다`() = runTest {
        val viewModel = viewModel(date = today.minusDays(2))

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditable)
    }

    @Test
    fun `진입하면 그 날의 기록이 실린다`() = runTest {
        val repository = FakeWorkoutLogRepository(listOf(entry(id = 1L)))
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(listOf(1L), viewModel.uiState.value.entries.map { it.id })
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `항목을 지우면 목록이 즉시 갱신된다`() = runTest {
        val repository = FakeWorkoutLogRepository(listOf(entry(id = 1L), entry(id = 2L)))
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickDeleteEntry(1L))
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.uiState.value.entries.map { it.id })
    }

    @Test
    fun `항목 추가는 entryId 없는 이동을 낸다`() = runTest {
        val viewModel = viewModel()
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickAddEntry)
        advanceUntilIdle()

        val effect = effects.filterIsInstance<WorkoutLogEffect.NavigateToEntryEdit>().single()
        assertEquals(today, effect.date)
        assertNull(effect.entryId)
    }

    @Test
    fun `항목 클릭은 그 항목의 id를 담은 이동을 낸다`() = runTest {
        val repository = FakeWorkoutLogRepository(listOf(entry(id = 7L)))
        val viewModel = viewModel(repository = repository)
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEntry(7L))
        advanceUntilIdle()

        val effect = effects.filterIsInstance<WorkoutLogEffect.NavigateToEntryEdit>().single()
        assertEquals(7L, effect.entryId)
    }

    @Test
    fun `조회가 실패하면 에러가 실리고 재시도가 다시 조회한다`() = runTest {
        val repository = FakeWorkoutLogRepository(listOf(entry(id = 1L)))
        repository.observeFailure = IllegalStateException("boom")
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)

        repository.observeFailure = null
        viewModel.handleIntent(WorkoutLogIntent.ClickRetry)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(1L), viewModel.uiState.value.entries.map { it.id })
    }

    @Test
    fun `삭제가 실패하면 메시지가 나가고 목록은 그대로다`() = runTest {
        val repository = FakeWorkoutLogRepository(listOf(entry(id = 1L)))
        val viewModel = viewModel(repository = repository)
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        repository.mutateFailure = IllegalStateException("boom")
        viewModel.handleIntent(WorkoutLogIntent.ClickDeleteEntry(1L))
        advanceUntilIdle()

        assertTrue(effects.any { it is WorkoutLogEffect.ShowMessage })
        assertEquals(listOf(1L), viewModel.uiState.value.entries.map { it.id })
    }

    private fun entry(id: Long) = WorkoutEntry(
        id = id,
        exerciseId = 1L,
        exerciseName = "플랫 벤치프레스 머신",
        bodyPart = BodyPart.CHEST,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40))),
    )
}
