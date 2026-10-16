package kr.sdbk.bodyplan.feature.my.impl.inbody

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.InbodyEntryStatus
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.InbodyPeriod
import kr.sdbk.bodyplan.core.domain.model.InbodyRecordWithResult
import kr.sdbk.bodyplan.core.domain.usecase.BuildInbodyTrendUseCase
import kr.sdbk.bodyplan.core.domain.usecase.ObserveInbodyEntriesUseCase
import kr.sdbk.bodyplan.core.domain.usecase.RequestInbodyAnalysisUseCase
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.TEST_TODAY
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAiCredentialRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAnalysisRunner
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeInbodyRecordRepository
import kr.sdbk.bodyplan.feature.my.impl.inbodyItem
import kr.sdbk.bodyplan.feature.my.impl.runningRun
import kr.sdbk.bodyplan.feature.my.impl.testClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class InbodyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val credential = AiCredential(provider = AiProvider.CLAUDE, token = "token")
    private val measurement = InbodyMeasurement(weightKg = 70.0, skeletalMuscleKg = 32.0, bodyFatKg = 14.0)

    private fun viewModel(
        items: List<InbodyRecordWithResult> = emptyList(),
        runner: FakeAnalysisRunner = FakeAnalysisRunner(),
        credentials: FakeAiCredentialRepository = FakeAiCredentialRepository(credential),
    ): InbodyViewModel {
        val records = FakeInbodyRecordRepository(items)
        return InbodyViewModel(
            observeInbodyEntries = ObserveInbodyEntriesUseCase(records, runner),
            buildInbodyTrend = BuildInbodyTrendUseCase(),
            requestInbodyAnalysis = RequestInbodyAnalysisUseCase(credentials, records, runner),
            aiCredentialRepository = credentials,
            clock = testClock,
        )
    }

    @Test
    fun `한 기록이 분석 중이어도 다른 기록은 분석을 요청할 수 있다`() = runTest {
        val runner = FakeAnalysisRunner()
        runner.runsFor(AnalysisKind.INBODY).value = listOf(runningRun(1))
        val viewModel = viewModel(listOf(inbodyItem(1), inbodyItem(2)), runner)
        subscribe(viewModel)

        viewModel.handleIntent(InbodyIntent.ClickAnalyze(2))
        advanceUntilIdle()

        assertEquals("2", runner.startCalls.single().scopeKey)
    }

    @Test
    fun `분석 중인 기록의 요청은 무시된다`() = runTest {
        val runner = FakeAnalysisRunner()
        runner.runsFor(AnalysisKind.INBODY).value = listOf(runningRun(1))
        val viewModel = viewModel(listOf(inbodyItem(1)), runner)
        subscribe(viewModel)

        viewModel.handleIntent(InbodyIntent.ClickAnalyze(1))
        advanceUntilIdle()

        assertTrue(runner.startCalls.isEmpty())
    }

    @Test
    fun `분석된 기록도 다시 분석할 수 있다`() = runTest {
        val runner = FakeAnalysisRunner()
        val viewModel = viewModel(listOf(inbodyItem(1, measurement = measurement)), runner)
        subscribe(viewModel)

        viewModel.handleIntent(InbodyIntent.ClickAnalyze(1))
        advanceUntilIdle()

        assertEquals("1", runner.startCalls.single().scopeKey)
    }

    @Test
    fun `조회 기간의 처음 값은 1개월이고 고르면 바뀐다`() = runTest {
        val viewModel = viewModel(listOf(inbodyItem(1, measurement = measurement)))
        subscribe(viewModel)

        assertEquals(InbodyPeriod.ONE_MONTH, viewModel.uiState.value.period)

        viewModel.handleIntent(InbodyIntent.SelectPeriod(InbodyPeriod.THREE_MONTHS))
        advanceUntilIdle()

        assertEquals(InbodyPeriod.THREE_MONTHS, viewModel.uiState.value.period)
        assertEquals(TEST_TODAY.minusMonths(3), viewModel.uiState.value.trend?.rangeStart)
    }

    @Test
    fun `AI 연결이 없으면 AI 연결 안내가 열리고 요청하지 않는다`() = runTest {
        val runner = FakeAnalysisRunner()
        val viewModel = viewModel(listOf(inbodyItem(1)), runner, FakeAiCredentialRepository(null))
        subscribe(viewModel)

        viewModel.handleIntent(InbodyIntent.ClickAnalyze(1))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isTokenDialogVisible)
        assertTrue(runner.startCalls.isEmpty())
    }

    @Test
    fun `확인하면 AI 연결 화면으로 가고 닫으면 안내만 닫힌다`() = runTest {
        val viewModel = viewModel(listOf(inbodyItem(1)), credentials = FakeAiCredentialRepository(null))
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(InbodyIntent.ClickAnalyze(1))
        advanceUntilIdle()
        viewModel.handleIntent(InbodyIntent.ConfirmTokenDialog)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTokenDialogVisible)
        assertTrue(effects.any { it is InbodyEffect.NavigateToAiToken })

        effects.clear()
        viewModel.handleIntent(InbodyIntent.ClickAnalyze(1))
        advanceUntilIdle()
        viewModel.handleIntent(InbodyIntent.DismissTokenDialog)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTokenDialogVisible)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `실패한 기록은 실패 사유와 이전 결과를 함께 가진 실패 상태로 나온다`() = runTest {
        val viewModel = viewModel(listOf(inbodyItem(1, measurement = measurement, failureReason = "형식 오류")))
        subscribe(viewModel)

        val entry = viewModel.uiState.value.entries.single()
        assertEquals(InbodyEntryStatus.FAILED, entry.status)
        assertEquals(measurement, entry.result?.measurement)
        assertEquals("형식 오류", entry.record.failureReason)
    }

    @Test
    fun `분석 중인 기록은 이전 결과가 있어도 분석 중 상태로 나온다`() = runTest {
        val runner = FakeAnalysisRunner()
        runner.runsFor(AnalysisKind.INBODY).value = listOf(runningRun(1))
        val viewModel = viewModel(listOf(inbodyItem(1, measurement = measurement)), runner)
        subscribe(viewModel)

        val entry = viewModel.uiState.value.entries.single()
        assertEquals(InbodyEntryStatus.RUNNING, entry.status)
        assertEquals(measurement, entry.result?.measurement)
    }

    @Test
    fun `가장 최근 측정 기록에만 최근 측정 표시가 붙는다`() = runTest {
        val viewModel = viewModel(
            listOf(
                inbodyItem(1, TEST_TODAY.withMonth(9).withDayOfMonth(1), measurement),
                inbodyItem(2, TEST_TODAY.withMonth(9).withDayOfMonth(15), measurement),
                inbodyItem(3, TEST_TODAY.withMonth(9).withDayOfMonth(8), measurement),
            ),
        )
        subscribe(viewModel)

        assertEquals(2L, viewModel.uiState.value.latestMeasuredId)
    }

    @Test
    fun `요청이 실패하면 오류 문구가 보인다`() = runTest {
        val runner = FakeAnalysisRunner().apply { startFailure = IllegalStateException("실패") }
        val viewModel = viewModel(listOf(inbodyItem(1)), runner)
        subscribe(viewModel)

        viewModel.handleIntent(InbodyIntent.ClickAnalyze(1))
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    private fun TestScope.subscribe(viewModel: InbodyViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun TestScope.collectEffects(viewModel: InbodyViewModel): MutableList<InbodyEffect> {
        val effects = mutableListOf<InbodyEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        return effects
    }
}
