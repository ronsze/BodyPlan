package kr.sdbk.bodyplan.feature.my.impl.inbodyadd

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.usecase.AddInbodyRecordUseCase
import kr.sdbk.bodyplan.core.domain.usecase.RequestInbodyAnalysisUseCase
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.TEST_TODAY
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAiCredentialRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAnalysisRunner
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeInbodyImageRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeInbodyRecordRepository
import kr.sdbk.bodyplan.feature.my.impl.testClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class InbodyAddViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val credential = AiCredential(provider = AiProvider.CLAUDE, token = "token")
    private val uri = "content://picked/image.jpg"

    private val records = FakeInbodyRecordRepository()
    private val images = FakeInbodyImageRepository()
    private val runner = FakeAnalysisRunner()

    private fun viewModel(credentials: FakeAiCredentialRepository = FakeAiCredentialRepository(credential)) =
        InbodyAddViewModel(
            addInbodyRecord = AddInbodyRecordUseCase(images, records, testClock),
            requestInbodyAnalysis = RequestInbodyAnalysisUseCase(credentials, records, runner),
            aiCredentialRepository = credentials,
            clock = testClock,
        )

    @Test
    fun `측정일의 처음 값은 오늘이고 오늘 이후 날짜는 고를 수 없다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)
        assertEquals(TEST_TODAY, viewModel.uiState.value.measuredOn)

        viewModel.handleIntent(InbodyAddIntent.ClickPickDate)
        viewModel.handleIntent(InbodyAddIntent.SelectDate(TEST_TODAY.plusDays(1)))
        assertEquals(TEST_TODAY, viewModel.uiState.value.measuredOn)

        viewModel.handleIntent(InbodyAddIntent.SelectDate(TEST_TODAY.minusDays(1)))
        assertEquals(TEST_TODAY.minusDays(1), viewModel.uiState.value.measuredOn)
        assertFalse(viewModel.uiState.value.isDatePickerVisible)
    }

    @Test
    fun `사진을 고르기 전에는 두 버튼이 눌리지 않는다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.handleIntent(InbodyAddIntent.ClickAddOnly)
        viewModel.handleIntent(InbodyAddIntent.ClickAddAndAnalyze)
        advanceUntilIdle()

        assertTrue(records.insertCalls.isEmpty())
    }

    @Test
    fun `사진만 추가를 누르면 기록만 만들고 분석은 요청하지 않으며 화면을 닫는다`() = runTest {
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyAddIntent.PickImage(uri))

        viewModel.handleIntent(InbodyAddIntent.ClickAddOnly)
        advanceUntilIdle()

        assertEquals(1, records.insertCalls.size)
        assertTrue(runner.startCalls.isEmpty())
        assertTrue(effects.any { it is InbodyAddEffect.Close })
    }

    @Test
    fun `추가하고 분석 요청을 누르면 기록을 만든 뒤 분석을 요청한다`() = runTest {
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyAddIntent.PickImage(uri))

        viewModel.handleIntent(InbodyAddIntent.ClickAddAndAnalyze)
        advanceUntilIdle()

        val newId = records.items.value.single().record.id
        assertEquals(newId.toString(), runner.startCalls.single().scopeKey)
        assertTrue(effects.any { it is InbodyAddEffect.Close })
    }

    @Test
    fun `AI 연결이 없으면 추가하고 분석 요청은 기록도 사진 복사도 만들지 않고 선택을 유지한다`() = runTest {
        val viewModel = viewModel(FakeAiCredentialRepository(null))
        subscribe(viewModel)
        viewModel.handleIntent(InbodyAddIntent.PickImage(uri))

        viewModel.handleIntent(InbodyAddIntent.ClickAddAndAnalyze)
        advanceUntilIdle()

        assertTrue(records.insertCalls.isEmpty())
        assertTrue(images.saved.isEmpty())
        val state = viewModel.uiState.value
        assertTrue(state.isTokenDialogVisible)
        assertEquals(uri, state.pickedImageUri)
        assertEquals(TEST_TODAY, state.measuredOn)
    }

    @Test
    fun `사진만 추가는 AI 연결이 없어도 동작한다`() = runTest {
        val viewModel = viewModel(FakeAiCredentialRepository(null))
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyAddIntent.PickImage(uri))

        viewModel.handleIntent(InbodyAddIntent.ClickAddOnly)
        advanceUntilIdle()

        assertEquals(1, records.insertCalls.size)
        assertTrue(effects.any { it is InbodyAddEffect.Close })
    }

    @Test
    fun `저장이 실패하면 오류 문구가 보이고 고른 사진과 측정일이 남는다`() = runTest {
        records.insertFailure = IllegalStateException("저장 실패")
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyAddIntent.PickImage(uri))

        viewModel.handleIntent(InbodyAddIntent.ClickAddOnly)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertFalse(state.isSubmitting)
        assertEquals(uri, state.pickedImageUri)
        assertEquals(TEST_TODAY, state.measuredOn)
        assertTrue(effects.none { it is InbodyAddEffect.Close })
    }

    @Test
    fun `기록을 만든 뒤 분석 요청이 실패하면 기록은 남고 오류를 알린 뒤 닫는다`() = runTest {
        runner.startFailure = IllegalStateException("요청 실패")
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyAddIntent.PickImage(uri))

        viewModel.handleIntent(InbodyAddIntent.ClickAddAndAnalyze)
        advanceUntilIdle()

        assertEquals(1, records.items.value.size)
        assertEquals(2, effects.size)
        assertTrue(effects[0] is InbodyAddEffect.ShowMessage)
        assertTrue(effects[1] is InbodyAddEffect.Close)
    }

    @Test
    fun `확인하면 AI 연결 화면으로 가고 선택은 남는다`() = runTest {
        val viewModel = viewModel(FakeAiCredentialRepository(null))
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyAddIntent.PickImage(uri))
        viewModel.handleIntent(InbodyAddIntent.ClickAddAndAnalyze)
        advanceUntilIdle()

        viewModel.handleIntent(InbodyAddIntent.ConfirmTokenDialog)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTokenDialogVisible)
        assertTrue(effects.any { it is InbodyAddEffect.NavigateToAiToken })
        assertEquals(uri, viewModel.uiState.value.pickedImageUri)
    }

    private fun TestScope.subscribe(viewModel: InbodyAddViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun TestScope.collectEffects(viewModel: InbodyAddViewModel): MutableList<InbodyAddEffect> {
        val effects = mutableListOf<InbodyAddEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        return effects
    }
}
