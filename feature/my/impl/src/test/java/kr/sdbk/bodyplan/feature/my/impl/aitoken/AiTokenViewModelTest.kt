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
    fun `저장된 것이 없으면 클로드가 골라져 있고 저장할 수 없다`() = runTest {
        val viewModel = viewModel()

        subscribe(viewModel)

        assertEquals(AiProvider.CLAUDE, viewModel.uiState.value.selectedProvider)
        assertNull(viewModel.uiState.value.saved)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `이미 연결된 제공자가 골라진 채로 열린다`() = runTest {
        val repository = FakeAiCredentialRepository(AiCredential(AiProvider.GPT, "sk-1234567890"))
        val viewModel = viewModel(credentials = repository)

        subscribe(viewModel)

        assertEquals(AiProvider.GPT, viewModel.uiState.value.selectedProvider)
    }

    @Test
    fun `키를 넣으면 저장할 수 있다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(AiTokenIntent.ChangeInput("sk-1234"))

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `저장하면 고른 제공자와 함께 남고 입력칸이 비워진다`() = runTest {
        val repository = FakeAiCredentialRepository()
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)

        viewModel.handleIntent(AiTokenIntent.SelectProvider(AiProvider.GEMINI))
        viewModel.handleIntent(AiTokenIntent.ChangeInput("  key-1234  "))
        viewModel.handleIntent(AiTokenIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(AiCredential(AiProvider.GEMINI, "key-1234"), repository.getCredential())
        assertEquals("", viewModel.uiState.value.input)
    }

    @Test
    fun `저장된 키는 앞뒤만 보인다`() = runTest {
        val repository = FakeAiCredentialRepository(AiCredential(AiProvider.CLAUDE, "sk-ant-abcdefgh"))
        val viewModel = viewModel(credentials = repository)

        subscribe(viewModel)

        val mask = viewModel.uiState.value.savedTokenMask
        assertNotNull(mask)
        assertTrue(mask!!.startsWith("sk-a"))
        assertTrue(mask.endsWith("efgh"))
        assertFalse(mask.contains("ant"))
    }

    @Test
    fun `연결을 해제하면 저장된 것이 사라진다`() = runTest {
        val repository = FakeAiCredentialRepository(AiCredential(AiProvider.CLAUDE, "sk-1234567890"))
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)

        viewModel.handleIntent(AiTokenIntent.ClickDisconnect)
        advanceUntilIdle()

        assertNull(repository.getCredential())
        assertNull(viewModel.uiState.value.saved)
    }

    @Test
    fun `저장된 것이 없으면 확인도 해제도 할 수 없다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.canVerify)
        assertFalse(viewModel.uiState.value.canDisconnect)

        viewModel.handleIntent(AiTokenIntent.ClickVerify)
        advanceUntilIdle()

        assertTrue(analysisRepository.verified.isEmpty())
    }

    @Test
    fun `연결 확인은 저장된 한 벌로 한다`() = runTest {
        val saved = AiCredential(AiProvider.CLAUDE, "sk-saved-1234")
        val repository = FakeAiCredentialRepository(saved)
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)

        viewModel.handleIntent(AiTokenIntent.ChangeInput("sk-typing-9999"))
        viewModel.handleIntent(AiTokenIntent.ClickVerify)
        advanceUntilIdle()

        assertEquals(listOf(saved), analysisRepository.verified)
        assertFalse(viewModel.uiState.value.isVerifying)
    }

    @Test
    fun `키가 거절되면 키가 틀렸다고 알린다`() = runTest {
        val repository = FakeAiCredentialRepository(AiCredential(AiProvider.CLAUDE, "sk-1234567890"))
        analysisRepository.verifyFailure = AiUnauthorizedException()
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)

        viewModel.handleIntent(AiTokenIntent.ClickVerify)
        advanceUntilIdle()

        assertEquals("키가 올바르지 않습니다", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isVerifying)
    }

    @Test
    fun `부르지 못하면 연결 실패로 알린다`() = runTest {
        val repository = FakeAiCredentialRepository(AiCredential(AiProvider.CLAUDE, "sk-1234567890"))
        analysisRepository.verifyFailure = AiRequestFailedException(null)
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)

        viewModel.handleIntent(AiTokenIntent.ClickVerify)
        advanceUntilIdle()

        assertEquals("연결하지 못했습니다", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `제공자를 바꾸면 앞선 실패 문구가 사라진다`() = runTest {
        val repository = FakeAiCredentialRepository(AiCredential(AiProvider.CLAUDE, "sk-1234567890"))
        analysisRepository.verifyFailure = AiUnauthorizedException()
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickVerify)
        advanceUntilIdle()

        viewModel.handleIntent(AiTokenIntent.SelectProvider(AiProvider.GPT))

        assertNull(viewModel.uiState.value.errorMessage)
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
