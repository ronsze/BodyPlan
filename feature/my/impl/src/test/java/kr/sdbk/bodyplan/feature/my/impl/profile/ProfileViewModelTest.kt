package kr.sdbk.bodyplan.feature.my.impl.profile

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.Gender
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeUserProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `저장된 값이 입력칸에 채워진다`() = runTest {
        val repository = FakeUserProfileRepository(
            UserProfile(ageYears = 30, heightCm = 175, gender = Gender.FEMALE, goals = setOf(Goal.DIET)),
        )
        val viewModel = ProfileViewModel(repository)

        subscribe(viewModel)

        val input = viewModel.uiState.value.input
        assertEquals("30", input.ageYears)
        assertEquals("175", input.heightCm)
        assertEquals(Gender.FEMALE, input.gender)
        assertEquals(setOf(Goal.DIET), input.goals)
    }

    @Test
    fun `비어 있는 프로필은 빈 칸으로 열린다`() = runTest {
        val viewModel = ProfileViewModel(FakeUserProfileRepository())

        subscribe(viewModel)

        assertEquals(ProfileInput(), viewModel.uiState.value.input)
    }

    @Test
    fun `고친 값이 저장되고 화면이 닫힌다`() = runTest {
        val repository = FakeUserProfileRepository()
        val viewModel = ProfileViewModel(repository)
        val effects = mutableListOf<ProfileEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(ProfileIntent.ChangeInput(ProfileInput(weightKg = "68")))
        viewModel.handleIntent(ProfileIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(68, repository.getProfile().weightKg)
        assertTrue(effects.any { it is ProfileEffect.GoBack })
    }

    @Test
    fun `빈 칸은 값이 없는 것으로 저장된다`() = runTest {
        val repository = FakeUserProfileRepository(UserProfile(ageYears = 30))
        val viewModel = ProfileViewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(ProfileIntent.ChangeInput(ProfileInput()))
        viewModel.handleIntent(ProfileIntent.ClickSave)
        advanceUntilIdle()

        assertNull(repository.getProfile().ageYears)
        assertTrue(repository.getProfile().isEmpty)
    }

    @Test
    fun `공백만 적은 목표는 없는 것으로 저장된다`() = runTest {
        val repository = FakeUserProfileRepository()
        val viewModel = ProfileViewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(ProfileIntent.ChangeInput(ProfileInput(targetNote = "   ")))
        viewModel.handleIntent(ProfileIntent.ClickSave)
        advanceUntilIdle()

        assertNull(repository.getProfile().targetNote)
    }

    @Test
    fun `저장이 실패하면 화면이 닫히지 않는다`() = runTest {
        val repository = FakeUserProfileRepository()
        repository.saveFailure = IllegalStateException("boom")
        val viewModel = ProfileViewModel(repository)
        val effects = mutableListOf<ProfileEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(ProfileIntent.ClickSave)
        advanceUntilIdle()

        assertFalse(effects.any { it is ProfileEffect.GoBack })
        assertTrue(effects.any { it is ProfileEffect.ShowMessage })
        assertFalse(viewModel.uiState.value.isSaving)
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: ProfileViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }
}
