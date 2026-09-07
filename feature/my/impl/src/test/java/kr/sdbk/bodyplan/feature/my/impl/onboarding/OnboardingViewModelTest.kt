package kr.sdbk.bodyplan.feature.my.impl.onboarding

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.Gender
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeOnboardingRepository
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeUserProfileRepository
import kr.sdbk.bodyplan.feature.my.impl.profile.ProfileInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profileRepository = FakeUserProfileRepository()
    private val onboardingRepository = FakeOnboardingRepository()

    private fun viewModel() = OnboardingViewModel(profileRepository, onboardingRepository)

    @Test
    fun `처음에는 모든 칸이 비어 있다`() = runTest {
        val viewModel = viewModel()

        subscribe(viewModel)

        assertEquals(ProfileInput(), viewModel.uiState.value.input)
    }

    @Test
    fun `시작하면 입력한 값이 저장되고 온보딩이 지나간 것으로 남는다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(
            OnboardingIntent.ChangeInput(
                ProfileInput(
                    ageYears = "30",
                    heightCm = "175",
                    gender = Gender.MALE,
                    goals = setOf(Goal.DIET),
                ),
            ),
        )
        viewModel.handleIntent(OnboardingIntent.ClickStart)
        advanceUntilIdle()

        val saved = profileRepository.getProfile()
        assertEquals(30, saved.ageYears)
        assertEquals(175, saved.heightCm)
        assertEquals(Gender.MALE, saved.gender)
        assertEquals(setOf(Goal.DIET), saved.goals)
        assertTrue(onboardingRepository.isCompleted)
    }

    @Test
    fun `건너뛰면 아무 값도 저장하지 않고 지나간 것으로만 남는다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(OnboardingIntent.ChangeInput(ProfileInput(ageYears = "30")))
        viewModel.handleIntent(OnboardingIntent.ClickSkip)
        advanceUntilIdle()

        assertEquals(0, profileRepository.saveCount)
        assertTrue(onboardingRepository.isCompleted)
    }

    @Test
    fun `아무것도 채우지 않고 시작해도 지나간 것으로 남는다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(OnboardingIntent.ClickStart)
        advanceUntilIdle()

        assertTrue(profileRepository.getProfile().isEmpty)
        assertTrue(onboardingRepository.isCompleted)
    }

    @Test
    fun `저장이 실패하면 지나간 것으로 남지 않고 다시 시도할 수 있다`() = runTest {
        profileRepository.saveFailure = IllegalStateException("boom")
        val viewModel = viewModel()
        val effects = mutableListOf<OnboardingEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        subscribe(viewModel)

        viewModel.handleIntent(OnboardingIntent.ClickStart)
        advanceUntilIdle()

        assertFalse(onboardingRepository.isCompleted)
        assertTrue(effects.any { it is OnboardingEffect.ShowMessage })
        assertFalse(viewModel.uiState.value.isSaving)
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: OnboardingViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }
}
