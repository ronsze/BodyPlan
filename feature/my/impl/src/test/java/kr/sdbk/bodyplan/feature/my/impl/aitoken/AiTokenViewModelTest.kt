package kr.sdbk.bodyplan.feature.my.impl.aitoken

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AiRequestFailedException
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.core.domain.model.OnDeviceDownload
import kr.sdbk.bodyplan.core.domain.model.OnDeviceModelStatus
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAiAnalysisRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAiCredentialRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeOnDeviceAiRepository
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
    private val onDeviceRepository = FakeOnDeviceAiRepository()

    private fun viewModel(
        credentials: FakeAiCredentialRepository = credentialRepository,
        analysis: FakeAiAnalysisRepository = analysisRepository,
        onDevice: FakeOnDeviceAiRepository = onDeviceRepository,
    ) = AiTokenViewModel(credentials, analysis, onDevice)

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

    @Test
    fun `온디바이스 미지원이면 제공자 목록에 온디바이스가 없다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.UNSUPPORTED
        val viewModel = viewModel()

        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.providers.contains(AiProvider.ON_DEVICE))
    }

    @Test
    fun `상태 확인 전에는 제공자 목록에 온디바이스가 없다`() = runTest {
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.providers.contains(AiProvider.ON_DEVICE))
    }

    @Test
    fun `상태 확인이 실패하면 제공자 목록에 온디바이스가 없다`() = runTest {
        onDeviceRepository.statusFailure = IllegalStateException("확인 실패")
        val viewModel = viewModel()

        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.providers.contains(AiProvider.ON_DEVICE))
    }

    @Test
    fun `모델이 준비된 기기는 제공자 목록에 온디바이스가 있다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.READY
        val viewModel = viewModel()

        subscribe(viewModel)

        assertTrue(viewModel.uiState.value.providers.contains(AiProvider.ON_DEVICE))
    }

    @Test
    fun `내려받아야 하는 기기도 제공자 목록에 온디바이스가 있다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.DOWNLOADABLE
        val viewModel = viewModel()

        subscribe(viewModel)

        assertTrue(viewModel.uiState.value.providers.contains(AiProvider.ON_DEVICE))
    }

    @Test
    fun `내려받는 중인 기기도 제공자 목록에 온디바이스가 있다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.DOWNLOADING
        val viewModel = viewModel()

        subscribe(viewModel)

        assertTrue(viewModel.uiState.value.providers.contains(AiProvider.ON_DEVICE))
    }

    @Test
    fun `모델이 준비됐으면 온디바이스를 누르는 즉시 연결된다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.READY
        val repository = FakeAiCredentialRepository()
        val viewModel = viewModel(credentials = repository)
        val effects = mutableListOf<AiTokenEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.ON_DEVICE))
        advanceUntilIdle()

        assertEquals(listOf(AiCredential.onDevice()), analysisRepository.verified)
        assertEquals(AiCredential.onDevice(), viewModel.uiState.value.connected)
        assertNull(viewModel.uiState.value.connectingProvider)
        assertTrue(effects.any { it is AiTokenEffect.ShowMessage && it.message == "연결했습니다" })
        assertNull(viewModel.uiState.value.connectedTokenMask)
    }

    @Test
    fun `모델이 준비된 상태에서 검증이 실패하면 내려받기 화면에 사유가 남는다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.READY
        analysisRepository.verifyFailure = AiRequestFailedException(null, "온디바이스 모델이 준비되지 않았어요")
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.ON_DEVICE))
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(AiProvider.ON_DEVICE, viewModel.uiState.value.connectingProvider)
    }

    @Test
    fun `내려받아야 하면 온디바이스를 눌러도 바로 연결되지 않는다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.DOWNLOADABLE
        val repository = FakeAiCredentialRepository()
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)

        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.ON_DEVICE))
        advanceUntilIdle()

        assertEquals(AiProvider.ON_DEVICE, viewModel.uiState.value.connectingProvider)
        assertEquals(0, repository.saveCallCount)
    }

    @Test
    fun `내려받기를 누르면 진행률이 상태에 반영되고 끝나면 연결된다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.DOWNLOADABLE
        val repository = FakeAiCredentialRepository()
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.ON_DEVICE))
        advanceUntilIdle()

        viewModel.handleIntent(AiTokenIntent.ClickDownloadModel)
        advanceUntilIdle()

        assertEquals(1, onDeviceRepository.downloadCallCount)
        assertTrue(viewModel.uiState.value.isConnecting)

        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            onDeviceRepository.downloads.emit(OnDeviceDownload.Started(1000))
        }
        advanceUntilIdle()
        assertEquals(1000L, viewModel.uiState.value.downloadTotalBytes)

        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            onDeviceRepository.downloads.emit(OnDeviceDownload.Progress(500))
        }
        advanceUntilIdle()
        assertEquals(0.5f, viewModel.uiState.value.downloadRatio)

        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            onDeviceRepository.downloads.emit(OnDeviceDownload.Completed)
        }
        advanceUntilIdle()

        assertEquals(AiCredential.onDevice(), viewModel.uiState.value.connected)
        assertEquals(AiCredential.onDevice(), repository.getCredential())
    }

    @Test
    fun `내려받는 중 취소하면 연결되지 않고 바이트가 되돌아가며, 뒤늦게 완료가 와도 저장되지 않는다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.DOWNLOADABLE
        val repository = FakeAiCredentialRepository()
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.ON_DEVICE))
        advanceUntilIdle()
        viewModel.handleIntent(AiTokenIntent.ClickDownloadModel)
        advanceUntilIdle()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            onDeviceRepository.downloads.emit(OnDeviceDownload.Started(1000))
        }
        advanceUntilIdle()

        viewModel.handleIntent(AiTokenIntent.ClickCancelConnect)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.connectingProvider)
        assertFalse(viewModel.uiState.value.isConnecting)
        assertEquals(0L, viewModel.uiState.value.downloadTotalBytes)
        assertEquals(0L, viewModel.uiState.value.downloadedBytes)
        assertEquals(0, repository.saveCallCount)
        // 취소는 실패가 아니다 — 사유가 남으면 돌아간 화면에 엉뚱한 오류가 보인다.
        assertNull(viewModel.uiState.value.errorMessage)

        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            onDeviceRepository.downloads.emit(OnDeviceDownload.Completed)
        }
        advanceUntilIdle()

        assertEquals(0, repository.saveCallCount)
        assertNull(viewModel.uiState.value.connected)
    }

    @Test
    fun `내려받기가 실패하면 사유가 채워지고 온디바이스를 누른 상태로 남는다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.DOWNLOADABLE
        val viewModel = viewModel()
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.ON_DEVICE))
        advanceUntilIdle()
        viewModel.handleIntent(AiTokenIntent.ClickDownloadModel)
        advanceUntilIdle()

        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            onDeviceRepository.downloads.emit(OnDeviceDownload.Failed("저장 공간이 부족해요"))
        }
        advanceUntilIdle()

        assertEquals("저장 공간이 부족해요", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isConnecting)
        assertEquals(AiProvider.ON_DEVICE, viewModel.uiState.value.connectingProvider)
    }

    @Test
    fun `READY 연결은 검증 중에도 내려받기 화면을 거치지 않는다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.READY
        val gate = CompletableDeferred<Unit>()
        analysisRepository.verifyGate = gate
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.ON_DEVICE))

        assertNull(viewModel.uiState.value.connectingProvider)
        assertTrue(viewModel.uiState.value.isConnecting)
        assertNull(viewModel.uiState.value.connected)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(AiCredential.onDevice(), viewModel.uiState.value.connected)
    }

    @Test
    fun `내려받아 연결하면 온디바이스 상태가 준비됨으로 바뀌어 다시 연결할 때 내려받기를 건너뛴다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.DOWNLOADABLE
        val repository = FakeAiCredentialRepository()
        val viewModel = viewModel(credentials = repository)
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.ON_DEVICE))
        advanceUntilIdle()
        viewModel.handleIntent(AiTokenIntent.ClickDownloadModel)
        advanceUntilIdle()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            onDeviceRepository.downloads.emit(OnDeviceDownload.Completed)
        }
        advanceUntilIdle()
        assertEquals(OnDeviceModelStatus.READY, viewModel.uiState.value.onDeviceStatus)

        viewModel.handleIntent(AiTokenIntent.ClickDisconnect)
        advanceUntilIdle()
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.ON_DEVICE))
        advanceUntilIdle()

        assertEquals(listOf(AiCredential.onDevice(), AiCredential.onDevice()), analysisRepository.verified)
        assertEquals(AiCredential.onDevice(), viewModel.uiState.value.connected)
        assertNull(viewModel.uiState.value.connectingProvider)
    }

    @Test
    fun `내려받기 실패 뒤 다시 내려받으면 이전 수집과 섞이지 않는다`() = runTest {
        onDeviceRepository.status = OnDeviceModelStatus.DOWNLOADABLE
        val viewModel = viewModel()
        subscribe(viewModel)
        viewModel.handleIntent(AiTokenIntent.ClickProvider(AiProvider.ON_DEVICE))
        advanceUntilIdle()
        viewModel.handleIntent(AiTokenIntent.ClickDownloadModel)
        advanceUntilIdle()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            onDeviceRepository.downloads.emit(OnDeviceDownload.Failed("실패"))
        }
        advanceUntilIdle()

        viewModel.handleIntent(AiTokenIntent.ClickDownloadModel)
        advanceUntilIdle()

        assertEquals(2, onDeviceRepository.downloadCallCount)

        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            onDeviceRepository.downloads.emit(OnDeviceDownload.Started(1000))
        }
        advanceUntilIdle()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            onDeviceRepository.downloads.emit(OnDeviceDownload.Progress(500))
        }
        advanceUntilIdle()

        assertEquals(500L, viewModel.uiState.value.downloadedBytes)
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: AiTokenViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }
}
