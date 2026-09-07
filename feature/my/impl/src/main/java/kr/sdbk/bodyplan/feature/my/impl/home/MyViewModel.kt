package kr.sdbk.bodyplan.feature.my.impl.home

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class MyViewModel
@Inject
constructor(
    private val aiCredentialRepository: AiCredentialRepository,
    private val userProfileRepository: UserProfileRepository,
) : BaseViewModel<MyState, MyIntent, MyEffect>(initialState = MyState()) {
    override suspend fun initializeData() {
        viewModelScope.launch {
            combine(
                aiCredentialRepository.observeCredential(),
                userProfileRepository.observeProfile(),
                ::Pair,
            ).collect { (credential, profile) ->
                updateState { it.copy(connectedProvider = credential?.provider, profile = profile) }
            }
        }
    }

    override fun handleIntent(intent: MyIntent) {
        when (intent) {
            is MyIntent.ClickAiToken -> updateEffect(MyEffect.NavigateToAiToken)
            is MyIntent.ClickProfile -> updateEffect(MyEffect.NavigateToProfile)
            is MyIntent.ClickInbody -> updateEffect(MyEffect.NavigateToInbody)
        }
    }
}
