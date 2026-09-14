package kr.sdbk.bodyplan.feature.workoutlog.impl.log

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.ExerciseBest
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.usecase.FindPersonalRecordsUseCase
import kr.sdbk.bodyplan.core.domain.usecase.IsEditableDateUseCase
import kr.sdbk.bodyplan.core.domain.usecase.ObserveWorkoutLogUseCase
import kr.sdbk.bodyplan.core.domain.usecase.SummarizeBodyPartVolumeUseCase
import kr.sdbk.bodyplan.core.ui.components.WorkoutSetKey
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

/** 코루틴 가상 시간(`advanceTimeBy`)과 별개로 흐르는 시각. 틱마다 같이 움직여야 경과 시간이 맞는다. */
private class MutableClock(private var current: Instant, private val zoneId: ZoneId = ZoneOffset.UTC) : Clock() {
    override fun getZone(): ZoneId = zoneId
    override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
    override fun instant(): Instant = current
    fun advanceSeconds(seconds: Long) {
        current = current.plusSeconds(seconds)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class WorkoutLogViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today: LocalDate = LocalDate.of(2026, 9, 7)
    private val clock: MutableClock = MutableClock(today.atStartOfDay(ZoneOffset.UTC).toInstant())

    private fun viewModel(
        date: LocalDate = today,
        repository: FakeWorkoutLogRepository = FakeWorkoutLogRepository(),
        routineRepository: FakeRoutineRepository = FakeRoutineRepository(),
    ) = WorkoutLogViewModel(
        workoutLogRepository = repository,
        routineRepository = routineRepository,
        isEditableDate = IsEditableDateUseCase(clock),
        observeWorkoutLog = ObserveWorkoutLogUseCase(repository, FindPersonalRecordsUseCase()),
        summarizeBodyPartVolume = SummarizeBodyPartVolumeUseCase(),
        clock = clock,
        navKey = WorkoutLogNavKey(date.toEpochDay()),
    )

    private fun advanceSeconds(seconds: Long) {
        repeat(seconds.toInt()) {
            clock.advanceSeconds(1)
            mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(1_000)
            mainDispatcherRule.dispatcher.scheduler.runCurrent()
        }
    }

    private fun runSessionJob() = mainDispatcherRule.dispatcher.scheduler.runCurrent()

    /**
     * 세션 중인 채로 테스트를 끝내면 안 된다 — 틱은 `while(true) { delay(1_000); tick() }`로 끝나지 않는 코루틴이라,
     * `runTest`가 종료 시점에 자체적으로 하는 스케줄러 비우기가 영영 끝나지 않는다. 세션을 볼 케이스가 아니면
     * 마지막에 이걸로 틱 Job을 취소한다.
     */
    private fun stopTicking(viewModel: WorkoutLogViewModel) {
        viewModel.handleIntent(WorkoutLogIntent.ClickEndSession)
        runSessionJob()
    }

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
    fun `오늘 날짜로 진입하면 세션을 시작할 수 있다`() = runTest {
        val viewModel = viewModel(date = today)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canStartSession)
    }

    @Test
    fun `어제 날짜로 진입하면 편집은 되지만 세션은 시작할 수 없다`() = runTest {
        val viewModel = viewModel(date = today.minusDays(1))

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditable)
        assertFalse(viewModel.uiState.value.canStartSession)
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
    fun `진입하면 그 날의 기록에서 부위별 볼륨이 계산된다`() = runTest {
        val repository = FakeWorkoutLogRepository(listOf(entry(id = 1L)))
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(
            listOf(BodyPart.CHEST),
            viewModel.uiState.value.bodyPartVolumes.map { it.bodyPart },
        )
        assertEquals(12 * 40, viewModel.uiState.value.bodyPartVolumes.single().weightVolumeKg)
    }

    @Test
    fun `그날 최고값이 지난 최고값을 넘긴 종목은 PR 집합에 담긴다`() = runTest {
        val repository = FakeWorkoutLogRepository(
            listOf(entry(id = 1L)),
            bestBeforeByDate = mapOf(
                today to listOf(
                    ExerciseBest(
                        exerciseId = 1L,
                        intensityType = IntensityType.WEIGHT,
                        maxIntensityValue = 20,
                        maxRepeatCount = 0,
                    ),
                ),
            ),
        )
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(setOf(1L), viewModel.uiState.value.personalRecordExerciseIds)
    }

    @Test
    fun `이전 최고값을 넘기지 못하면 PR 집합이 비어 있다`() = runTest {
        val repository = FakeWorkoutLogRepository(
            listOf(entry(id = 1L)),
            bestBeforeByDate = mapOf(
                today to listOf(
                    ExerciseBest(
                        exerciseId = 1L,
                        intensityType = IntensityType.WEIGHT,
                        maxIntensityValue = 100,
                        maxRepeatCount = 0,
                    ),
                ),
            ),
        )
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.personalRecordExerciseIds.isEmpty())
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
    fun `항목을 지우면 부위별 볼륨도 따라 바뀐다`() = runTest {
        val repository = FakeWorkoutLogRepository(listOf(entry(id = 1L)))
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickDeleteEntry(1L))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.bodyPartVolumes.isEmpty())
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

    @Test
    fun `조회 전용 날짜에서는 운동 시작 인텐트를 보내도 세션이 열리지 않는다`() = runTest {
        val viewModel = viewModel(date = today.minusDays(1))

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.session)
    }

    @Test
    fun `운동 시작을 누르면 세션이 열리고 시작 시각이 지금이다`() = runTest {
        val viewModel = viewModel()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        runSessionJob()

        val session = viewModel.uiState.value.session
        assertNotNull(session)
        assertEquals(clock.millis(), session!!.startedAtMillis)
        assertEquals(0L, session.elapsedSeconds)
        stopTicking(viewModel)
    }

    @Test
    fun `이미 세션 중에 운동 시작을 다시 눌러도 세션이 그대로다`() = runTest {
        val viewModel = viewModel()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        runSessionJob()
        val startedAt = viewModel.uiState.value.session?.startedAtMillis

        advanceSeconds(5)
        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        runSessionJob()

        assertEquals(startedAt, viewModel.uiState.value.session?.startedAtMillis)
        stopTicking(viewModel)
    }

    @Test
    fun `세션 중에는 매초 경과 시간이 늘어난다`() = runTest {
        val viewModel = viewModel()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        runSessionJob()
        advanceSeconds(65)

        assertEquals(65L, viewModel.uiState.value.session?.elapsedSeconds)
        stopTicking(viewModel)
    }

    @Test
    fun `세트를 체크하면 완료 집합에 담기고 휴식이 90초로 시작한다`() = runTest {
        val viewModel = viewModel()
        val key = WorkoutSetKey(entryId = 1L, setIndex = 0)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        viewModel.handleIntent(WorkoutLogIntent.ToggleSetCompleted(key))
        runSessionJob()

        val session = viewModel.uiState.value.session
        assertEquals(setOf(key), session?.completedSets)
        assertEquals(90, session?.rest?.remainingSeconds)
        stopTicking(viewModel)
    }

    @Test
    fun `휴식 중에는 매초 남은 시간이 줄어든다`() = runTest {
        val viewModel = viewModel()
        val key = WorkoutSetKey(entryId = 1L, setIndex = 0)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        viewModel.handleIntent(WorkoutLogIntent.ToggleSetCompleted(key))
        runSessionJob()
        advanceSeconds(10)

        assertEquals(80, viewModel.uiState.value.session?.rest?.remainingSeconds)
        stopTicking(viewModel)
    }

    @Test
    fun `휴식이 0에 닿으면 타이머가 사라지고 진동 이펙트가 한 번 나간다`() = runTest {
        val viewModel = viewModel()
        val key = WorkoutSetKey(entryId = 1L, setIndex = 0)
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        viewModel.handleIntent(WorkoutLogIntent.ToggleSetCompleted(key))
        runSessionJob()
        advanceSeconds(90)

        assertNull(viewModel.uiState.value.session?.rest)
        assertEquals(1, effects.filterIsInstance<WorkoutLogEffect.VibrateRestEnd>().size)
        stopTicking(viewModel)
    }

    @Test
    fun `체크를 끄는 것은 휴식 타이머에 영향이 없다`() = runTest {
        val viewModel = viewModel()
        val key = WorkoutSetKey(entryId = 1L, setIndex = 0)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        viewModel.handleIntent(WorkoutLogIntent.ToggleSetCompleted(key))
        runSessionJob()
        advanceSeconds(10)

        viewModel.handleIntent(WorkoutLogIntent.ToggleSetCompleted(key))
        runSessionJob()

        assertFalse(viewModel.uiState.value.session?.completedSets.orEmpty().contains(key))
        assertEquals(80, viewModel.uiState.value.session?.rest?.remainingSeconds)
        stopTicking(viewModel)
    }

    @Test
    fun `휴식 중 다른 세트를 체크하면 타이머가 90초로 다시 시작한다`() = runTest {
        val viewModel = viewModel()
        val first = WorkoutSetKey(entryId = 1L, setIndex = 0)
        val second = WorkoutSetKey(entryId = 1L, setIndex = 1)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        viewModel.handleIntent(WorkoutLogIntent.ToggleSetCompleted(first))
        runSessionJob()
        advanceSeconds(30)

        viewModel.handleIntent(WorkoutLogIntent.ToggleSetCompleted(second))
        runSessionJob()

        assertEquals(90, viewModel.uiState.value.session?.rest?.remainingSeconds)
        stopTicking(viewModel)
    }

    @Test
    fun `휴식 30초를 줄여 0 이하가 되면 즉시 끝나고 진동은 없다`() = runTest {
        val viewModel = viewModel()
        val key = WorkoutSetKey(entryId = 1L, setIndex = 0)
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        viewModel.handleIntent(WorkoutLogIntent.ToggleSetCompleted(key))
        runSessionJob()

        // 90초에서 -30초를 세 번 주면 0에 닿는다.
        viewModel.handleIntent(WorkoutLogIntent.ClickAdjustRest(-30))
        viewModel.handleIntent(WorkoutLogIntent.ClickAdjustRest(-30))
        viewModel.handleIntent(WorkoutLogIntent.ClickAdjustRest(-30))
        runSessionJob()

        assertNull(viewModel.uiState.value.session?.rest)
        assertTrue(effects.filterIsInstance<WorkoutLogEffect.VibrateRestEnd>().isEmpty())
        stopTicking(viewModel)
    }

    @Test
    fun `휴식 30초 더하기는 제한 없이 누적된다`() = runTest {
        val viewModel = viewModel()
        val key = WorkoutSetKey(entryId = 1L, setIndex = 0)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        viewModel.handleIntent(WorkoutLogIntent.ToggleSetCompleted(key))
        runSessionJob()

        viewModel.handleIntent(WorkoutLogIntent.ClickAdjustRest(30))
        viewModel.handleIntent(WorkoutLogIntent.ClickAdjustRest(30))
        runSessionJob()

        assertEquals(150, viewModel.uiState.value.session?.rest?.remainingSeconds)
        stopTicking(viewModel)
    }

    @Test
    fun `휴식 건너뛰기는 진동 없이 타이머를 끝낸다`() = runTest {
        val viewModel = viewModel()
        val key = WorkoutSetKey(entryId = 1L, setIndex = 0)
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        viewModel.handleIntent(WorkoutLogIntent.ToggleSetCompleted(key))
        runSessionJob()

        viewModel.handleIntent(WorkoutLogIntent.ClickSkipRest)
        runSessionJob()

        assertNull(viewModel.uiState.value.session?.rest)
        assertTrue(effects.filterIsInstance<WorkoutLogEffect.VibrateRestEnd>().isEmpty())
        stopTicking(viewModel)
    }

    @Test
    fun `운동 종료를 누르면 경과 분과 볼륨이 담긴 메시지가 나가고 세션이 사라진다`() = runTest {
        val repository = FakeWorkoutLogRepository(listOf(entry(id = 1L)))
        val viewModel = viewModel(repository = repository)
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        runSessionJob()
        advanceSeconds(120)

        viewModel.handleIntent(WorkoutLogIntent.ClickEndSession)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.session)
        assertTrue(
            effects.filterIsInstance<WorkoutLogEffect.ShowMessage>()
                .any { it.message == "운동 종료 · 2분 · 볼륨 480kg" },
        )
    }

    @Test
    fun `운동 종료 뒤에는 시간이 지나도 상태가 바뀌지 않는다`() = runTest {
        val viewModel = viewModel()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        runSessionJob()
        advanceSeconds(5)
        viewModel.handleIntent(WorkoutLogIntent.ClickEndSession)
        advanceUntilIdle()

        val stateAfterEnd = viewModel.uiState.value
        advanceSeconds(30)

        assertEquals(stateAfterEnd, viewModel.uiState.value)
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
