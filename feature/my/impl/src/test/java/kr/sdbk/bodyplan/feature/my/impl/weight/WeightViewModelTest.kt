package kr.sdbk.bodyplan.feature.my.impl.weight

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.core.domain.usecase.GetWeightTrendUseCase
import kr.sdbk.bodyplan.core.domain.usecase.IsEditableDateUseCase
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeWeightLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class WeightViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 9, 10)
    private val yesterday = today.minusDays(1)

    private fun viewModel(
        weightLogRepository: FakeWeightLogRepository = FakeWeightLogRepository(),
        clock: Clock = clockAt(today),
    ): WeightViewModel = WeightViewModel(
        weightLogRepository = weightLogRepository,
        getWeightTrend = GetWeightTrendUseCase(),
        isEditableDate = IsEditableDateUseCase(clock),
        clock = clock,
    )

    @Test
    fun `화면에 들어오면 오늘이 선택되고 오늘 기록이 입력칸에 채워진다`() = runTest {
        val repository = FakeWeightLogRepository(listOf(WeightRecord(today, 72.4)))
        val viewModel = viewModel(weightLogRepository = repository)

        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertEquals(today, state.today)
        assertEquals(today, state.selectedDate)
        assertEquals("72.4", state.input)
    }

    @Test
    fun `어제를 고르면 selectedDate가 어제가 되고 입력칸이 어제 기록으로 바뀐다`() = runTest {
        val repository = FakeWeightLogRepository(listOf(WeightRecord(yesterday, 70.0)))
        val viewModel = viewModel(weightLogRepository = repository)
        subscribe(viewModel)

        viewModel.handleIntent(WeightIntent.SelectDate(yesterday))

        val state = viewModel.uiState.value
        assertEquals(yesterday, state.selectedDate)
        assertEquals("70.0", state.input)
    }

    @Test
    fun `어제 기록이 없으면 어제를 골랐을 때 입력칸이 빈다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(WeightIntent.SelectDate(yesterday))

        assertEquals("", viewModel.uiState.value.input)
    }

    @Test
    fun `editableDates는 오늘과 어제뿐이다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        assertEquals(listOf(today, yesterday), viewModel.uiState.value.editableDates)
    }

    @Test
    fun `범위 밖 날짜의 SelectDate는 무시된다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(WeightIntent.SelectDate(today.minusDays(2)))

        assertEquals(today, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `저장하면 그 날짜에 값이 남고 다시 저장해도 기록이 하나로 유지된다`() = runTest {
        val repository = FakeWeightLogRepository()
        val viewModel = viewModel(weightLogRepository = repository)
        subscribe(viewModel)

        viewModel.handleIntent(WeightIntent.ChangeInput("72.4"))
        viewModel.handleIntent(WeightIntent.ClickSave)
        advanceUntilIdle()
        viewModel.handleIntent(WeightIntent.ChangeInput("73.0"))
        viewModel.handleIntent(WeightIntent.ClickSave)
        advanceUntilIdle()

        val recordsOnToday = repository.currentRecords().filter { it.date == today }
        assertEquals(1, recordsOnToday.size)
        assertEquals(73.0, recordsOnToday.first().weightKg, DELTA)
        assertEquals(2, repository.saveCount)
    }

    @Test
    fun `ChangeInput은 형식에 맞지 않으면 이전 값을 유지한다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(WeightIntent.ChangeInput("72"))
        viewModel.handleIntent(WeightIntent.ChangeInput("72.44"))
        assertEquals("72", viewModel.uiState.value.input)

        viewModel.handleIntent(WeightIntent.ChangeInput("abc"))
        assertEquals("72", viewModel.uiState.value.input)

        viewModel.handleIntent(WeightIntent.ChangeInput("1234"))
        assertEquals("72", viewModel.uiState.value.input)

        viewModel.handleIntent(WeightIntent.ChangeInput(""))
        assertEquals("", viewModel.uiState.value.input)
    }

    @Test
    fun `저장 직후 기록 방출이 입력 중인 값을 덮지 않는다`() = runTest {
        val repository = FakeWeightLogRepository()
        val viewModel = viewModel(weightLogRepository = repository)
        subscribe(viewModel)

        viewModel.handleIntent(WeightIntent.ChangeInput("72.4"))
        viewModel.handleIntent(WeightIntent.ClickSave)
        advanceUntilIdle()
        viewModel.handleIntent(WeightIntent.ChangeInput("72.9"))

        assertEquals("72.9", viewModel.uiState.value.input)
    }

    @Test
    fun `저장 시점에 선택 날짜가 편집 불가면 저장하지 않고 메시지를 낸다`() = runTest {
        val clock = MutableClock(clockAt(today))
        val repository = FakeWeightLogRepository()
        val viewModel = viewModel(weightLogRepository = repository, clock = clock)
        val effects = mutableListOf<WeightEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        subscribe(viewModel)

        viewModel.handleIntent(WeightIntent.ChangeInput("72.4"))
        // 이틀을 넘겨야 화면이 그려질 때의 오늘이 더 이상 오늘도 어제도 아니게 된다 —
        // 하루만 넘기면 그날은 새 기준의 "어제"가 되어 여전히 편집 가능하다.
        clock.advanceTo(today.plusDays(2))
        viewModel.handleIntent(WeightIntent.ClickSave)
        advanceUntilIdle()

        assertTrue(repository.currentRecords().isEmpty())
        assertTrue(effects.any { it is WeightEffect.ShowMessage && it.message == "오늘과 어제만 기록할 수 있어요" })
    }

    @Test
    fun `조회가 실패하면 로딩이 끝나고 메시지가 난다`() = runTest {
        val repository = FakeWeightLogRepository()
        repository.loadFailure = IllegalStateException("boom")
        val viewModel = viewModel(weightLogRepository = repository)
        val effects = mutableListOf<WeightEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }

        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(effects.any { it is WeightEffect.ShowMessage && it.message == "체중 기록을 불러오지 못했습니다" })
    }

    @Test
    fun `저장이 실패하면 isSaving이 돌아오고 메시지가 나며 입력이 남는다`() = runTest {
        val repository = FakeWeightLogRepository()
        repository.saveFailure = IllegalStateException("boom")
        val viewModel = viewModel(weightLogRepository = repository)
        val effects = mutableListOf<WeightEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        subscribe(viewModel)

        viewModel.handleIntent(WeightIntent.ChangeInput("72.4"))
        viewModel.handleIntent(WeightIntent.ClickSave)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(effects.any { it is WeightEffect.ShowMessage && it.message == "체중을 저장하지 못했습니다" })
        assertEquals("72.4", viewModel.uiState.value.input)
    }

    @Test
    fun `trend가 UseCase가 낸 값으로 채워진다`() = runTest {
        val repository = FakeWeightLogRepository(listOf(WeightRecord(today, 72.0), WeightRecord(yesterday, 72.5)))
        val viewModel = viewModel(weightLogRepository = repository)

        subscribe(viewModel)

        assertEquals(-0.5, viewModel.uiState.value.trend.dailyChangeKg!!, DELTA)
    }

    @Test
    fun `뒤로가기를 누르면 화면이 닫힌다`() = runTest {
        val viewModel = viewModel()
        val effects = mutableListOf<WeightEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        subscribe(viewModel)

        viewModel.handleIntent(WeightIntent.ClickBack)
        advanceUntilIdle()

        assertTrue(effects.any { it is WeightEffect.GoBack })
    }

    private fun TestScope.subscribe(viewModel: WeightViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }
}

private const val DELTA = 0.0001

private fun clockAt(date: LocalDate): Clock = Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

/**
 * 화면 진입 이후 자정을 넘긴 상황을 흉내 낸다. `WeightViewModel`은 초기화 시점의 오늘을
 * `today`로 굳히고, 저장 시점에는 [Clock]을 다시 읽어 편집 가능 여부를 판정하므로
 * 그 사이 시계가 바뀌는 상황을 시험하려면 가변 [Clock]이 필요하다.
 */
private class MutableClock(private var current: Clock) : Clock() {
    fun advanceTo(date: LocalDate) {
        current = clockAt(date)
    }

    override fun instant(): Instant = current.instant()

    override fun getZone(): ZoneId = current.zone

    override fun withZone(zone: ZoneId): Clock = current.withZone(zone)
}
