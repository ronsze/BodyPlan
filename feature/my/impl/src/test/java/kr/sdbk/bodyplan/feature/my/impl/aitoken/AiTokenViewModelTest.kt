package kr.sdbk.bodyplan.feature.my.impl.aitoken

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AiRequestFailedException
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAiAnalysisRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAiCredentialRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class AiTokenViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val credentialRepository = FakeAiCredentialRepository()
    private val analysisRepository = FakeAiAnalysisRepository()

    private fun viewModel(
        credentials: FakeAiCredentialRepository = credentialRepository,
        analysis: FakeAiAnalysisRepository = analysisRepository,
    ) = AiTokenViewModel(credentials, analysis)

    @Test
    fun `저장된 키가 없으면 고르는 중이다`() = runTest {
        val viewModel = viewModel()

        subscribe(viewModel)

        assertNull(viewModel.uiState.value.connected)
        assertNull(viewModel.uiState.value.connectingProvider)
    }

    @Test
    fun `저장된 키가 있으면 들어오자마자 채워진다`() = runTest {
        val saved = AiCredential(AiProvider.GPT, "sk-1234567890")
        val repository = FakeAiCredentialRepository(saved)
        val viewModel = viewModel(credentials = repository)

        subscribe(viewModel)

        assertEquals(saved, viewModel.uiState.value.connected)
    }

    @Test
    fun `제공자를 누르면 그 제공자로 키 넣는 중이 되고 입력이 비워진다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ChangeInput("남아있던값"))

        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.GEMINI))

        assertEquals(AiProvider.GEMINI, viewModel.uiState.value.connectingProvider)
        assertEquals("", viewModel.uiState.value.input)
    }

    @Test
    fun `검증을 통과해야 저장한다`() = runTest {
        val repository = FakeAiCredentialRepository()
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.GEMINI))
        viewModel.handleIntent(AiTokenIntent.ChangeInput("  key-1234  "))

        viewModel.handleIntent(AiTokenIntent.ClickConnect)
        advanceUntilIdle()

        assertEquals(listOf(AiCredential(AiProvider.GEMINI, "key-1234")), analysisRepository.verified)
        assertEquals(AiCredential(AiProvider.GEMINI, "key-1234"), repository.getCredential())
    }

    @Test
    fun `검증이 실패하면 저장 요청이 가지 않는다`() = runTest {
        analysisRepository.verifyFailure = AiUnauthorizedException()
        val repository = FakeAiCredentialRepository()
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.CLAUDE))
        viewModel.handleIntent(AiTokenIntent.ChangeInput("sk-bad-key"))

        viewModel.handleIntent(AiTokenIntent.ClickConnect)
        advanceUntilIdle()

        assertEquals(0, repository.saveCallCount)
    }

    @Test
    fun `검증 실패 시 넣은 키는 그대로 남고 오류가 채워진다`() = runTest {
        analysisRepository.verifyFailure = AiUnauthorizedException()
        val viewModel = viewModel()
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.CLAUDE))
        viewModel.handleIntent(AiTokenIntent.ChangeInput("sk-bad-key"))

        viewModel.handleIntent(AiTokenIntent.ClickConnect)
        advanceUntilIdle()

        assertEquals("sk-bad-key", viewModel.uiState.value.input)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `키가 거절되면 키가 틀렸다고 알린다`() = runTest {
        analysisRepository.verifyFailure = AiUnauthorizedException()
        val viewModel = viewModel()
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.CLAUDE))
        viewModel.handleIntent(AiTokenIntent.ChangeInput("sk-bad-key"))

        viewModel.handleIntent(AiTokenIntent.ClickConnect)
        advanceUntilIdle()

        assertEquals("키가 올바르지 않습니다", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `사유가 있는 요청 실패는 사유를 붙여 알린다`() = runTest {
        analysisRepository.verifyFailure = AiRequestFailedException(null, "요청 형식이 잘못됐습니다")
        val viewModel = viewModel()
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.CLAUDE))
        viewModel.handleIntent(AiTokenIntent.ChangeInput("sk-1234567890"))

        viewModel.handleIntent(AiTokenIntent.ClickConnect)
        advanceUntilIdle()

        assertEquals("연결하지 못했습니다: 요청 형식이 잘못됐습니다", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `사유 없는 요청 실패는 기본 문구로 알린다`() = runTest {
        analysisRepository.verifyFailure = AiRequestFailedException(null)
        val viewModel = viewModel()
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.CLAUDE))
        viewModel.handleIntent(AiTokenIntent.ChangeInput("sk-1234567890"))

        viewModel.handleIntent(AiTokenIntent.ClickConnect)
        advanceUntilIdle()

        assertEquals("연결하지 못했습니다", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `연동에 성공하면 입력과 연결 중 상태가 비워지고 메시지가 난다`() = runTest {
        val viewModel = viewModel()
        val effects = mutableListOf<AiTokenEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.CLAUDE))
        viewModel.handleIntent(AiTokenIntent.ChangeInput("sk-1234567890"))

        viewModel.handleIntent(AiTokenIntent.ClickConnect)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.connectingProvider)
        assertEquals("", viewModel.uiState.value.input)
        assertTrue(effects.any { it is AiTokenEffect.ShowMessage && it.message == "연결했습니다" })
    }

    @Test
    fun `입력이 비어 있으면 연결할 수 없고 시도해도 아무 일도 없다`() = runTest {
        val repository = FakeAiCredentialRepository()
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.CLAUDE))

        assertFalse(viewModel.uiState.value.canConnect)

        viewModel.handleIntent(AiTokenIntent.ClickConnect)
        advanceUntilIdle()

        assertTrue(analysisRepository.verified.isEmpty())
        assertEquals(0, repository.saveCallCount)
    }

    @Test
    fun `연결 취소는 연결 중 상태와 입력, 오류를 되돌린다`() = runTest {
        analysisRepository.verifyFailure = AiUnauthorizedException()
        val viewModel = viewModel()
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.CLAUDE))
        viewModel.handleIntent(AiTokenIntent.ChangeInput("sk-bad-key"))
        viewModel.handleIntent(AiTokenIntent.ClickConnect)
        advanceUntilIdle()

        viewModel.handleIntent(AiTokenIntent.ClickCancelConnect)

        assertNull(viewModel.uiState.value.connectingProvider)
        assertEquals("", viewModel.uiState.value.input)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `연결을 해제하면 저장소가 비워지고 해제 메시지가 난다`() = runTest {
        val saved = AiCredential(AiProvider.CLAUDE, "sk-1234567890")
        val repository = FakeAiCredentialRepository(saved)
        val viewModel = viewModel(credentials = repository)
        val effects = mutableListOf<AiTokenEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(AiTokenIntent.ClickDisconnect)
        advanceUntilIdle()

        assertNull(repository.getCredential())
        assertTrue(effects.any { it is AiTokenEffect.ShowMessage && it.message == "연결을 해제했습니다" })
    }

    @Test
    fun `저장된 키는 전체를 드러내지 않는다`() = runTest {
        val repository = FakeAiCredentialRepository(AiCredential(AiProvider.CLAUDE, "sk-ant-abcdefgh"))
        val viewModel = viewModel(credentials = repository)

        subscribe(viewModel)

        val mask = viewModel.uiState.value.connectedTokenMask
        assertNotNull(mask)
        assertFalse(mask!!.contains("sk-ant-abcdefgh"))
    }

    @Test
    fun `뒤로 가기는 화면을 닫는 이벤트를 낸다`() = runTest {
        val viewModel = viewModel()
        val effects = mutableListOf<AiTokenEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(AiTokenIntent.ClickBack)
        advanceUntilIdle()

        assertTrue(effects.any { it is AiTokenEffect.GoBack })
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: AiTokenViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }
}
