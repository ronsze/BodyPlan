package kr.sdbk.bodyplan.feature.my.impl.inbody

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AiRequestFailedException
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.domain.usecase.AnalyzeInbodyUseCase
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAiAnalysisRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAiCredentialRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAnalysisResultRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeInbodyImageRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeUserProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    private fun viewModel(
        inbodyImageRepository: FakeInbodyImageRepository = FakeInbodyImageRepository(),
        analysisResultRepository: FakeAnalysisResultRepository = FakeAnalysisResultRepository(),
        aiAnalysisRepository: FakeAiAnalysisRepository = FakeAiAnalysisRepository(),
        aiCredentialRepository: FakeAiCredentialRepository = FakeAiCredentialRepository(credential),
    ): InbodyViewModel {
        val analyzeInbody = AnalyzeInbodyUseCase(
            userProfileRepository = FakeUserProfileRepository(),
            aiCredentialRepository = aiCredentialRepository,
            aiAnalysisRepository = aiAnalysisRepository,
            analysisResultRepository = analysisResultRepository,
            inbodyImageRepository = inbodyImageRepository,
        )
        return InbodyViewModel(
            analyzeInbody = analyzeInbody,
            analysisResultRepository = analysisResultRepository,
            aiCredentialRepository = aiCredentialRepository,
        )
    }

    @Test
    fun `사진을 고르지 않으면 canAnalyze가 거짓이고 ClickAnalyze가 아무 것도 하지 않는다`() = runTest {
        val aiAnalysisRepository = FakeAiAnalysisRepository()
        val viewModel = viewModel(aiAnalysisRepository = aiAnalysisRepository)
        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.canAnalyze)

        viewModel.handleIntent(InbodyIntent.ClickAnalyze)
        advanceUntilIdle()

        assertEquals(0, aiAnalysisRepository.analyzeInbodyCallCount)
    }

    @Test
    fun `토큰이 없으면 isTokenDialogVisible이 켜지고 AI를 부르지 않는다`() = runTest {
        val aiAnalysisRepository = FakeAiAnalysisRepository()
        val viewModel = viewModel(
            aiCredentialRepository = FakeAiCredentialRepository(null),
            aiAnalysisRepository = aiAnalysisRepository,
        )
        subscribe(viewModel)

        viewModel.handleIntent(InbodyIntent.PickImage(sourceUri))
        viewModel.handleIntent(InbodyIntent.ClickAnalyze)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isTokenDialogVisible)
        assertEquals(0, aiAnalysisRepository.analyzeInbodyCallCount)
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
    fun `이력이 최신순으로 들어오고 고른 것이 없으면 가장 최근 결과가 보인다`() = runTest {
        val older = AnalysisResult(
            id = 1L,
            kind = AnalysisKind.INBODY,
            scopeKey = "",
            content = content.copy(summary = "예전 결과"),
            createdAtMillis = 0L,
        )
        val newer = AnalysisResult(
            id = 2L,
            kind = AnalysisKind.INBODY,
            scopeKey = "",
            content = content.copy(summary = "최근 결과"),
            createdAtMillis = 1L,
        )
        val analysisResultRepository = FakeAnalysisResultRepository(initialHistory = listOf(older, newer))
        val viewModel = viewModel(analysisResultRepository = analysisResultRepository)

        subscribe(viewModel)

        assertEquals(listOf(2L, 1L), viewModel.uiState.value.history.map { it.id })
        assertEquals("최근 결과", viewModel.uiState.value.result?.content?.summary)
    }

    @Test
    fun `이력에서 하나를 누르면 그 결과가 보이고 고르던 사진이 비워진다`() = runTest {
        val older = AnalysisResult(
            id = 1L,
            kind = AnalysisKind.INBODY,
            scopeKey = "",
            content = content.copy(summary = "예전 결과"),
            createdAtMillis = 0L,
        )
        val newer = AnalysisResult(
            id = 2L,
            kind = AnalysisKind.INBODY,
            scopeKey = "",
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

    @Test
    fun `분석에 성공하면 고르던 사진이 비워지고 새로 저장된 결과가 보인다`() = runTest {
        val aiAnalysisRepository = FakeAiAnalysisRepository(content = content)
        val analysisResultRepository = FakeAnalysisResultRepository()
        val viewModel = viewModel(
            aiAnalysisRepository = aiAnalysisRepository,
            analysisResultRepository = analysisResultRepository,
        )
        subscribe(viewModel)
        viewModel.handleIntent(InbodyIntent.PickImage(sourceUri))

        viewModel.handleIntent(InbodyIntent.ClickAnalyze)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pickedImageUri)
        assertEquals(content, viewModel.uiState.value.result?.content)
        assertEquals(1, analysisResultRepository.saveCount)
    }

    @Test
    fun `분석이 실패하면 고르던 사진이 남고 errorMessage가 채워진다`() = runTest {
        val aiAnalysisRepository = FakeAiAnalysisRepository()
        aiAnalysisRepository.analyzeInbodyFailure = IllegalStateException("boom")
        val viewModel = viewModel(aiAnalysisRepository = aiAnalysisRepository)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyIntent.PickImage(sourceUri))

        viewModel.handleIntent(InbodyIntent.ClickAnalyze)
        advanceUntilIdle()

        assertEquals(sourceUri, viewModel.uiState.value.pickedImageUri)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `부르지 못한 사유가 있으면 기본 문구 뒤에 붙는다`() = runTest {
        val aiAnalysisRepository = FakeAiAnalysisRepository()
        aiAnalysisRepository.analyzeInbodyFailure = AiRequestFailedException(null, "요청 형식이 잘못됐습니다")
        val viewModel = viewModel(aiAnalysisRepository = aiAnalysisRepository)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyIntent.PickImage(sourceUri))

        viewModel.handleIntent(InbodyIntent.ClickAnalyze)
        advanceUntilIdle()

        assertEquals("분석하지 못했습니다: 요청 형식이 잘못됐습니다", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `키가 거절되면 키가 틀렸다고 알린다`() = runTest {
        val aiAnalysisRepository = FakeAiAnalysisRepository()
        aiAnalysisRepository.analyzeInbodyFailure = AiUnauthorizedException()
        val viewModel = viewModel(aiAnalysisRepository = aiAnalysisRepository)
        subscribe(viewModel)
        viewModel.handleIntent(InbodyIntent.PickImage(sourceUri))

        viewModel.handleIntent(InbodyIntent.ClickAnalyze)
        advanceUntilIdle()

        assertEquals("키가 올바르지 않습니다", viewModel.uiState.value.errorMessage)
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
