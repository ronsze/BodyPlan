package kr.sdbk.bodyplan.feature.my.impl.home

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeAiCredentialRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class MyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `연결된 제공자가 없으면 비어 있다`() = runTest {
        val viewModel = MyViewModel(FakeAiCredentialRepository())

        subscribe(viewModel)

        assertNull(viewModel.uiState.value.connectedProvider)
    }

    @Test
    fun `연결된 제공자가 실린다`() = runTest {
        val repository = FakeAiCredentialRepository(AiCredential(AiProvider.GEMINI, "key"))
        val viewModel = MyViewModel(repository)

        subscribe(viewModel)

        assertEquals(AiProvider.GEMINI, viewModel.uiState.value.connectedProvider)
    }

    @Test
    fun `연결을 해제하면 표시가 즉시 사라진다`() = runTest {
        val repository = FakeAiCredentialRepository(AiCredential(AiProvider.CLAUDE, "key"))
        val viewModel = MyViewModel(repository)
        subscribe(viewModel)

        repository.clear()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.connectedProvider)
    }

    @Test
    fun `토큰 줄을 누르면 토큰 화면으로 이동한다`() = runTest {
        val viewModel = MyViewModel(FakeAiCredentialRepository())
        val effects = mutableListOf<MyEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(MyIntent.ClickAiToken)
        advanceUntilIdle()

        assertTrue(effects.any { it is MyEffect.NavigateToAiToken })
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: MyViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }
}
