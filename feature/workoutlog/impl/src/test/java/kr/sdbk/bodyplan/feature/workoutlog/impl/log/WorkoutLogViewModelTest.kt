package kr.sdbk.bodyplan.feature.workoutlog.impl.log

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
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

    @Test
    fun `저장된 메모가 있는 날짜를 열면 메모가 실린다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        repository.saveMemo(today, "오늘은 가슴 운동")
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("오늘은 가슴 운동", viewModel.uiState.value.memo)
    }

    @Test
    fun `편집 가능한 날짜에서 메모 편집을 열면 기존 메모로 채워진다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        repository.saveMemo(today, "오늘은 가슴 운동")
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEditMemo)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditingMemo)
        assertEquals("오늘은 가슴 운동", viewModel.uiState.value.memoInput)
    }

    @Test
    fun `메모가 없는 날짜에서 편집을 열면 입력이 빈 문자열이다`() = runTest {
        val viewModel = viewModel()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEditMemo)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditingMemo)
        assertEquals("", viewModel.uiState.value.memoInput)
    }

    @Test
    fun `조회 전용 날짜에서는 메모 편집이 열리지 않는다`() = runTest {
        val viewModel = viewModel(date = today.minusDays(2))

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEditMemo)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditingMemo)
    }

    @Test
    fun `메모 저장에 성공하면 편집이 닫히고 저장소에 원문이 전달되며 메모가 갱신된다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEditMemo)
        viewModel.handleIntent(WorkoutLogIntent.ChangeMemoInput("오늘은 하체 운동"))
        viewModel.handleIntent(WorkoutLogIntent.ClickSaveMemo)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditingMemo)
        assertEquals("오늘은 하체 운동", repository.lastSavedMemo)
        assertEquals("오늘은 하체 운동", viewModel.uiState.value.memo)
    }

    @Test
    fun `공백만 입력해 저장하면 메모가 없는 상태가 된다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEditMemo)
        viewModel.handleIntent(WorkoutLogIntent.ChangeMemoInput("   "))
        viewModel.handleIntent(WorkoutLogIntent.ClickSaveMemo)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.memo)
    }

    @Test
    fun `메모 저장이 실패하면 메시지가 나가고 편집 모드와 입력이 유지된다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository = repository)
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEditMemo)
        viewModel.handleIntent(WorkoutLogIntent.ChangeMemoInput("오늘은 하체 운동"))
        repository.mutateFailure = IllegalStateException("boom")
        viewModel.handleIntent(WorkoutLogIntent.ClickSaveMemo)
        advanceUntilIdle()

        assertTrue(effects.filterIsInstance<WorkoutLogEffect.ShowMessage>().any { it.message == "메모를 저장하지 못했습니다" })
        assertTrue(viewModel.uiState.value.isEditingMemo)
        assertEquals("오늘은 하체 운동", viewModel.uiState.value.memoInput)
        assertFalse(viewModel.uiState.value.isSavingMemo)
    }

    @Test
    fun `메모 편집을 취소하면 저장 없이 편집이 닫히고 입력이 비워진다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEditMemo)
        viewModel.handleIntent(WorkoutLogIntent.ChangeMemoInput("쓰다가 만 메모"))
        viewModel.handleIntent(WorkoutLogIntent.ClickCancelMemo)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditingMemo)
        assertEquals("", viewModel.uiState.value.memoInput)
        assertNull(repository.lastSavedMemo)
    }

    @Test
    fun `메모 입력이 500자를 넘기면 500자로 잘린다`() = runTest {
        val viewModel = viewModel()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEditMemo)
        viewModel.handleIntent(WorkoutLogIntent.ChangeMemoInput("가".repeat(600)))
        advanceUntilIdle()

        assertEquals(500, viewModel.uiState.value.memoInput.length)
    }

    @Test
    fun `저장 중에 저장 인텐트를 다시 보내도 저장소는 한 번만 호출된다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        repository.saveMemoGate = CompletableDeferred()
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEditMemo)
        viewModel.handleIntent(WorkoutLogIntent.ChangeMemoInput("오늘은 등 운동"))
        viewModel.handleIntent(WorkoutLogIntent.ClickSaveMemo)
        // 첫 저장이 걸쇠에 걸려 isSavingMemo가 true인 채로 멈춰 있을 때 두 번째 인텐트를 보낸다.
        assertTrue(viewModel.uiState.value.isSavingMemo)
        viewModel.handleIntent(WorkoutLogIntent.ClickSaveMemo)

        assertEquals(1, repository.saveMemoCallCount)

        repository.saveMemoGate?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `조회 전용 날짜에서는 저장 인텐트를 보내도 저장소가 호출되지 않는다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(date = today.minusDays(2), repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickSaveMemo)
        advanceUntilIdle()

        assertEquals(0, repository.saveMemoCallCount)
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
