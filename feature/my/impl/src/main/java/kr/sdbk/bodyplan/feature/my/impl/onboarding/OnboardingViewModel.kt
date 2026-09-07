package kr.sdbk.bodyplan.feature.my.impl.onboarding

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.repository.OnboardingRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

/**
 * 첫 실행에서 한 번 지나가는 화면.
 *
 * 완료 표시가 켜지면 앱 껍데기가 알아서 다음 화면으로 옮긴다. 이 화면은 어디로 갈지 모른다.
 */
@HiltViewModel
internal class OnboardingViewModel
@Inject
constructor(
    private val userProfileRepository: UserProfileRepository,
    private val onboardingRepository: OnboardingRepository,
) : BaseViewModel<OnboardingState, OnboardingIntent, OnboardingEffect>(
    initialState = OnboardingState(),
) {
    override fun handleIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.ChangeInput -> updateState { it.copy(input = intent.input) }
            is OnboardingIntent.ClickStart -> finish(saveProfile = true)
            is OnboardingIntent.ClickSkip -> finish(saveProfile = false)
        }
    }

    private fun finish(saveProfile: Boolean) {
        if (state.value.isSaving) return
        val input = state.value.input
        updateState { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                if (saveProfile) userProfileRepository.saveProfile(input.toProfile())
                onboardingRepository.markCompleted()
            }.onFailure {
                updateState { it.copy(isSaving = false) }
                updateEffect(OnboardingEffect.ShowMessage(SAVE_FAILED))
            }
        }
    }
}

private const val SAVE_FAILED = "저장하지 못했습니다"
