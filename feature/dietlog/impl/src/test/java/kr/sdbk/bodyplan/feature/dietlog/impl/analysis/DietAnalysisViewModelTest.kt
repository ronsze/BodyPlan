package kr.sdbk.bodyplan.feature.dietlog.impl.analysis

import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.domain.model.DietEntry
import kr.sdbk.bodyplan.core.domain.usecase.AnalyzeDietUseCase
import kr.sdbk.bodyplan.feature.dietlog.api.DietAnalysisNavKey
import kr.sdbk.bodyplan.feature.dietlog.api.DietAnalysisPeriod
import kr.sdbk.bodyplan.feature.dietlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.dietlog.impl.fake.FakeAiAnalysisRepository
import kr.sdbk.bodyplan.feature.dietlog.impl.fake.FakeAiCredentialRepository
import kr.sdbk.bodyplan.feature.dietlog.impl.fake.FakeAnalysisResultRepository
import kr.sdbk.bodyplan.feature.dietlog.impl.fake.FakeDietLogRepository
import kr.sdbk.bodyplan.feature.dietlog.impl.fake.FakeUserProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class DietAnalysisViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val date: LocalDate = LocalDate.of(2026, 9, 7)
    private val credential = AiCredential(provider = AiProvider.CLAUDE, token = "token")
    private val content = AnalysisContent(
        summary = "요약",
        sections = listOf(AnalysisSection(title = "먹은 음식", body = "밥")),
    )

    private fun viewModel(
        dietLogRepository: FakeDietLogRepository = FakeDietLogRepository(
            listOf(DietEntry(id = 1L, imagePath = "/a.jpg", memo = null)),
        ),
        analysisResultRepository: FakeAnalysisResultRepository = FakeAnalysisResultRepository(),
        aiAnalysisRepository: FakeAiAnalysisRepository = FakeAiAnalysisRepository(content = content),
        aiCredentialRepository: FakeAiCredentialRepository = FakeAiCredentialRepository(credential),
        period: DietAnalysisPeriod = DietAnalysisPeriod.DAILY,
    ): DietAnalysisViewModel {
        val analyzeDiet = AnalyzeDietUseCase(
            dietLogRepository = dietLogRepository,
            userProfileRepository = FakeUserProfileRepository(),
            aiCredentialRepository = aiCredentialRepository,
            aiAnalysisRepository = aiAnalysisRepository,
            analysisResultRepository = analysisResultRepository,
        )
        return DietAnalysisViewModel(
            analyzeDiet = analyzeDiet,
            analysisResultRepository = analysisResultRepository,
            aiCredentialRepository = aiCredentialRepository,
            navKey = DietAnalysisNavKey(period, date.toEpochDay()),
        )
    }

    @Test
    fun `저장된 결과가 있으면 화면을 다시 열 때 그대로 보이고 호출하지 않는다`() = runTest {
        val savedContent = content.copy(summary = "이미 저장된 요약")
        val scopeKey = AnalysisScopeKey.daily(date)
        val analysisResultRepository = FakeAnalysisResultRepository(
            initial = mapOf(
                (AnalysisKind.DIET_DAILY to scopeKey) to
                    AnalysisResult(1L, AnalysisKind.DIET_DAILY, scopeKey, savedContent, 0L),
            ),
        )
        val aiAnalysisRepository = FakeAiAnalysisRepository(content = content)
        val viewModel = viewModel(
            analysisResultRepository = analysisResultRepository,
            aiAnalysisRepository = aiAnalysisRepository,
        )

        subscribe(viewModel)

        assertEquals(savedContent, viewModel.uiState.value.result?.content)
        assertEquals(0, aiAnalysisRepository.analyzeDietCallCount)
    }

    @Test
    fun `새로 분석하기를 누르면 다시 호출해 결과를 저장한다`() = runTest {
        val analysisResultRepository = FakeAnalysisResultRepository()
        val aiAnalysisRepository = FakeAiAnalysisRepository(content = content)
        val viewModel = viewModel(
            analysisResultRepository = analysisResultRepository,
            aiAnalysisRepository = aiAnalysisRepository,
        )
        subscribe(viewModel)

        viewModel.handleIntent(DietAnalysisIntent.ClickAnalyze)
        advanceUntilIdle()

        assertEquals(1, aiAnalysisRepository.analyzeDietCallCount)
        assertEquals(1, analysisResultRepository.saveCount)
        assertEquals(content, viewModel.uiState.value.result?.content)
        assertFalse(viewModel.uiState.value.isAnalyzing)
    }

    @Test
    fun `호출이 실패하면 이전 결과가 남고 errorMessage가 채워진다`() = runTest {
        val previousContent = content.copy(summary = "이전 결과")
        val scopeKey = AnalysisScopeKey.daily(date)
        val analysisResultRepository = FakeAnalysisResultRepository(
            initial = mapOf(
                (AnalysisKind.DIET_DAILY to scopeKey) to
                    AnalysisResult(1L, AnalysisKind.DIET_DAILY, scopeKey, previousContent, 0L),
            ),
        )
        val aiAnalysisRepository = FakeAiAnalysisRepository(content = content)
        aiAnalysisRepository.failure = IllegalStateException("boom")
        val viewModel = viewModel(
            analysisResultRepository = analysisResultRepository,
            aiAnalysisRepository = aiAnalysisRepository,
        )
        subscribe(viewModel)

        viewModel.handleIntent(DietAnalysisIntent.ClickAnalyze)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(previousContent, viewModel.uiState.value.result?.content)
        assertEquals(0, analysisResultRepository.saveCount)
    }

    @Test
    fun `토큰이 없으면 호출하지 않고 isTokenDialogVisible이 켜진다`() = runTest {
        val aiCredentialRepository = FakeAiCredentialRepository(null)
        val aiAnalysisRepository = FakeAiAnalysisRepository(content = content)
        val viewModel = viewModel(
            aiCredentialRepository = aiCredentialRepository,
            aiAnalysisRepository = aiAnalysisRepository,
        )
        subscribe(viewModel)

        viewModel.handleIntent(DietAnalysisIntent.ClickAnalyze)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isTokenDialogVisible)
        assertEquals(0, aiAnalysisRepository.analyzeDietCallCount)
    }

    @Test
    fun `팝업의 확인을 누르면 NavigateToAiToken Effect가 난다`() = runTest {
        val viewModel = viewModel(aiCredentialRepository = FakeAiCredentialRepository(null))
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(DietAnalysisIntent.ClickAnalyze)
        advanceUntilIdle()
        viewModel.handleIntent(DietAnalysisIntent.ConfirmTokenDialog)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTokenDialogVisible)
        assertTrue(effects.any { it is DietAnalysisEffect.NavigateToAiToken })
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: DietAnalysisViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun kotlinx.coroutines.test.TestScope.collectEffects(
        viewModel: DietAnalysisViewModel,
    ): List<DietAnalysisEffect> {
        val effects = mutableListOf<DietAnalysisEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        return effects
    }
}
