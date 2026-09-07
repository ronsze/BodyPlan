package kr.sdbk.bodyplan.feature.my.impl.home

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class MyViewModel
@Inject
constructor(private val aiCredentialRepository: AiCredentialRepository) :
    BaseViewModel<MyState, MyIntent, MyEffect>(initialState = MyState()) {
    override suspend fun initializeData() {
        viewModelScope.launch {
            aiCredentialRepository.observeCredential().collect { credential ->
                updateState { it.copy(connectedProvider = credential?.provider) }
            }
        }
    }

    override fun handleIntent(intent: MyIntent) {
        when (intent) {
            is MyIntent.ClickAiToken -> updateEffect(MyEffect.NavigateToAiToken)
        }
    }
}
