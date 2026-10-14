package kr.sdbk.bodyplan.feature.my.impl.inbodydetail

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.InbodyRecordWithResult
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.usecase.BuildInbodyDetailUseCase
import kr.sdbk.bodyplan.core.domain.usecase.ObserveInbodyEntriesUseCase
import kr.sdbk.bodyplan.core.domain.usecase.RequestInbodyAnalysisUseCase
import kr.sdbk.bodyplan.feature.my.api.InbodyDetailNavKey
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAiCredentialRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAnalysisRunner
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeInbodyRecordRepository
import kr.sdbk.bodyplan.feature.my.impl.inbodyItem
import kr.sdbk.bodyplan.feature.my.impl.runningRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class InbodyDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val credential = AiCredential(provider = AiProvider.CLAUDE, token = "token")
    private val measurement = InbodyMeasurement(weightKg = 70.0, skeletalMuscleKg = 32.0, bodyFatKg = 14.0)
    private val runner = FakeAnalysisRunner()

    private fun viewModel(
        items: List<InbodyRecordWithResult>,
        recordId: Long = 1L,
        credentials: AiCredentialRepository = FakeAiCredentialRepository(credential),
    ): InbodyDetailViewModel {
        val records = FakeInbodyRecordRepository(items)
        return InbodyDetailViewModel(
            observeInbodyEntries = ObserveInbodyEntriesUseCase(records, runner),
            buildInbodyDetail = BuildInbodyDetailUseCase(),
            requestInbodyAnalysis = RequestInbodyAnalysisUseCase(credentials, records, runner),
            aiCredentialRepository = credentials,
            navKey = InbodyDetailNavKey(recordId),
        )
    }

    @Test
    fun `결과가 있으면 상태와 상관없이 측정값과 분석 내용을 보인다`() = runTest {
        runner.runsFor(AnalysisKind.INBODY).value = listOf(runningRun(1))
        val viewModel = viewModel(listOf(inbodyItem(1, measurement = measurement)))
        subscribe(viewModel)

        val detail = viewModel.uiState.value.detail
        assertNotNull(detail)
        assertEquals(measurement, detail!!.entry.result?.measurement)
        assertTrue(detail.metrics.any { it.valueTenths != null })
    }

    @Test
    fun `분석 중이면 새로 분석하기가 눌리지 않는다`() = runTest {
        runner.runsFor(AnalysisKind.INBODY).value = listOf(runningRun(1))
        val viewModel = viewModel(listOf(inbodyItem(1, measurement = measurement)))
        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.canAnalyze)
        viewModel.handleIntent(InbodyDetailIntent.ClickAnalyze)
        advanceUntilIdle()

        assertTrue(runner.startCalls.isEmpty())
    }

    @Test
    fun `분석 전 기록은 결과 없이 나온다`() = runTest {
        val viewModel = viewModel(listOf(inbodyItem(1)))
        subscribe(viewModel)

        val detail = viewModel.uiState.value.detail
        assertNotNull(detail)
        assertNull(detail!!.entry.result)
        assertTrue(detail.metrics.all { it.valueTenths == null })
    }

    @Test
    fun `새로 분석하기를 누르면 그 기록의 분석을 요청한다`() = runTest {
        val viewModel = viewModel(listOf(inbodyItem(1, measurement = measurement)))
        subscribe(viewModel)

        viewModel.handleIntent(InbodyDetailIntent.ClickAnalyze)
        advanceUntilIdle()

        assertEquals("1", runner.startCalls.single().scopeKey)
    }

    @Test
    fun `AI 연결이 없으면 AI 연결 안내가 열린다`() = runTest {
        val viewModel = viewModel(listOf(inbodyItem(1)), credentials = FakeAiCredentialRepository(null))
        subscribe(viewModel)

        viewModel.handleIntent(InbodyDetailIntent.ClickAnalyze)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isTokenDialogVisible)
        assertTrue(runner.startCalls.isEmpty())
    }

    @Test
    fun `요청이 실패하면 오류 문구가 보이고 다음 요청이 시작되면 지워진다`() = runTest {
        val viewModel = viewModel(listOf(inbodyItem(1, measurement = measurement)))
        subscribe(viewModel)
        runner.startFailure = IllegalStateException("start")

        viewModel.handleIntent(InbodyDetailIntent.ClickAnalyze)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        runner.startFailure = null
        val callsBefore = runner.startCalls.size
        viewModel.handleIntent(InbodyDetailIntent.ClickAnalyze)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(callsBefore + 1, runner.startCalls.size)
    }

    @Test
    fun `AI 연결이 없어 요청이 막히면 오류 문구가 아니라 안내가 열린다`() = runTest {
        val backing = FakeAiCredentialRepository(credential)
        val vanishing = object : AiCredentialRepository by backing {
            override suspend fun getCredential(): AiCredential? = null
        }
        val viewModel = viewModel(listOf(inbodyItem(1)), credentials = vanishing)
        subscribe(viewModel)
        assertTrue(viewModel.uiState.value.hasCredential)

        viewModel.handleIntent(InbodyDetailIntent.ClickAnalyze)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isTokenDialogVisible)
        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(runner.startCalls.isEmpty())
    }

    @Test
    fun `없는 기록 번호면 상세가 없다`() = runTest {
        val viewModel = viewModel(listOf(inbodyItem(1)), recordId = 99L)
        subscribe(viewModel)

        assertNull(viewModel.uiState.value.detail)
    }

    @Test
    fun `이미지를 누르면 확대 보기가 열리고 닫으면 닫힌다`() = runTest {
        val viewModel = viewModel(listOf(inbodyItem(1)))
        subscribe(viewModel)

        viewModel.handleIntent(InbodyDetailIntent.ClickImage)
        assertTrue(viewModel.uiState.value.isImageViewerVisible)

        viewModel.handleIntent(InbodyDetailIntent.DismissImageViewer)
        assertFalse(viewModel.uiState.value.isImageViewerVisible)
    }

    private fun TestScope.subscribe(viewModel: InbodyDetailViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }
}
