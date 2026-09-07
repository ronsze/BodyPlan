package kr.sdbk.bodyplan.feature.my.impl.profile

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class ProfileViewModel
@Inject
constructor(private val userProfileRepository: UserProfileRepository) :
    BaseViewModel<ProfileState, ProfileIntent, ProfileEffect>(initialState = ProfileState()) {
    override suspend fun initializeData() {
        updateState { it.copy(isLoading = true) }
        val profile = runCatching { userProfileRepository.getProfile() }.getOrNull()
        updateState {
            it.copy(
                isLoading = false,
                input = profile?.let(ProfileInput::from) ?: it.input,
            )
        }
    }

    override fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.ChangeInput -> updateState { it.copy(input = intent.input) }
            is ProfileIntent.ClickSave -> save()
            is ProfileIntent.ClickBack -> updateEffect(ProfileEffect.GoBack)
        }
    }

    private fun save() {
        val input = state.value.input
        if (state.value.isSaving) return
        updateState { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching { userProfileRepository.saveProfile(input.toProfile()) }
                .onSuccess {
                    updateState { it.copy(isSaving = false) }
                    updateEffect(ProfileEffect.GoBack)
                }
                .onFailure {
                    updateState { it.copy(isSaving = false) }
                    updateEffect(ProfileEffect.ShowMessage(SAVE_FAILED))
                }
        }
    }
}

private const val SAVE_FAILED = "저장하지 못했습니다"
