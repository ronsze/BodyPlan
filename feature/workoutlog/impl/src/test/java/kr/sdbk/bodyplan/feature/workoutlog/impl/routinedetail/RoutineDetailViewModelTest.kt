package kr.sdbk.bodyplan.feature.workoutlog.impl.routinedetail

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.feature.workoutlog.api.RoutineDetailNavKey
import kr.sdbk.bodyplan.feature.workoutlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeRoutineRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class RoutineDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun entry(id: Long) = WorkoutEntry(
        id = id,
        exerciseId = 1L,
        exerciseName = "스쿼트",
        bodyPart = BodyPart.LEG,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40))),
    )

    private fun routine(id: Long = 1L, entries: List<WorkoutEntry> = emptyList()) =
        Routine(id = id, name = "하체 루틴", bodyPart = BodyPart.LEG, entries = entries)

    private fun viewModel(routineId: Long = 1L, repository: FakeRoutineRepository = FakeRoutineRepository()) =
        RoutineDetailViewModel(routineRepository = repository, navKey = RoutineDetailNavKey(routineId))

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: RoutineDetailViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun kotlinx.coroutines.test.TestScope.collectEffects(
        viewModel: RoutineDetailViewModel,
    ): List<RoutineDetailEffect> {
        val effects = mutableListOf<RoutineDetailEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        return effects
    }

    @Test
    fun `상단 제목과 부위는 루틴에서 온다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine()))
        val viewModel = viewModel(repository = repository)

        subscribe(viewModel)

        assertEquals("하체 루틴", viewModel.uiState.value.routine?.name)
        assertEquals(BodyPart.LEG, viewModel.uiState.value.routine?.bodyPart)
    }

    @Test
    fun `운동 추가를 누르면 루틴 대상 편집 이동을 낸다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine()))
        val viewModel = viewModel(repository = repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(RoutineDetailIntent.ClickAddEntry)
        advanceUntilIdle()

        val effect = effects.filterIsInstance<RoutineDetailEffect.NavigateToEntryEdit>().single()
        assertEquals(1L, effect.routineId)
        assertNull(effect.entryId)
    }

    @Test
    fun `항목 카드를 누르면 그 항목의 편집 이동을 낸다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine(entries = listOf(entry(10L)))))
        val viewModel = viewModel(repository = repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(RoutineDetailIntent.ClickEntry(10L))
        advanceUntilIdle()

        val effect = effects.filterIsInstance<RoutineDetailEffect.NavigateToEntryEdit>().single()
        assertEquals(10L, effect.entryId)
    }

    @Test
    fun `삭제를 누르면 항목이 지워진다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine(entries = listOf(entry(10L)))))
        val viewModel = viewModel(repository = repository)
        subscribe(viewModel)

        viewModel.handleIntent(RoutineDetailIntent.ClickDeleteEntry(10L))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.routine?.entries.orEmpty().isEmpty())
    }

    @Test
    fun `삭제가 실패하면 메시지가 나간다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine(entries = listOf(entry(10L)))))
        repository.mutateFailure = IllegalStateException("boom")
        val viewModel = viewModel(repository = repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(RoutineDetailIntent.ClickDeleteEntry(10L))
        advanceUntilIdle()

        assertTrue(
            effects.filterIsInstance<RoutineDetailEffect.ShowMessage>().any { it.message == "삭제하지 못했습니다" },
        )
    }

    @Test
    fun `항목이 없으면 목록이 비어 있다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine(entries = emptyList())))
        val viewModel = viewModel(repository = repository)

        subscribe(viewModel)

        assertTrue(viewModel.uiState.value.routine?.entries.orEmpty().isEmpty())
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `조회가 실패하면 에러가 실린다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine()))
        repository.observeFailure = IllegalStateException("boom")
        val viewModel = viewModel(repository = repository)

        subscribe(viewModel)

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `없는 루틴이면 에러가 실린다`() = runTest {
        val repository = FakeRoutineRepository()
        val viewModel = viewModel(routineId = 999L, repository = repository)

        subscribe(viewModel)

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.routine)
    }

    @Test
    fun `조회 실패 뒤 재시도가 성공하면 에러가 걷힌다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine()))
        repository.observeFailure = IllegalStateException("boom")
        val viewModel = viewModel(repository = repository)

        subscribe(viewModel)
        assertNotNull(viewModel.uiState.value.errorMessage)

        repository.observeFailure = null
        viewModel.handleIntent(RoutineDetailIntent.ClickRetry)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("하체 루틴", viewModel.uiState.value.routine?.name)
    }

    @Test
    fun `뒤로가기 인텐트는 GoBack을 낸다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine()))
        val viewModel = viewModel(repository = repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(RoutineDetailIntent.ClickBack)
        advanceUntilIdle()

        assertTrue(effects.any { it is RoutineDetailEffect.GoBack })
    }
}
