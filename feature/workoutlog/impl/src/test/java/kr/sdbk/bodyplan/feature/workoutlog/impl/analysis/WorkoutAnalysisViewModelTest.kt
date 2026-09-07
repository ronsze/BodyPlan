package kr.sdbk.bodyplan.feature.workoutlog.impl.analysis

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AiRequestFailedException
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.usecase.AnalyzeWorkoutUseCase
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutAnalysisNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutAnalysisPeriod
import kr.sdbk.bodyplan.feature.workoutlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeAiAnalysisRepository
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeAiCredentialRepository
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeAnalysisResultRepository
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeUserProfileRepository
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeWorkoutLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    private val clock = Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun viewModel(
        workoutLogRepository: FakeWorkoutLogRepository = FakeWorkoutLogRepository(
            listOf(
                WorkoutEntry(
                    id = 1L,
                    exerciseId = 1L,
                    exerciseName = "벤치프레스",
                    bodyPart = BodyPart.CHEST,
                    intensityType = IntensityType.WEIGHT,
                    sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(60))),
                ),
            ),
        ),
        analysisResultRepository: FakeAnalysisResultRepository = FakeAnalysisResultRepository(),
        aiAnalysisRepository: FakeAiAnalysisRepository = FakeAiAnalysisRepository(content = content),
        aiCredentialRepository: FakeAiCredentialRepository = FakeAiCredentialRepository(credential),
        period: WorkoutAnalysisPeriod = WorkoutAnalysisPeriod.DAILY,
    ): WorkoutAnalysisViewModel {
        val analyzeWorkout = AnalyzeWorkoutUseCase(
            workoutLogRepository = workoutLogRepository,
            userProfileRepository = FakeUserProfileRepository(),
            aiCredentialRepository = aiCredentialRepository,
            aiAnalysisRepository = aiAnalysisRepository,
            analysisResultRepository = analysisResultRepository,
            clock = clock,
        )
        return WorkoutAnalysisViewModel(
            analyzeWorkout = analyzeWorkout,
            analysisResultRepository = analysisResultRepository,
            aiCredentialRepository = aiCredentialRepository,
            navKey = WorkoutAnalysisNavKey(period, date.toEpochDay()),
        )
    }

    @Test
    fun `저장된 결과가 있으면 화면을 다시 열 때 그대로 보이고 호출하지 않는다`() = runTest {
        val savedContent = content.copy(summary = "이미 저장된 요약")
        val scopeKey = AnalysisScopeKey.daily(date)
        val analysisResultRepository = FakeAnalysisResultRepository(
            initial = mapOf(
                (AnalysisKind.WORKOUT_DAILY to scopeKey) to
                    AnalysisResult(1L, AnalysisKind.WORKOUT_DAILY, scopeKey, savedContent, 0L),
            ),
        )
        val aiAnalysisRepository = FakeAiAnalysisRepository(content = content)
        val viewModel = viewModel(
            analysisResultRepository = analysisResultRepository,
            aiAnalysisRepository = aiAnalysisRepository,
        )

        subscribe(viewModel)

        assertEquals(savedContent, viewModel.uiState.value.result?.content)
        assertEquals(0, aiAnalysisRepository.analyzeWorkoutCallCount)
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

        viewModel.handleIntent(WorkoutAnalysisIntent.ClickAnalyze)
        advanceUntilIdle()

        assertEquals(1, aiAnalysisRepository.analyzeWorkoutCallCount)
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
                (AnalysisKind.WORKOUT_DAILY to scopeKey) to
                    AnalysisResult(1L, AnalysisKind.WORKOUT_DAILY, scopeKey, previousContent, 0L),
            ),
        )
        val aiAnalysisRepository = FakeAiAnalysisRepository(content = content)
        aiAnalysisRepository.failure = IllegalStateException("boom")
        val viewModel = viewModel(
            analysisResultRepository = analysisResultRepository,
            aiAnalysisRepository = aiAnalysisRepository,
        )
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutAnalysisIntent.ClickAnalyze)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(previousContent, viewModel.uiState.value.result?.content)
        assertEquals(0, analysisResultRepository.saveCount)
    }

    @Test
    fun `부르지 못한 사유가 있으면 기본 문구 뒤에 붙는다`() = runTest {
        val aiAnalysisRepository = FakeAiAnalysisRepository(content = content)
        aiAnalysisRepository.failure = AiRequestFailedException(null, "요청 형식이 잘못됐습니다")
        val viewModel = viewModel(aiAnalysisRepository = aiAnalysisRepository)
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutAnalysisIntent.ClickAnalyze)
        advanceUntilIdle()

        assertEquals("분석하지 못했습니다: 요청 형식이 잘못됐습니다", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `키가 거절되면 키가 틀렸다고 알린다`() = runTest {
        val aiAnalysisRepository = FakeAiAnalysisRepository(content = content)
        aiAnalysisRepository.failure = AiUnauthorizedException()
        val viewModel = viewModel(aiAnalysisRepository = aiAnalysisRepository)
        subscribe(viewModel)

        viewModel.handleIntent(WorkoutAnalysisIntent.ClickAnalyze)
        advanceUntilIdle()

        assertEquals("키가 올바르지 않습니다", viewModel.uiState.value.errorMessage)
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

        viewModel.handleIntent(WorkoutAnalysisIntent.ClickAnalyze)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isTokenDialogVisible)
        assertEquals(0, aiAnalysisRepository.analyzeWorkoutCallCount)
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

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: WorkoutAnalysisViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun kotlinx.coroutines.test.TestScope.collectEffects(
        viewModel: WorkoutAnalysisViewModel,
    ): List<WorkoutAnalysisEffect> {
        val effects = mutableListOf<WorkoutAnalysisEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        return effects
    }
}
