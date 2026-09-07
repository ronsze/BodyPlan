package kr.sdbk.bodyplan.feature.my.impl.inbody

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunState
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAiCredentialRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAnalysisResultRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAnalysisRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class InbodyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val credential = AiCredential(provider = AiProvider.CLAUDE, token = "token")
    private val content = AnalysisContent(
        summary = "요약",
        sections = listOf(AnalysisSection(title = "체성분", body = "근육량")),
    )
    private val sourceUri = "content://picked/image.jpg"

    /** 인바디는 대상 열쇠가 없어 빈 문자열 하나를 함께 쓴다. ViewModel과 같은 열쇠를 써야 관찰이 이어진다. */
    private val scopeKey = ""

    private fun viewModel(
        analysisResultRepository: FakeAnalysisResultRepository = FakeAnalysisResultRepository(),
        analysisRunner: FakeAnalysisRunner = FakeAnalysisRunner(),
        aiCredentialRepository: FakeAiCredentialRepository = FakeAiCredentialRepository(credential),
    ): InbodyViewModel = InbodyViewModel(
        analysisRunner = analysisRunner,
        analysisResultRepository = analysisResultRepository,
        aiCredentialRepository = aiCredentialRepository,
    )

    @Test
    fun `사진을 고르지 않으면 canAnalyze가 거짓이고 ClickAnalyze가 start를 부르지 않는다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        val viewModel = viewModel(analysisRunner = analysisRunner)
        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.canAnalyze)

        viewModel.handleIntent(InbodyIntent.ClickAnalyze)
        advanceUntilIdle()

        assertTrue(analysisRunner.startCalls.isEmpty())
    }

    @Test
    fun `사진을 고르고 분석하면 AnalysisRunner에 인바디 요청을 넘긴다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        val viewModel = viewModel(analysisRunner = analysisRunner)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyIntent.PickImage(sourceUri))

        viewModel.handleIntent(InbodyIntent.ClickAnalyze)
        advanceUntilIdle()

        val request = analysisRunner.startCalls.single()
        assertEquals(AnalysisKind.INBODY, request.kind)
        assertEquals(scopeKey, request.scopeKey)
        assertEquals(sourceUri, request.sourceUri)
    }

    @Test
    fun `토큰이 없으면 isTokenDialogVisible이 켜지고 start를 부르지 않는다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        val viewModel = viewModel(
            aiCredentialRepository = FakeAiCredentialRepository(null),
            analysisRunner = analysisRunner,
        )
        subscribe(viewModel)

        viewModel.handleIntent(InbodyIntent.PickImage(sourceUri))
        viewModel.handleIntent(InbodyIntent.ClickAnalyze)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isTokenDialogVisible)
        assertTrue(analysisRunner.startCalls.isEmpty())
    }

    @Test
    fun `팝업의 확인을 누르면 NavigateToAiToken Effect가 난다`() = runTest {
        val viewModel = viewModel(aiCredentialRepository = FakeAiCredentialRepository(null))
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(InbodyIntent.PickImage(sourceUri))
        viewModel.handleIntent(InbodyIntent.ClickAnalyze)
        advanceUntilIdle()
        viewModel.handleIntent(InbodyIntent.ConfirmTokenDialog)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTokenDialogVisible)
        assertTrue(effects.any { it is InbodyEffect.NavigateToAiToken })
    }

    @Test
    fun `observe가 RUNNING을 내려주면 isAnalyzing이 참이고 canAnalyze가 거짓이다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        val viewModel = viewModel(analysisRunner = analysisRunner)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyIntent.PickImage(sourceUri))

        analysisRunner.flowFor(AnalysisKind.INBODY, scopeKey).value =
            AnalysisRun(kind = AnalysisKind.INBODY, scopeKey = scopeKey, state = AnalysisRunState.RUNNING)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAnalyzing)
        assertFalse(viewModel.uiState.value.canAnalyze)
    }

    @Test
    fun `observe가 FAILED와 사유를 내려주면 message가 그 사유다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        val viewModel = viewModel(analysisRunner = analysisRunner)
        subscribe(viewModel)

        analysisRunner.flowFor(AnalysisKind.INBODY, scopeKey).value = AnalysisRun(
            kind = AnalysisKind.INBODY,
            scopeKey = scopeKey,
            state = AnalysisRunState.FAILED,
            failureReason = "요청 형식이 잘못됐습니다",
        )
        advanceUntilIdle()

        assertEquals("요청 형식이 잘못됐습니다", viewModel.uiState.value.message)
    }

    @Test
    fun `화면을 나갔다 들어와도(ViewModel을 새로 만들어도) 이미 도는 흐름이면 바로 분석 중이다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        analysisRunner.flowFor(AnalysisKind.INBODY, scopeKey).value =
            AnalysisRun(kind = AnalysisKind.INBODY, scopeKey = scopeKey, state = AnalysisRunState.RUNNING)
        val viewModel = viewModel(analysisRunner = analysisRunner)

        subscribe(viewModel)

        assertTrue(viewModel.uiState.value.isAnalyzing)
    }

    @Test
    fun `돌던 작업이 끝나면 pickedImageUri가 비워진다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        val viewModel = viewModel(analysisRunner = analysisRunner)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyIntent.PickImage(sourceUri))
        assertEquals(sourceUri, viewModel.uiState.value.pickedImageUri)

        analysisRunner.flowFor(AnalysisKind.INBODY, scopeKey).value =
            AnalysisRun(kind = AnalysisKind.INBODY, scopeKey = scopeKey, state = AnalysisRunState.RUNNING)
        advanceUntilIdle()
        analysisRunner.flowFor(AnalysisKind.INBODY, scopeKey).value =
            AnalysisRun(kind = AnalysisKind.INBODY, scopeKey = scopeKey, state = AnalysisRunState.SUCCEEDED)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pickedImageUri)
    }

    /** 끝난 작업의 정보는 다음 분석까지 남아 있어, 화면에 들어올 때마다 되풀이된다. */
    @Test
    fun `지난 성공이 남아 있어도 새로 고른 사진을 지우지 않는다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        analysisRunner.flowFor(AnalysisKind.INBODY, scopeKey).value =
            AnalysisRun(kind = AnalysisKind.INBODY, scopeKey = scopeKey, state = AnalysisRunState.SUCCEEDED)
        val viewModel = viewModel(analysisRunner = analysisRunner)
        subscribe(viewModel)
        advanceUntilIdle()

        viewModel.handleIntent(InbodyIntent.PickImage(sourceUri))
        advanceUntilIdle()

        assertEquals(sourceUri, viewModel.uiState.value.pickedImageUri)
    }

    @Test
    fun `이력이 최신순으로 들어오고 고른 것이 없으면 가장 최근 결과가 보인다`() = runTest {
        val older = AnalysisResult(
            id = 1L,
            kind = AnalysisKind.INBODY,
            scopeKey = scopeKey,
            content = content.copy(summary = "예전 결과"),
            createdAtMillis = 0L,
        )
        val newer = AnalysisResult(
            id = 2L,
            kind = AnalysisKind.INBODY,
            scopeKey = scopeKey,
            content = content.copy(summary = "최근 결과"),
            createdAtMillis = 1L,
        )
        val analysisResultRepository = FakeAnalysisResultRepository(initialHistory = listOf(older, newer))
        val analysisRunner = FakeAnalysisRunner()
        val viewModel = viewModel(
            analysisResultRepository = analysisResultRepository,
            analysisRunner = analysisRunner,
        )

        subscribe(viewModel)

        assertEquals(listOf(2L, 1L), viewModel.uiState.value.history.map { it.id })
        assertEquals("최근 결과", viewModel.uiState.value.result?.content?.summary)
        assertTrue(analysisRunner.startCalls.isEmpty())
    }

    @Test
    fun `이력에서 하나를 누르면 그 결과가 보이고 고르던 사진이 비워진다`() = runTest {
        val older = AnalysisResult(
            id = 1L,
            kind = AnalysisKind.INBODY,
            scopeKey = scopeKey,
            content = content.copy(summary = "예전 결과"),
            createdAtMillis = 0L,
        )
        val newer = AnalysisResult(
            id = 2L,
            kind = AnalysisKind.INBODY,
            scopeKey = scopeKey,
            content = content.copy(summary = "최근 결과"),
            createdAtMillis = 1L,
        )
        val analysisResultRepository = FakeAnalysisResultRepository(initialHistory = listOf(older, newer))
        val viewModel = viewModel(analysisResultRepository = analysisResultRepository)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyIntent.PickImage(sourceUri))

        viewModel.handleIntent(InbodyIntent.ClickHistory(1L))
        advanceUntilIdle()

        assertEquals("예전 결과", viewModel.uiState.value.result?.content?.summary)
        assertNull(viewModel.uiState.value.pickedImageUri)
    }

    private fun TestScope.subscribe(viewModel: InbodyViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun TestScope.collectEffects(viewModel: InbodyViewModel): List<InbodyEffect> {
        val effects = mutableListOf<InbodyEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        return effects
    }
}
