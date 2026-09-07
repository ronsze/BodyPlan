package kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit

import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutOptions
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutEntryEditNavKey
import kr.sdbk.bodyplan.feature.workoutlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeExerciseRepository
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeWorkoutLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class WorkoutEntryEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val date: LocalDate = LocalDate.of(2026, 9, 7)

    private val benchPress = Exercise(1L, BodyPart.CHEST, "플랫 벤치프레스 머신", IntensityType.WEIGHT)
    private val pushUp = Exercise(2L, BodyPart.CHEST, "푸쉬업", IntensityType.ANGLE)
    private val latPullDown = Exercise(3L, BodyPart.BACK, "랫풀다운", IntensityType.WEIGHT)
    private val running = Exercise(4L, BodyPart.CARDIO, "러닝", IntensityType.DURATION)

    private fun viewModel(
        entryId: Long? = null,
        exerciseRepository: FakeExerciseRepository = FakeExerciseRepository(
            listOf(benchPress, pushUp, latPullDown, running),
        ),
        workoutLogRepository: FakeWorkoutLogRepository = FakeWorkoutLogRepository(),
    ) = WorkoutEntryEditViewModel(
        exerciseRepository = exerciseRepository,
        workoutLogRepository = workoutLogRepository,
        navKey = WorkoutEntryEditNavKey(date.toEpochDay(), entryId),
    )

    @Test
    fun `부위를 고르기 전에는 종목 목록이 비어 있다`() = runTest {
        val viewModel = viewModel()

        subscribe(viewModel)

        assertTrue(viewModel.uiState.value.exercises.isEmpty())
        assertNull(viewModel.uiState.value.selectedBodyPart)
    }

    @Test
    fun `부위를 고르면 그 부위의 종목만 실린다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutEntryEditIntent.SelectBodyPart(BodyPart.CHEST))
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.exercises.map { it.id })
    }

    @Test
    fun `무게 종목을 고르면 기본값 세트가 한 줄 생긴다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutEntryEditIntent.SelectBodyPart(BodyPart.CHEST))
        advanceUntilIdle()
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectExercise(benchPress.id))

        val sets = viewModel.uiState.value.sets
        assertEquals(1, sets.size)
        assertEquals(WorkoutOptions.weightKilograms.first(), sets.single().intensityValue)
        assertEquals(WorkoutOptions.repeatCounts.first(), sets.single().repeatCount)
    }

    @Test
    fun `각도 종목을 고르면 각도 기본값으로 세트가 생긴다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutEntryEditIntent.SelectBodyPart(BodyPart.CHEST))
        advanceUntilIdle()
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectExercise(pushUp.id))

        assertEquals(WorkoutOptions.angleDegrees.first(), viewModel.uiState.value.sets.single().intensityValue)
    }

    @Test
    fun `유산소 종목을 고르면 시간 기본값으로 세트가 생긴다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutEntryEditIntent.SelectBodyPart(BodyPart.CARDIO))
        advanceUntilIdle()
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectExercise(running.id))

        assertEquals(WorkoutOptions.durationMinutes.first(), viewModel.uiState.value.sets.single().intensityValue)
    }

    @Test
    fun `세트를 추가하면 앞 세트 값을 물려받지 않고 기본값으로 생긴다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)
        selectBenchPress(viewModel)

        val setId = viewModel.uiState.value.sets.single().id
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectSetIntensity(setId, 45))
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectSetRepeatCount(setId, 8))
        viewModel.handleIntent(WorkoutEntryEditIntent.ClickAddSet)

        val sets = viewModel.uiState.value.sets
        assertEquals(2, sets.size)
        assertEquals(WorkoutOptions.weightKilograms.first(), sets[1].intensityValue)
        assertEquals(WorkoutOptions.repeatCounts.first(), sets[1].repeatCount)
        assertEquals(2, sets.map { it.id }.distinct().size)
        // 앞 세트는 그대로 남는다.
        assertEquals(45, sets[0].intensityValue)
        assertEquals(8, sets[0].repeatCount)
    }

    @Test
    fun `각도 종목은 세트를 더해도 각도 기본값으로 생긴다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectBodyPart(BodyPart.CHEST))
        advanceUntilIdle()
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectExercise(pushUp.id))

        viewModel.handleIntent(WorkoutEntryEditIntent.ClickAddSet)

        assertEquals(
            listOf(WorkoutOptions.angleDegrees.first(), WorkoutOptions.angleDegrees.first()),
            viewModel.uiState.value.sets.map { it.intensityValue },
        )
    }

    @Test
    fun `저장에 성공하면 저장 중 표시가 풀린다`() = runTest {
        val logRepository = FakeWorkoutLogRepository()
        val viewModel = viewModel(workoutLogRepository = logRepository)
        subscribe(viewModel)
        selectBenchPress(viewModel)

        viewModel.handleIntent(WorkoutEntryEditIntent.ClickSave)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `세트는 열 개를 넘지 않는다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)
        selectBenchPress(viewModel)

        repeat(WorkoutOptions.MAX_SET_COUNT + 5) {
            viewModel.handleIntent(WorkoutEntryEditIntent.ClickAddSet)
        }

        assertEquals(WorkoutOptions.MAX_SET_COUNT, viewModel.uiState.value.sets.size)
        assertFalse(viewModel.uiState.value.canAddSet)
    }

    @Test
    fun `세트가 하나면 지워지지 않는다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)
        selectBenchPress(viewModel)

        val setId = viewModel.uiState.value.sets.single().id
        viewModel.handleIntent(WorkoutEntryEditIntent.ClickRemoveSet(setId))

        assertEquals(1, viewModel.uiState.value.sets.size)
        assertFalse(viewModel.uiState.value.canRemoveSet)
    }

    @Test
    fun `세트마다 무게와 횟수를 다르게 둘 수 있다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)
        selectBenchPress(viewModel)

        viewModel.handleIntent(WorkoutEntryEditIntent.ClickAddSet)
        val (first, second) = viewModel.uiState.value.sets
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectSetIntensity(first.id, 40))
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectSetRepeatCount(first.id, 12))
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectSetIntensity(second.id, 60))
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectSetRepeatCount(second.id, 4))

        val sets = viewModel.uiState.value.sets
        assertEquals(listOf(40, 60), sets.map { it.intensityValue })
        assertEquals(listOf(12, 4), sets.map { it.repeatCount })
    }

    @Test
    fun `종목을 바꾸면 세트가 한 줄로 초기화된다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)
        selectBenchPress(viewModel)
        viewModel.handleIntent(WorkoutEntryEditIntent.ClickAddSet)
        assertEquals(2, viewModel.uiState.value.sets.size)

        viewModel.handleIntent(WorkoutEntryEditIntent.SelectExercise(pushUp.id))

        assertEquals(1, viewModel.uiState.value.sets.size)
    }

    @Test
    fun `부위를 바꾸면 고른 종목과 세트가 지워진다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)
        selectBenchPress(viewModel)

        viewModel.handleIntent(WorkoutEntryEditIntent.SelectBodyPart(BodyPart.BACK))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedExercise)
        assertTrue(viewModel.uiState.value.sets.isEmpty())
        assertEquals(listOf(3L), viewModel.uiState.value.exercises.map { it.id })
    }

    @Test
    fun `신규 저장은 addEntry를 부르고 화면을 닫는다`() = runTest {
        val logRepository = FakeWorkoutLogRepository()
        val viewModel = viewModel(workoutLogRepository = logRepository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        selectBenchPress(viewModel)

        viewModel.handleIntent(WorkoutEntryEditIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(1, logRepository.addedCount)
        assertEquals(benchPress, logRepository.lastSavedExercise)
        assertTrue(effects.any { it is WorkoutEntryEditEffect.GoBack })
    }

    @Test
    fun `수정 저장은 updateEntry를 부른다`() = runTest {
        val stored = storedEntry()
        val logRepository = FakeWorkoutLogRepository(listOf(stored))
        val viewModel = viewModel(entryId = stored.id, workoutLogRepository = logRepository)
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutEntryEditIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(stored.id, logRepository.lastUpdatedEntryId)
        assertEquals(0, logRepository.addedCount)
    }

    @Test
    fun `저장이 실패하면 화면이 닫히지 않는다`() = runTest {
        val logRepository = FakeWorkoutLogRepository()
        logRepository.mutateFailure = IllegalStateException("boom")
        val viewModel = viewModel(workoutLogRepository = logRepository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        selectBenchPress(viewModel)

        viewModel.handleIntent(WorkoutEntryEditIntent.ClickSave)
        advanceUntilIdle()

        assertFalse(effects.any { it is WorkoutEntryEditEffect.GoBack })
        assertTrue(effects.any { it is WorkoutEntryEditEffect.ShowMessage })
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `수정 진입이면 저장된 세트가 복원된다`() = runTest {
        val stored = storedEntry()
        val viewModel = viewModel(
            entryId = stored.id,
            workoutLogRepository = FakeWorkoutLogRepository(listOf(stored)),
        )

        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertEquals(BodyPart.CHEST, state.selectedBodyPart)
        assertEquals(benchPress.id, state.selectedExercise?.id)
        assertEquals(listOf(40, 45), state.sets.map { it.intensityValue })
        assertEquals(listOf(12, 8), state.sets.map { it.repeatCount })
    }

    @Test
    fun `지워진 종목의 기록도 스냅샷으로 복원된다`() = runTest {
        val stored = storedEntry().copy(exerciseId = 99L, exerciseName = "사라진 종목")
        val viewModel = viewModel(
            entryId = stored.id,
            workoutLogRepository = FakeWorkoutLogRepository(listOf(stored)),
        )

        subscribe(viewModel)

        val selected = viewModel.uiState.value.selectedExercise
        assertNotNull(selected)
        assertEquals("사라진 종목", selected?.name)
        assertTrue(selected?.isDeleted == true)
        // 목록에 없더라도 화면에 고른 종목이 보여야 한다.
        assertTrue(viewModel.uiState.value.selectableExercises.any { it.id == 99L })
    }

    @Test
    fun `종목을 고르지 않으면 저장하지 않는다`() = runTest {
        val logRepository = FakeWorkoutLogRepository()
        val viewModel = viewModel(workoutLogRepository = logRepository)
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutEntryEditIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(0, logRepository.addedCount)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `종목 조회가 실패하면 에러가 실린다`() = runTest {
        val exerciseRepository = FakeExerciseRepository(listOf(benchPress))
        exerciseRepository.observeFailure = IllegalStateException("boom")
        val viewModel = viewModel(exerciseRepository = exerciseRepository)
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutEntryEditIntent.SelectBodyPart(BodyPart.CHEST))
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: WorkoutEntryEditViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun kotlinx.coroutines.test.TestScope.collectEffects(
        viewModel: WorkoutEntryEditViewModel,
    ): List<WorkoutEntryEditEffect> {
        val effects = mutableListOf<WorkoutEntryEditEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        return effects
    }

    private fun kotlinx.coroutines.test.TestScope.selectBenchPress(viewModel: WorkoutEntryEditViewModel) {
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectBodyPart(BodyPart.CHEST))
        advanceUntilIdle()
        viewModel.handleIntent(WorkoutEntryEditIntent.SelectExercise(benchPress.id))
    }

    private fun storedEntry() = WorkoutEntry(
        id = 5L,
        exerciseId = benchPress.id,
        exerciseName = benchPress.name,
        bodyPart = BodyPart.CHEST,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(
            WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40)),
            WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(45)),
        ),
    )
}
