package kr.sdbk.bodyplan.feature.workoutlog.impl.calendar

import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.DayStatus
import kr.sdbk.bodyplan.core.domain.usecase.GetMonthlyDayStatusUseCase
import kr.sdbk.bodyplan.core.domain.usecase.IsEditableDateUseCase
import kr.sdbk.bodyplan.feature.workoutlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeWorkoutLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class WorkoutCalendarViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today: LocalDate = LocalDate.of(2026, 9, 7)
    private val clock: Clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun viewModel(repository: FakeWorkoutLogRepository): WorkoutCalendarViewModel {
        val isEditableDate = IsEditableDateUseCase(clock)
        return WorkoutCalendarViewModel(
            getMonthlyDayStatus = GetMonthlyDayStatusUseCase(repository, isEditableDate, clock),
            clock = clock,
        )
    }

    @Test
    fun `진입하면 이번 달과 오늘이 실린다`() = runTest {
        val viewModel = viewModel(FakeWorkoutLogRepository())

        subscribe(viewModel)

        assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.yearMonth)
        assertEquals(today, viewModel.uiState.value.today)
    }

    @Test
    fun `기록이 있는 날은 부위가 실리고 지난 빈 날은 휴식이다`() = runTest {
        val repository = FakeWorkoutLogRepository(
            bodyPartsByDate = mapOf(LocalDate.of(2026, 9, 3) to setOf(BodyPart.CHEST)),
        )
        val viewModel = viewModel(repository)

        subscribe(viewModel)

        val statuses = viewModel.uiState.value.dayStatuses
        assertEquals(DayStatus.Recorded(setOf(BodyPart.CHEST)), statuses[LocalDate.of(2026, 9, 3)])
        assertEquals(DayStatus.Rest, statuses[LocalDate.of(2026, 9, 4)])
    }

    @Test
    fun `오늘과 어제는 비어 있어도 휴식이 아니다`() = runTest {
        val viewModel = viewModel(FakeWorkoutLogRepository())

        subscribe(viewModel)

        val statuses = viewModel.uiState.value.dayStatuses
        assertEquals(DayStatus.Pending, statuses[today])
        assertEquals(DayStatus.Pending, statuses[today.minusDays(1)])
        assertEquals(DayStatus.Rest, statuses[today.minusDays(2)])
    }

    @Test
    fun `오늘 이후는 아직 오지 않은 날이다`() = runTest {
        val viewModel = viewModel(FakeWorkoutLogRepository())

        subscribe(viewModel)

        assertEquals(DayStatus.Upcoming, viewModel.uiState.value.dayStatuses[today.plusDays(1)])
    }

    @Test
    fun `그 달의 모든 날짜가 키로 들어간다`() = runTest {
        val viewModel = viewModel(FakeWorkoutLogRepository())

        subscribe(viewModel)

        val statuses = viewModel.uiState.value.dayStatuses
        assertEquals(30, statuses.size)
        assertNotNull(statuses[LocalDate.of(2026, 9, 1)])
        assertNotNull(statuses[LocalDate.of(2026, 9, 30)])
    }

    @Test
    fun `이전 달로 옮기면 그 달을 다시 읽는다`() = runTest {
        val repository = FakeWorkoutLogRepository(
            bodyPartsByDate = mapOf(LocalDate.of(2026, 8, 10) to setOf(BodyPart.BACK)),
        )
        val viewModel = viewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutCalendarIntent.ChangeMonth(YearMonth.of(2026, 8)))
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 8), viewModel.uiState.value.yearMonth)
        assertEquals(31, viewModel.uiState.value.dayStatuses.size)
        assertEquals(
            DayStatus.Recorded(setOf(BodyPart.BACK)),
            viewModel.uiState.value.dayStatuses[LocalDate.of(2026, 8, 10)],
        )
    }

    @Test
    fun `다음 달은 전부 아직 오지 않은 날이다`() = runTest {
        val viewModel = viewModel(FakeWorkoutLogRepository())
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutCalendarIntent.ChangeMonth(YearMonth.of(2026, 10)))
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.yearMonth)
        assertTrue(viewModel.uiState.value.dayStatuses.values.all { it == DayStatus.Upcoming })
    }

    @Test
    fun `날짜를 누르면 그 날짜의 기록으로 이동한다`() = runTest {
        val viewModel = viewModel(FakeWorkoutLogRepository())
        val effects = mutableListOf<WorkoutCalendarEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutCalendarIntent.ClickDate(LocalDate.of(2026, 9, 3)))
        advanceUntilIdle()

        val effect = effects.filterIsInstance<WorkoutCalendarEffect.NavigateToLog>().single()
        assertEquals(LocalDate.of(2026, 9, 3), effect.date)
    }

    @Test
    fun `종목 추이 아이콘을 누르면 종목 추이 화면으로 이동한다`() = runTest {
        val viewModel = viewModel(FakeWorkoutLogRepository())
        val effects = mutableListOf<WorkoutCalendarEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutCalendarIntent.ClickExerciseTrend)
        advanceUntilIdle()

        assertTrue(effects.contains(WorkoutCalendarEffect.NavigateToExerciseTrend))
    }

    @Test
    fun `진입 시 펼침 상태는 접혀 있다`() = runTest {
        val viewModel = viewModel(FakeWorkoutLogRepository())

        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.isCalendarExpanded)
    }

    @Test
    fun `토글을 한 번 하면 펼쳐지고 두 번 하면 다시 접힌다`() = runTest {
        val viewModel = viewModel(FakeWorkoutLogRepository())
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutCalendarIntent.ToggleCalendarExpansion)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isCalendarExpanded)

        viewModel.handleIntent(WorkoutCalendarIntent.ToggleCalendarExpansion)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCalendarExpanded)
    }

    @Test
    fun `다른 달을 보다가 펼친 뒤 접으면 이번 달로 돌아오고 다시 읽는다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutCalendarIntent.ChangeMonth(YearMonth.of(2026, 8)))
        advanceUntilIdle()
        viewModel.handleIntent(WorkoutCalendarIntent.ToggleCalendarExpansion)
        advanceUntilIdle()
        val callCountBeforeCollapse = repository.observeBodyPartsInRangeCallCount

        viewModel.handleIntent(WorkoutCalendarIntent.ToggleCalendarExpansion)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCalendarExpanded)
        assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.yearMonth)
        assertTrue(repository.observeBodyPartsInRangeCallCount > callCountBeforeCollapse)
    }

    @Test
    fun `이번 달을 보는 채로 접으면 다시 읽지 않는다`() = runTest {
        val repository = FakeWorkoutLogRepository()
        val viewModel = viewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutCalendarIntent.ToggleCalendarExpansion)
        advanceUntilIdle()
        val callCountBeforeCollapse = repository.observeBodyPartsInRangeCallCount

        viewModel.handleIntent(WorkoutCalendarIntent.ToggleCalendarExpansion)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCalendarExpanded)
        assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.yearMonth)
        assertEquals(callCountBeforeCollapse, repository.observeBodyPartsInRangeCallCount)
    }

    @Test
    fun `조회가 실패하면 에러가 실리고 재시도가 다시 읽는다`() = runTest {
        val repository = FakeWorkoutLogRepository(
            bodyPartsByDate = mapOf(LocalDate.of(2026, 9, 3) to setOf(BodyPart.LEG)),
        )
        repository.observeFailure = IllegalStateException("boom")
        val viewModel = viewModel(repository)
        subscribe(viewModel)

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)

        repository.observeFailure = null
        viewModel.handleIntent(WorkoutCalendarIntent.ClickRetry)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(
            DayStatus.Recorded(setOf(BodyPart.LEG)),
            viewModel.uiState.value.dayStatuses[LocalDate.of(2026, 9, 3)],
        )
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: WorkoutCalendarViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }
}
