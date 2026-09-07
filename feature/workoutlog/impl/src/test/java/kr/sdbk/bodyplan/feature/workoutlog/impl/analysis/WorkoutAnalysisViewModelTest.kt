package kr.sdbk.bodyplan.feature.workoutlog.impl.analysis

import java.time.LocalDate
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
import kr.sdbk.bodyplan.core.ui.components.analysisPeriodInfo
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutAnalysisNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutAnalysisPeriod
import kr.sdbk.bodyplan.feature.workoutlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeAiCredentialRepository
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeAnalysisResultRepository
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeAnalysisRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class WorkoutAnalysisViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val date: LocalDate = LocalDate.of(2026, 9, 7)
    private val credential = AiCredential(provider = AiProvider.CLAUDE, token = "token")
    private val content = AnalysisContent(
        summary = "요약",
        sections = listOf(AnalysisSection(title = "부위별 볼륨", body = "가슴")),
    )
    private val period = WorkoutAnalysisPeriod.DAILY
    private val periodInfo = analysisPeriodInfo(AnalysisKind.WORKOUT_DAILY, date)

    private fun viewModel(
        analysisResultRepository: FakeAnalysisResultRepository = FakeAnalysisResultRepository(),
        analysisRunner: FakeAnalysisRunner = FakeAnalysisRunner(),
        aiCredentialRepository: FakeAiCredentialRepository = FakeAiCredentialRepository(credential),
    ): WorkoutAnalysisViewModel = WorkoutAnalysisViewModel(
        analysisRunner = analysisRunner,
        analysisResultRepository = analysisResultRepository,
        aiCredentialRepository = aiCredentialRepository,
        navKey = WorkoutAnalysisNavKey(period, date.toEpochDay()),
    )

    @Test
    fun `새로 분석하기를 누르면 AnalysisRunner에 그 화면의 요청을 넘긴다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        val viewModel = viewModel(analysisRunner = analysisRunner)
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutAnalysisIntent.ClickAnalyze)
        advanceUntilIdle()

        val request = analysisRunner.startCalls.single()
        assertEquals(AnalysisKind.WORKOUT_DAILY, request.kind)
        assertEquals(periodInfo.scopeKey, request.scopeKey)
        assertEquals(periodInfo.label, request.periodLabel)
        assertEquals(periodInfo.from, request.from)
        assertEquals(periodInfo.to, request.to)
    }

    @Test
    fun `토큰이 없으면 start를 부르지 않고 isTokenDialogVisible이 켜진다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        val viewModel = viewModel(
            analysisRunner = analysisRunner,
            aiCredentialRepository = FakeAiCredentialRepository(null),
        )
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutAnalysisIntent.ClickAnalyze)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isTokenDialogVisible)
        assertTrue(analysisRunner.startCalls.isEmpty())
    }

    @Test
    fun `observe가 RUNNING을 내려주면 isAnalyzing이 참이고 canAnalyze가 거짓이다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        val viewModel = viewModel(analysisRunner = analysisRunner)
        subscribe(viewModel)

        analysisRunner.flowFor(AnalysisKind.WORKOUT_DAILY, periodInfo.scopeKey).value =
            AnalysisRun(
                kind = AnalysisKind.WORKOUT_DAILY,
                scopeKey = periodInfo.scopeKey,
                state = AnalysisRunState.RUNNING,
            )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAnalyzing)
        assertFalse(viewModel.uiState.value.canAnalyze)
    }

    @Test
    fun `observe가 FAILED와 사유를 내려주면 message가 그 사유다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        val viewModel = viewModel(analysisRunner = analysisRunner)
        subscribe(viewModel)

        analysisRunner.flowFor(AnalysisKind.WORKOUT_DAILY, periodInfo.scopeKey).value = AnalysisRun(
            kind = AnalysisKind.WORKOUT_DAILY,
            scopeKey = periodInfo.scopeKey,
            state = AnalysisRunState.FAILED,
            failureReason = "요청 형식이 잘못됐습니다",
        )
        advanceUntilIdle()

        assertEquals("요청 형식이 잘못됐습니다", viewModel.uiState.value.message)
    }

    @Test
    fun `화면을 나갔다 들어와도(ViewModel을 새로 만들어도) 이미 도는 흐름이면 바로 분석 중이다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        analysisRunner.flowFor(AnalysisKind.WORKOUT_DAILY, periodInfo.scopeKey).value =
            AnalysisRun(
                kind = AnalysisKind.WORKOUT_DAILY,
                scopeKey = periodInfo.scopeKey,
                state = AnalysisRunState.RUNNING,
            )
        val viewModel = viewModel(analysisRunner = analysisRunner)

        subscribe(viewModel)

        assertTrue(viewModel.uiState.value.isAnalyzing)
    }

    @Test
    fun `저장된 결과가 있으면 화면을 다시 열 때 그대로 보이고 start를 부르지 않는다`() = runTest {
        val analysisRunner = FakeAnalysisRunner()
        val analysisResultRepository = FakeAnalysisResultRepository(
            initial = mapOf(
                (AnalysisKind.WORKOUT_DAILY to periodInfo.scopeKey) to
                    AnalysisResult(1L, AnalysisKind.WORKOUT_DAILY, periodInfo.scopeKey, content, 0L),
            ),
        )
        val viewModel = viewModel(
            analysisResultRepository = analysisResultRepository,
            analysisRunner = analysisRunner,
        )

        subscribe(viewModel)

        assertEquals(content, viewModel.uiState.value.result?.content)
        assertTrue(analysisRunner.startCalls.isEmpty())
    }

    @Test
    fun `팝업의 확인을 누르면 NavigateToAiToken Effect가 난다`() = runTest {
        val viewModel = viewModel(aiCredentialRepository = FakeAiCredentialRepository(null))
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutAnalysisIntent.ClickAnalyze)
        advanceUntilIdle()
        viewModel.handleIntent(WorkoutAnalysisIntent.ConfirmTokenDialog)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTokenDialogVisible)
        assertTrue(effects.any { it is WorkoutAnalysisEffect.NavigateToAiToken })
    }

    private fun TestScope.subscribe(viewModel: WorkoutAnalysisViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun TestScope.collectEffects(viewModel: WorkoutAnalysisViewModel): List<WorkoutAnalysisEffect> {
        val effects = mutableListOf<WorkoutAnalysisEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        return effects
    }
}
