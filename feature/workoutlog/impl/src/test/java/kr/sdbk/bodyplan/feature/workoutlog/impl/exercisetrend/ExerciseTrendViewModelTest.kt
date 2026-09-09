package kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend

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
import kr.sdbk.bodyplan.core.domain.usecase.GetExerciseTrendUseCase
import kr.sdbk.bodyplan.core.domain.usecase.SummarizeExerciseTrendUseCase
import kr.sdbk.bodyplan.feature.workoutlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeWorkoutLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class ExerciseTrendViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today: LocalDate = LocalDate.of(2026, 9, 7)
    private val clock: Clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private val benchPress = entry(
        id = 1L,
        exerciseId = 1L,
        exerciseName = "벤치프레스",
        bodyPart = BodyPart.CHEST,
        weight = 100,
        repeatCount = 10,
    )
    private val squat = entry(
        id = 2L,
        exerciseId = 2L,
        exerciseName = "스쿼트",
        bodyPart = BodyPart.LEG,
        weight = 80,
        repeatCount = 8,
    )

    private fun viewModel(repository: FakeWorkoutLogRepository) = ExerciseTrendViewModel(
        getExerciseTrend = GetExerciseTrendUseCase(
            workoutLogRepository = repository,
            summarizeExerciseTrend = SummarizeExerciseTrendUseCase(),
            clock = clock,
        ),
    )

    private fun repository(entriesByDate: Map<LocalDate, List<WorkoutEntry>>): FakeWorkoutLogRepository =
        FakeWorkoutLogRepository(entriesByDate = entriesByDate)

    @Test
    fun `구독하면 종목이 실리고 선택이 없어 추이는 비어 있다`() = runTest {
        val repository = repository(
            mapOf(today to listOf(benchPress), today.minusDays(6) to listOf(squat)),
        )
        val viewModel = viewModel(repository)

        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.exercises.size)
        assertNull(state.selectedExerciseId)
        assertNull(state.trend)
    }

    @Test
    fun `부위를 고르면 선택과 추이가 되돌아간다`() = runTest {
        val repository = repository(mapOf(today to listOf(benchPress)))
        val viewModel = viewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseTrendIntent.SelectExercise(1L))
        advanceUntilIdle()
        assertEquals(1L, viewModel.uiState.value.selectedExerciseId)

        viewModel.handleIntent(ExerciseTrendIntent.SelectBodyPart(BodyPart.LEG))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(BodyPart.LEG, state.selectedBodyPart)
        assertNull(state.selectedExerciseId)
        assertNull(state.trend)
    }

    @Test
    fun `같은 부위를 다시 고르면 아무것도 바뀌지 않는다`() = runTest {
        val repository = repository(mapOf(today to listOf(benchPress)))
        val viewModel = viewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseTrendIntent.SelectBodyPart(BodyPart.CHEST))
        advanceUntilIdle()
        viewModel.handleIntent(ExerciseTrendIntent.SelectExercise(1L))
        advanceUntilIdle()

        viewModel.handleIntent(ExerciseTrendIntent.SelectBodyPart(BodyPart.CHEST))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(BodyPart.CHEST, state.selectedBodyPart)
        assertEquals(1L, state.selectedExerciseId)
        assertEquals(1L, state.trend?.exercise?.id)
    }

    @Test
    fun `종목을 고르면 그 종목의 추이가 채워진다`() = runTest {
        val repository = repository(mapOf(today to listOf(benchPress)))
        val viewModel = viewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseTrendIntent.SelectExercise(1L))
        advanceUntilIdle()

        val trend = viewModel.uiState.value.trend
        assertEquals(1L, trend?.exercise?.id)
        assertTrue(trend!!.points.any { it.values.isNotEmpty() })
    }

    @Test
    fun `같은 종목을 다시 고르면 재구독하지 않는다`() = runTest {
        val repository = repository(mapOf(today to listOf(benchPress)))
        val viewModel = viewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseTrendIntent.SelectExercise(1L))
        advanceUntilIdle()
        val countAfterFirst = repository.observeEntriesInRangeCallCount

        viewModel.handleIntent(ExerciseTrendIntent.SelectExercise(1L))
        advanceUntilIdle()

        assertEquals(countAfterFirst, repository.observeEntriesInRangeCallCount)
    }

    @Test
    fun `selectableExercises는 고른 부위로 거른다`() = runTest {
        val repository = repository(mapOf(today to listOf(benchPress, squat)))
        val viewModel = viewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseTrendIntent.SelectBodyPart(BodyPart.CHEST))
        advanceUntilIdle()

        val selectable = viewModel.uiState.value.selectableExercises
        assertEquals(1, selectable.size)
        assertEquals(1L, selectable.single().id)
    }

    @Test
    fun `뒤로가기를 누르면 이동 이펙트가 난다`() = runTest {
        val viewModel = viewModel(repository(emptyMap()))
        val effects = mutableListOf<ExerciseTrendEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseTrendIntent.ClickBack)
        advanceUntilIdle()

        assertTrue(effects.contains(ExerciseTrendEffect.GoBack))
    }

    @Test
    fun `구독이 실패하면 에러가 실린다`() = runTest {
        val repository = repository(emptyMap())
        repository.observeFailure = IllegalStateException("boom")
        val viewModel = viewModel(repository)

        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertEquals("불러오지 못했습니다", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `실패 뒤 재시도하면 에러가 지워지고 다시 채워진다`() = runTest {
        val repository = repository(mapOf(today to listOf(benchPress)))
        repository.observeFailure = IllegalStateException("boom")
        val viewModel = viewModel(repository)
        subscribe(viewModel)

        repository.observeFailure = null
        viewModel.handleIntent(ExerciseTrendIntent.ClickRetry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals(1, state.exercises.size)
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: ExerciseTrendViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun entry(
        id: Long,
        exerciseId: Long,
        exerciseName: String,
        bodyPart: BodyPart,
        weight: Int,
        repeatCount: Int,
    ): WorkoutEntry = WorkoutEntry(
        id = id,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        bodyPart = bodyPart,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = repeatCount, intensity = Intensity.Weight(weight))),
    )
}
