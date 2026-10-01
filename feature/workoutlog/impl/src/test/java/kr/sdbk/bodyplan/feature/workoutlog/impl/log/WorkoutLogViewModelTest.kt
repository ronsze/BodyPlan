package kr.sdbk.bodyplan.feature.workoutlog.impl.log

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Condition
import kr.sdbk.bodyplan.core.domain.model.DayStatus
import kr.sdbk.bodyplan.core.domain.model.Effort
import kr.sdbk.bodyplan.core.domain.model.ExerciseBest
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutMemo
import kr.sdbk.bodyplan.core.domain.model.WorkoutSession
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.model.WorkoutSetKey
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import kr.sdbk.bodyplan.core.domain.usecase.CompareBodyPartVolumeWithDateUseCase
import kr.sdbk.bodyplan.core.domain.usecase.FindPersonalRecordsUseCase
import kr.sdbk.bodyplan.core.domain.usecase.GetMonthlyDayStatusUseCase
import kr.sdbk.bodyplan.core.domain.usecase.IsEditableDateUseCase
import kr.sdbk.bodyplan.core.domain.usecase.ObserveWorkoutLogUseCase
import kr.sdbk.bodyplan.core.domain.usecase.SummarizeBodyPartVolumeUseCase
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutLogNavKey
import kr.sdbk.bodyplan.feature.workoutlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeRoutineRepository
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeWorkoutLogRepository
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeWorkoutSessionController
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
        sessionController: FakeWorkoutSessionController = FakeWorkoutSessionController(),
        comparisonRepository: WorkoutLogRepository = repository,
        monthlyStatusRepository: WorkoutLogRepository = repository,
    ) = WorkoutLogViewModel(
        workoutLogRepository = repository,
        routineRepository = routineRepository,
        isEditableDate = IsEditableDateUseCase(clock),
        observeWorkoutLog = ObserveWorkoutLogUseCase(repository, FindPersonalRecordsUseCase()),
        summarizeBodyPartVolume = SummarizeBodyPartVolumeUseCase(),
        compareBodyPartVolumeWithDate = CompareBodyPartVolumeWithDateUseCase(
            comparisonRepository,
            SummarizeBodyPartVolumeUseCase(),
        ),
        getMonthlyDayStatus = GetMonthlyDayStatusUseCase(
            monthlyStatusRepository,
            IsEditableDateUseCase(clock),
            clock,
        ),
        clock = clock,
        sessionController = sessionController,
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
    fun `날짜를 지정하기 전에는 비교가 비어 있고 비교용 조회도 하지 않는다`() = runTest {
        val yesterday = today.minusDays(1)
        val repository = FakeWorkoutLogRepository(
            initial = listOf(entry(id = 1L)),
            entriesByDate = mapOf(yesterday to listOf(entry(id = 2L))),
        )
        val viewModel = viewModel(repository = repository)

        loaded(viewModel)

        assertNull(viewModel.uiState.value.comparisonDate)
        assertTrue(viewModel.uiState.value.bodyPartVolumeComparisons.isEmpty())
        assertEquals(0, repository.observeEntriesInRangeCallCount)
    }

    @Test
    fun `비교 날짜를 고르면 지정일 기록과 견준 결과가 실리고 선택기가 닫힌다`() = runTest {
        val yesterday = today.minusDays(1)
        val repository = FakeWorkoutLogRepository(
            initial = listOf(entry(id = 1L)),
            entriesByDate = mapOf(yesterday to listOf(entry(id = 2L))),
        )
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)
        viewModel.handleIntent(WorkoutLogIntent.ClickPickComparisonDate)

        viewModel.handleIntent(WorkoutLogIntent.SelectComparisonDate(yesterday))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(yesterday, state.comparisonDate)
        assertFalse(state.isComparisonDatePickerVisible)
        val comparison = state.bodyPartVolumeComparisons.single()
        assertEquals(BodyPart.CHEST, comparison.bodyPart)
        assertEquals(12 * 40, comparison.volumeKg)
        assertEquals(12 * 40, comparison.comparedVolumeKg)
        assertEquals(0, comparison.changeKg)
    }

    @Test
    fun `비교 날짜를 해제하면 날짜가 null이고 비교가 비어 있다`() = runTest {
        val yesterday = today.minusDays(1)
        val repository = FakeWorkoutLogRepository(
            initial = listOf(entry(id = 1L)),
            entriesByDate = mapOf(yesterday to listOf(entry(id = 2L))),
        )
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)
        viewModel.handleIntent(WorkoutLogIntent.SelectComparisonDate(yesterday))
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickPickComparisonDate)
        viewModel.handleIntent(WorkoutLogIntent.ClickClearComparisonDate)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.comparisonDate)
        assertTrue(viewModel.uiState.value.bodyPartVolumeComparisons.isEmpty())
        assertFalse(viewModel.uiState.value.isComparisonDatePickerVisible)
    }

    @Test
    fun `로그 날짜와 같거나 이후 날짜는 비교 날짜로 고를 수 없다`() = runTest {
        val repository = FakeWorkoutLogRepository(listOf(entry(id = 1L)))
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)

        viewModel.handleIntent(WorkoutLogIntent.SelectComparisonDate(today))
        viewModel.handleIntent(WorkoutLogIntent.SelectComparisonDate(today.plusDays(1)))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.comparisonDate)
        assertTrue(viewModel.uiState.value.bodyPartVolumeComparisons.isEmpty())
        assertEquals(0, repository.observeEntriesInRangeCallCount)
    }

    @Test
    fun `비교 조회가 실패해도 기록은 실리고 비교는 비어 있다`() = runTest {
        val repository = FakeWorkoutLogRepository(listOf(entry(id = 1L)))
        val failingComparison = object : WorkoutLogRepository by repository {
            override fun observeEntriesInRange(
                from: LocalDate,
                to: LocalDate,
            ): Flow<Map<LocalDate, List<WorkoutEntry>>> = throw IllegalStateException("비교 조회 실패")
        }
        val viewModel = viewModel(repository = repository, comparisonRepository = failingComparison)
        loaded(viewModel)

        viewModel.handleIntent(WorkoutLogIntent.SelectComparisonDate(today.minusDays(1)))
        advanceUntilIdle()

        assertEquals(listOf(1L), viewModel.uiState.value.entries.map { it.id })
        assertTrue(viewModel.uiState.value.bodyPartVolumeComparisons.isEmpty())
    }

    @Test
    fun `비교 날짜를 고른 뒤 기록이 바뀌면 새 기록으로 비교가 다시 계산된다`() = runTest {
        val yesterday = today.minusDays(1)
        val repository = FakeWorkoutLogRepository(
            initial = listOf(entry(id = 1L), entry(id = 2L)),
            entriesByDate = mapOf(yesterday to listOf(entry(id = 3L))),
        )
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)
        viewModel.handleIntent(WorkoutLogIntent.SelectComparisonDate(yesterday))
        advanceUntilIdle()
        assertEquals(12 * 40, viewModel.uiState.value.bodyPartVolumeComparisons.single().changeKg)

        viewModel.handleIntent(WorkoutLogIntent.ClickDeleteEntry(2L))
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.bodyPartVolumeComparisons.single().changeKg)
    }

    @Test
    fun `비교 날짜 선택기 표시 인텐트는 표시와 숨김을 바꾼다`() = runTest {
        val viewModel = viewModel()
        loaded(viewModel)

        viewModel.handleIntent(WorkoutLogIntent.ClickPickComparisonDate)
        assertTrue(viewModel.uiState.value.isComparisonDatePickerVisible)

        viewModel.handleIntent(WorkoutLogIntent.DismissComparisonDatePicker)
        assertFalse(viewModel.uiState.value.isComparisonDatePickerVisible)
    }

    @Test
    fun `비교 날짜 선택기를 열면 로그 날짜의 달과 그 달의 기록 있는 날이 실린다`() = runTest {
        val recordedDate = LocalDate.of(2026, 9, 3)
        val repository = FakeWorkoutLogRepository(
            bodyPartsByDate = mapOf(
                recordedDate to setOf(BodyPart.CHEST),
                LocalDate.of(2026, 8, 20) to setOf(BodyPart.BACK),
            ),
        )
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)

        viewModel.handleIntent(WorkoutLogIntent.ClickPickComparisonDate)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(YearMonth.of(2026, 9), state.comparisonPickerMonth)
        assertEquals(DayStatus.Recorded(setOf(BodyPart.CHEST)), state.comparisonPickerStatuses[recordedDate])
        assertFalse(state.comparisonPickerStatuses.containsKey(LocalDate.of(2026, 8, 20)))
    }

    @Test
    fun `비교 날짜가 다른 달에 있으면 선택기는 그 달로 열린다`() = runTest {
        val comparisonDate = LocalDate.of(2026, 8, 20)
        val repository = FakeWorkoutLogRepository(
            bodyPartsByDate = mapOf(comparisonDate to setOf(BodyPart.BACK)),
        )
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)
        viewModel.handleIntent(WorkoutLogIntent.SelectComparisonDate(comparisonDate))
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickPickComparisonDate)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(YearMonth.of(2026, 8), state.comparisonPickerMonth)
        assertEquals(DayStatus.Recorded(setOf(BodyPart.BACK)), state.comparisonPickerStatuses[comparisonDate])
    }

    @Test
    fun `선택기의 달을 바꾸면 달과 새 달의 상태가 실린다`() = runTest {
        val augustDate = LocalDate.of(2026, 8, 20)
        val repository = FakeWorkoutLogRepository(
            bodyPartsByDate = mapOf(
                LocalDate.of(2026, 9, 3) to setOf(BodyPart.CHEST),
                augustDate to setOf(BodyPart.BACK),
            ),
        )
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)
        viewModel.handleIntent(WorkoutLogIntent.ClickPickComparisonDate)
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ChangeComparisonPickerMonth(YearMonth.of(2026, 8)))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(YearMonth.of(2026, 8), state.comparisonPickerMonth)
        assertEquals(DayStatus.Recorded(setOf(BodyPart.BACK)), state.comparisonPickerStatuses[augustDate])
        assertFalse(state.comparisonPickerStatuses.containsKey(LocalDate.of(2026, 9, 3)))
    }

    @Test
    fun `달 상태 조회가 실패해도 선택기는 표시되고 상태는 비어 있다`() = runTest {
        val repository = FakeWorkoutLogRepository(listOf(entry(id = 1L)))
        val failingStatus = object : WorkoutLogRepository by repository {
            override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
                flow { throw IllegalStateException("달 상태 조회 실패") }
        }
        val viewModel = viewModel(repository = repository, monthlyStatusRepository = failingStatus)
        loaded(viewModel)

        viewModel.handleIntent(WorkoutLogIntent.ClickPickComparisonDate)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isComparisonDatePickerVisible)
        assertTrue(viewModel.uiState.value.comparisonPickerStatuses.isEmpty())
        assertEquals(listOf(1L), viewModel.uiState.value.entries.map { it.id })
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
        repository.saveMemo(today, WorkoutMemo(text = "오늘은 가슴 운동"))
        val viewModel = viewModel(repository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("오늘은 가슴 운동", viewModel.uiState.value.memo?.text)
    }

    @Test
    fun `편집 가능한 날짜에서 메모 편집을 열면 기존 메모로 채워진다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        repository.saveMemo(today, WorkoutMemo(text = "오늘은 가슴 운동"))
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
        assertEquals(WorkoutMemo(text = "오늘은 하체 운동"), repository.lastSavedMemo)
        assertEquals(WorkoutMemo(text = "오늘은 하체 운동"), viewModel.uiState.value.memo)
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

        assertTrue(viewModel.uiState.value.routines.isEmpty())
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
    fun `조회 전용 날짜에서는 운동 시작 인텐트를 보내도 컨트롤러가 호출되지 않는다`() = runTest {
        val sessionController = FakeWorkoutSessionController()
        val viewModel = viewModel(date = today.minusDays(1), sessionController = sessionController)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        advanceUntilIdle()

        assertEquals(0, sessionController.startCallCount)
        assertNull(viewModel.uiState.value.session)
    }

    @Test
    fun `오늘 화면에서 컨트롤러 세션이 바뀌면 상태의 세션에 그대로 비친다`() = runTest {
        val sessionController = FakeWorkoutSessionController()
        val viewModel = viewModel(sessionController = sessionController)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val session = WorkoutSession(
            elapsedSeconds = 12L,
            isPaused = false,
            completedSets = emptySet(),
            rest = null,
        )
        sessionController.session.value = session
        advanceUntilIdle()

        assertEquals(session, viewModel.uiState.value.session)
    }

    @Test
    fun `어제 화면은 컨트롤러 세션이 있어도 상태의 세션은 계속 null이다`() = runTest {
        val sessionController = FakeWorkoutSessionController()
        val viewModel = viewModel(date = today.minusDays(1), sessionController = sessionController)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        sessionController.session.value = WorkoutSession(
            elapsedSeconds = 10L,
            isPaused = false,
            completedSets = emptySet(),
            rest = null,
        )
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.session)
    }

    @Test
    fun `운동 시작을 누르면 컨트롤러 시작이 위임된다`() = runTest {
        val sessionController = FakeWorkoutSessionController()
        val viewModel = viewModel(sessionController = sessionController)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        advanceUntilIdle()

        assertEquals(1, sessionController.startCallCount)
        assertNotNull(viewModel.uiState.value.session)
    }

    @Test
    fun `이미 세션 중에 운동 시작을 다시 눌러도 컨트롤러가 다시 호출되지 않는다`() = runTest {
        val sessionController = FakeWorkoutSessionController()
        val viewModel = viewModel(sessionController = sessionController)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        advanceUntilIdle()
        viewModel.handleIntent(WorkoutLogIntent.ClickStartSession)
        advanceUntilIdle()

        assertEquals(1, sessionController.startCallCount)
    }

    @Test
    fun `일시정지·재개 인텐트는 컨트롤러로 위임된다`() = runTest {
        val sessionController = FakeWorkoutSessionController()
        val viewModel = viewModel(sessionController = sessionController)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickPauseSession)
        viewModel.handleIntent(WorkoutLogIntent.ClickResumeSession)
        advanceUntilIdle()

        assertEquals(1, sessionController.pauseCallCount)
        assertEquals(1, sessionController.resumeCallCount)
    }

    @Test
    fun `세트 체크·휴식 조정·건너뛰기 인텐트는 컨트롤러로 위임된다`() = runTest {
        val sessionController = FakeWorkoutSessionController()
        val viewModel = viewModel(sessionController = sessionController)
        val key = WorkoutSetKey(entryId = 1L, setIndex = 0)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ToggleSetCompleted(key))
        viewModel.handleIntent(WorkoutLogIntent.ClickAdjustRest(-30))
        viewModel.handleIntent(WorkoutLogIntent.ClickSkipRest)
        advanceUntilIdle()

        assertEquals(listOf(key), sessionController.toggledKeys)
        assertEquals(listOf(-30), sessionController.adjustRestDeltas)
        assertEquals(1, sessionController.skipRestCallCount)
    }

    @Test
    fun `운동 종료를 누르면 경과 분과 볼륨이 담긴 메시지가 나가고 컨트롤러 종료가 호출된다`() = runTest {
        val repository = FakeWorkoutLogRepository(listOf(entry(id = 1L)))
        val sessionController = FakeWorkoutSessionController()
        val viewModel = viewModel(repository = repository, sessionController = sessionController)
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        sessionController.session.value = WorkoutSession(
            elapsedSeconds = 150L,
            isPaused = false,
            completedSets = emptySet(),
            rest = null,
        )
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEndSession)
        advanceUntilIdle()

        assertEquals(1, sessionController.endCallCount)
        assertTrue(
            effects.filterIsInstance<WorkoutLogEffect.ShowMessage>()
                .any { it.message == "운동 종료 · 2분 · 볼륨 480kg" },
        )
    }

    @Test
    fun `세션이 없을 때 운동 종료를 눌러도 컨트롤러가 호출되지 않고 메시지도 없다`() = runTest {
        val sessionController = FakeWorkoutSessionController()
        val viewModel = viewModel(sessionController = sessionController)
        val effects = mutableListOf<WorkoutLogEffect>()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEndSession)
        advanceUntilIdle()

        assertEquals(0, sessionController.endCallCount)
        assertTrue(effects.filterIsInstance<WorkoutLogEffect.ShowMessage>().isEmpty())
    }

    private fun routine(id: Long, entries: List<WorkoutEntry>) = Routine(id = id, name = "루틴$id", entries = entries)

    private fun entry(id: Long) = WorkoutEntry(
        id = id,
        exerciseId = 1L,
        exerciseName = "플랫 벤치프레스 머신",
        bodyPart = BodyPart.CHEST,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40))),
    )

    private suspend fun kotlinx.coroutines.test.TestScope.loaded(vm: WorkoutLogViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { vm.uiState.collect {} }
        advanceUntilIdle()
    }

    @Test
    fun `컨디션을 고르면 즉시 상태와 저장소에 반영된다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)

        viewModel.handleIntent(WorkoutLogIntent.SelectCondition(Condition.GOOD))
        advanceUntilIdle()

        assertEquals(Condition.GOOD, viewModel.uiState.value.memo?.condition)
        assertEquals(WorkoutMemo(condition = Condition.GOOD), repository.lastSavedMemo)
    }

    @Test
    fun `이미 고른 컨디션을 다시 누르면 해제된다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)

        viewModel.handleIntent(WorkoutLogIntent.SelectCondition(Condition.BAD))
        advanceUntilIdle()
        viewModel.handleIntent(WorkoutLogIntent.SelectCondition(Condition.BAD))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.memo)
        assertTrue(repository.lastSavedMemo!!.isEmpty)
    }

    @Test
    fun `다른 컨디션을 누르면 값이 바뀐다`() = runTest {
        val viewModel = viewModel()
        loaded(viewModel)

        viewModel.handleIntent(WorkoutLogIntent.SelectCondition(Condition.BAD))
        advanceUntilIdle()
        viewModel.handleIntent(WorkoutLogIntent.SelectCondition(Condition.NORMAL))
        advanceUntilIdle()

        assertEquals(Condition.NORMAL, viewModel.uiState.value.memo?.condition)
    }

    @Test
    fun `강도를 고르면 즉시 반영되고 같은 값을 다시 누르면 해제된다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)

        viewModel.handleIntent(WorkoutLogIntent.SelectEffort(Effort.HARD))
        advanceUntilIdle()
        assertEquals(Effort.HARD, viewModel.uiState.value.memo?.effort)
        assertEquals(WorkoutMemo(effort = Effort.HARD), repository.lastSavedMemo)

        viewModel.handleIntent(WorkoutLogIntent.SelectEffort(Effort.HARD))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.memo)
    }

    @Test
    fun `통증 부위를 누르면 추가되고 다시 누르면 제거된다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)

        viewModel.handleIntent(WorkoutLogIntent.TogglePainPart(BodyPart.LEG))
        viewModel.handleIntent(WorkoutLogIntent.TogglePainPart(BodyPart.CHEST))
        advanceUntilIdle()
        assertEquals(setOf(BodyPart.LEG, BodyPart.CHEST), viewModel.uiState.value.memo?.painParts)
        assertEquals(setOf(BodyPart.LEG, BodyPart.CHEST), repository.lastSavedMemo?.painParts)

        viewModel.handleIntent(WorkoutLogIntent.TogglePainPart(BodyPart.LEG))
        advanceUntilIdle()
        assertEquals(setOf(BodyPart.CHEST), viewModel.uiState.value.memo?.painParts)
    }

    @Test
    fun `조회 전용 날짜에서는 칩을 눌러도 저장하지 않는다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(date = today.minusDays(2), repository = repository)
        loaded(viewModel)

        viewModel.handleIntent(WorkoutLogIntent.SelectCondition(Condition.GOOD))
        viewModel.handleIntent(WorkoutLogIntent.SelectEffort(Effort.LIGHT))
        viewModel.handleIntent(WorkoutLogIntent.TogglePainPart(BodyPart.LEG))
        advanceUntilIdle()

        assertEquals(0, repository.saveMemoCallCount)
        assertNull(viewModel.uiState.value.memo)
    }

    @Test
    fun `칩 저장이 실패하면 이전 메모로 되돌아가고 메시지가 나간다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository = repository)
        val effects = mutableListOf<WorkoutLogEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        loaded(viewModel)
        viewModel.handleIntent(WorkoutLogIntent.SelectCondition(Condition.GOOD))
        advanceUntilIdle()

        repository.mutateFailure = IllegalStateException("boom")
        viewModel.handleIntent(WorkoutLogIntent.SelectEffort(Effort.HARD))
        advanceUntilIdle()

        assertEquals(WorkoutMemo(condition = Condition.GOOD), viewModel.uiState.value.memo)
        assertTrue(effects.any { it is WorkoutLogEffect.ShowMessage })
    }

    @Test
    fun `칩을 고른 뒤 글을 저장해도 칩 값이 유지된다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)
        viewModel.handleIntent(WorkoutLogIntent.SelectCondition(Condition.GOOD))
        viewModel.handleIntent(WorkoutLogIntent.TogglePainPart(BodyPart.LEG))
        advanceUntilIdle()

        viewModel.handleIntent(WorkoutLogIntent.ClickEditMemo)
        viewModel.handleIntent(WorkoutLogIntent.ChangeMemoInput("  컨디션 좋음  "))
        viewModel.handleIntent(WorkoutLogIntent.ClickSaveMemo)
        advanceUntilIdle()

        val expected = WorkoutMemo(condition = Condition.GOOD, painParts = setOf(BodyPart.LEG), text = "컨디션 좋음")
        assertEquals(expected, repository.lastSavedMemo)
        assertEquals(expected, viewModel.uiState.value.memo)
    }

    @Test
    fun `모든 칩을 해제하고 글이 없으면 메모가 없는 상태가 된다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository = repository)
        loaded(viewModel)

        viewModel.handleIntent(WorkoutLogIntent.SelectCondition(Condition.GOOD))
        viewModel.handleIntent(WorkoutLogIntent.TogglePainPart(BodyPart.LEG))
        advanceUntilIdle()
        viewModel.handleIntent(WorkoutLogIntent.SelectCondition(Condition.GOOD))
        viewModel.handleIntent(WorkoutLogIntent.TogglePainPart(BodyPart.LEG))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.memo)
        assertTrue(repository.lastSavedMemo!!.isEmpty)
    }
}
