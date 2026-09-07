package kr.sdbk.bodyplan.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kr.sdbk.bodyplan.core.domain.repository.OnboardingRepository

/**
 * 앱 껍데기가 첫 화면을 정하는 데 필요한 값 하나.
 *
 * 화면이 아니라 껍데기의 상태라 MVI 한 벌을 두지 않는다. 온보딩을 지나갔는지만 본다.
 * `null`은 아직 읽지 못한 것이다 — 모르는 채로 첫 화면을 정하면 온보딩이 잠깐 스친다.
 */
@HiltViewModel
class MainShellViewModel
@Inject
constructor(onboardingRepository: OnboardingRepository) : ViewModel() {
    val onboardingCompleted: StateFlow<Boolean?> = onboardingRepository.observeCompleted()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
            initialValue = null,
        )

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
