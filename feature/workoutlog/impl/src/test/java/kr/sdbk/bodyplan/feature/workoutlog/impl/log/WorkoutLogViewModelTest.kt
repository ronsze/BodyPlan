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
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.usecase.IsEditableDateUseCase
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutLogNavKey
import kr.sdbk.bodyplan.feature.workoutlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeRoutineRepository
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

    private fun viewModel(
        date: LocalDate = today,
        repository: FakeWorkoutLogRepository = FakeWorkoutLogRepository(),
        routineRepository: FakeRoutineRepository = FakeRoutineRepository(),
    ) = WorkoutLogViewModel(
        workoutLogRepository = repository,
        routineRepository = routineRepository,
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

    @Test
    fun `조회 전용 날짜에서는 루틴 불러오기를 눌러도 시트가 열리지 않는다`() = runTest {
        val viewModel = viewModel(date = today.minusDays(2))

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickLoadRoutine)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRoutineSheetVisible)
    }

    @Test
    fun `편집 가능한 날짜에서 루틴 불러오기를 누르면 시트가 열린다`() = runTest {
        val viewModel = viewModel()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickLoadRoutine)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isRoutineSheetVisible)
    }

    @Test
    fun `조회 전용 날짜에서는 루틴을 구독하지 않는다`() = runTest {
        val routineRepository = FakeRoutineRepository(listOf(routine(id = 1L, entries = listOf(entry(id = 10L)))))
        val viewModel = viewModel(date = today.minusDays(2), routineRepository = routineRepository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.routines.isEmpty())
    }

    @Test
    fun `루틴을 고르면 그 루틴의 항목이 기록 뒤에 덧붙고 시트가 닫힌다`() = runTest {
        val routineEntry = entry(id = 10L)
        val routineRepository = FakeRoutineRepository(listOf(routine(id = 1L, entries = listOf(routineEntry))))
        val logRepository = FakeWorkoutLogRepository(listOf(entry(id = 1L)))
        val viewModel = viewModel(repository = logRepository, routineRepository = routineRepository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickLoadRoutine)
        viewModel.handleIntent(WorkoutLogIntent.SelectRoutine(1L))
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.entries.size)
        assertFalse(viewModel.uiState.value.isRoutineSheetVisible)
        assertEquals(listOf(routineEntry), logRepository.lastAddedEntries)
    }

    @Test
    fun `루틴이 없으면 시트에 만든 루틴이 없다는 안내가 보인다`() = runTest {
        val viewModel = viewModel()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.routineSections.isEmpty())
    }

    @Test
    fun `항목이 없는 루틴을 고르면 메시지가 나가고 시트는 유지된다`() = runTest {
        val routineRepository = FakeRoutineRepository(listOf(routine(id = 1L, entries = emptyList())))
        val viewModel = viewModel(routineRepository = routineRepository)
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickLoadRoutine)
        viewModel.handleIntent(WorkoutLogIntent.SelectRoutine(1L))
        advanceUntilIdle()

        assertTrue(
            effects.filterIsInstance<WorkoutLogEffect.ShowMessage>().any { it.message == "루틴에 종목이 없습니다" },
        )
        assertTrue(viewModel.uiState.value.isRoutineSheetVisible)
    }

    @Test
    fun `없는 루틴 id를 고르면 메시지가 나가고 시트는 유지된다`() = runTest {
        val viewModel = viewModel()
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickLoadRoutine)
        viewModel.handleIntent(WorkoutLogIntent.SelectRoutine(999L))
        advanceUntilIdle()

        assertTrue(
            effects.filterIsInstance<WorkoutLogEffect.ShowMessage>().any { it.message == "루틴에 종목이 없습니다" },
        )
        assertTrue(viewModel.uiState.value.isRoutineSheetVisible)
    }

    @Test
    fun `addEntries가 실패하면 메시지가 나가고 시트는 유지된다`() = runTest {
        val routineRepository = FakeRoutineRepository(listOf(routine(id = 1L, entries = listOf(entry(id = 10L)))))
        val logRepository = FakeWorkoutLogRepository()
        logRepository.mutateFailure = IllegalStateException("boom")
        val viewModel = viewModel(repository = logRepository, routineRepository = routineRepository)
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickLoadRoutine)
        viewModel.handleIntent(WorkoutLogIntent.SelectRoutine(1L))
        advanceUntilIdle()

        assertTrue(
            effects.filterIsInstance<WorkoutLogEffect.ShowMessage>().any { it.message == "루틴을 불러오지 못했습니다" },
        )
        assertTrue(viewModel.uiState.value.isRoutineSheetVisible)
    }

    @Test
    fun `시트를 닫는 인텐트는 넣는 중이면 무시된다`() = runTest {
        val routineRepository = FakeRoutineRepository(listOf(routine(id = 1L, entries = listOf(entry(id = 10L)))))
        routineRepository.getRoutineGate = CompletableDeferred()
        val viewModel = viewModel(routineRepository = routineRepository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickLoadRoutine)
        viewModel.handleIntent(WorkoutLogIntent.SelectRoutine(1L))
        // 저장이 끝나기 전(걸쇠에 걸려 멈춘 동안)에는 넣는 중 상태라 닫기를 무시해야 한다.
        assertTrue(viewModel.uiState.value.isApplyingRoutine)
        viewModel.handleIntent(WorkoutLogIntent.DismissRoutineSheet)

        assertTrue(viewModel.uiState.value.isRoutineSheetVisible)

        routineRepository.getRoutineGate?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `루틴 조회가 실패하면 메시지가 한 번 나가고 화면은 에러 상태가 아니다`() = runTest {
        val routineRepository = FakeRoutineRepository()
        routineRepository.observeFailure = IllegalStateException("boom")
        val viewModel = viewModel(routineRepository = routineRepository)
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        assertEquals(1, effects.filterIsInstance<WorkoutLogEffect.ShowMessage>().size)
        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.routines.isEmpty())
    }

    private fun routine(id: Long, entries: List<WorkoutEntry>) =
        Routine(id = id, name = "루틴$id", bodyPart = BodyPart.CHEST, entries = entries)

    private fun entry(id: Long) = WorkoutEntry(
        id = id,
        exerciseId = 1L,
        exerciseName = "플랫 벤치프레스 머신",
        bodyPart = BodyPart.CHEST,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40))),
    )
}
