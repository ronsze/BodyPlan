package kr.sdbk.bodyplan.feature.my.impl.home

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAiCredentialRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAnalysisResultRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeUserProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class MyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val content = AnalysisContent(
        summary = "요약",
        sections = listOf(AnalysisSection(title = "체성분", body = "근육량")),
    )

    private fun viewModel(
        aiCredentialRepository: FakeAiCredentialRepository = FakeAiCredentialRepository(),
        userProfileRepository: FakeUserProfileRepository = FakeUserProfileRepository(),
        analysisResultRepository: FakeAnalysisResultRepository = FakeAnalysisResultRepository(),
    ): MyViewModel = MyViewModel(aiCredentialRepository, userProfileRepository, analysisResultRepository)

    private fun inbodyResult(id: Long, measurement: InbodyMeasurement): AnalysisResult = AnalysisResult(
        id = id,
        kind = AnalysisKind.INBODY,
        scopeKey = "",
        content = content,
        createdAtMillis = 0L,
        measurement = measurement,
    )

    @Test
    fun `연결된 제공자가 없으면 비어 있다`() = runTest {
        val viewModel = viewModel()

        subscribe(viewModel)

        assertNull(viewModel.uiState.value.connectedProvider)
    }

    @Test
    fun `연결된 제공자가 실린다`() = runTest {
        val repository = FakeAiCredentialRepository(AiCredential(AiProvider.GEMINI, "key"))
        val viewModel = viewModel(aiCredentialRepository = repository)

        subscribe(viewModel)

        assertEquals(AiProvider.GEMINI, viewModel.uiState.value.connectedProvider)
    }

    @Test
    fun `연결을 해제하면 표시가 즉시 사라진다`() = runTest {
        val repository = FakeAiCredentialRepository(AiCredential(AiProvider.CLAUDE, "key"))
        val viewModel = viewModel(aiCredentialRepository = repository)
        subscribe(viewModel)

        repository.clear()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.connectedProvider)
    }

    @Test
    fun `토큰 줄을 누르면 토큰 화면으로 이동한다`() = runTest {
        val viewModel = viewModel()
        val effects = mutableListOf<MyEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(MyIntent.ClickAiToken)
        advanceUntilIdle()

        assertTrue(effects.any { it is MyEffect.NavigateToAiToken })
    }

    /**
     * 실제 구현은 measurement 유무만이 아니라 "지금 프로필 값이 그 측정값에서 왔는지"(반올림해 일치하는지)로
     * profileFromInbody를 정한다 — 사용자가 인바디 이후 손수 고치면 배지를 떼기 위해서다.
     * 그래서 아래 테스트들은 프로필 값을 측정값과 일치하도록 맞춰서 검증한다.
     */
    @Test
    fun `최근 인바디의 체중이 반올림해 프로필 체중과 같으면 profileFromInbody가 참이다`() = runTest {
        val userProfileRepository = FakeUserProfileRepository(UserProfile(weightKg = 65))
        val analysisResultRepository = FakeAnalysisResultRepository(
            initialHistory = listOf(inbodyResult(id = 1L, measurement = InbodyMeasurement(weightKg = 65.0))),
        )
        val viewModel = viewModel(
            userProfileRepository = userProfileRepository,
            analysisResultRepository = analysisResultRepository,
        )

        subscribe(viewModel)

        assertTrue(viewModel.uiState.value.profileFromInbody)
    }

    @Test
    fun `최근 인바디의 키가 반올림해 프로필 키와 같으면 profileFromInbody가 참이다`() = runTest {
        val userProfileRepository = FakeUserProfileRepository(UserProfile(heightCm = 172))
        val analysisResultRepository = FakeAnalysisResultRepository(
            initialHistory = listOf(inbodyResult(id = 1L, measurement = InbodyMeasurement(heightCm = 172.0))),
        )
        val viewModel = viewModel(
            userProfileRepository = userProfileRepository,
            analysisResultRepository = analysisResultRepository,
        )

        subscribe(viewModel)

        assertTrue(viewModel.uiState.value.profileFromInbody)
    }

    @Test
    fun `최근 인바디에 체중도 키도 없으면 profileFromInbody가 거짓이다`() = runTest {
        val analysisResultRepository = FakeAnalysisResultRepository(
            initialHistory = listOf(
                inbodyResult(id = 1L, measurement = InbodyMeasurement(skeletalMuscleKg = 30.0)),
            ),
        )
        val viewModel = viewModel(analysisResultRepository = analysisResultRepository)

        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.profileFromInbody)
    }

    @Test
    fun `측정값이 있어도 프로필 값과 다르면(손수 고쳤으면) profileFromInbody가 거짓이다`() = runTest {
        val userProfileRepository = FakeUserProfileRepository(UserProfile(weightKg = 70, heightCm = 180))
        val analysisResultRepository = FakeAnalysisResultRepository(
            initialHistory = listOf(
                inbodyResult(id = 1L, measurement = InbodyMeasurement(weightKg = 65.0, heightCm = 172.0)),
            ),
        )
        val viewModel = viewModel(
            userProfileRepository = userProfileRepository,
            analysisResultRepository = analysisResultRepository,
        )

        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.profileFromInbody)
    }

    @Test
    fun `인바디 이력이 없으면 profileFromInbody가 거짓이다`() = runTest {
        val viewModel = viewModel()

        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.profileFromInbody)
    }

    @Test
    fun `체중 줄을 누르면 체중 화면으로 이동한다`() = runTest {
        val viewModel = viewModel()
        val effects = mutableListOf<MyEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(MyIntent.ClickWeight)
        advanceUntilIdle()

        assertTrue(effects.any { it is MyEffect.NavigateToWeight })
    }

    @Test
    fun `루틴 관리 줄을 누르면 루틴 관리 화면으로 이동한다`() = runTest {
        val viewModel = viewModel()
        val effects = mutableListOf<MyEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(MyIntent.ClickRoutine)
        advanceUntilIdle()

        assertTrue(effects.any { it is MyEffect.NavigateToRoutine })
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: MyViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }
}
