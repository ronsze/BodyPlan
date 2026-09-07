package kr.sdbk.bodyplan.feature.my.impl.home

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class MyViewModel
@Inject
constructor(
    private val aiCredentialRepository: AiCredentialRepository,
    private val userProfileRepository: UserProfileRepository,
    private val analysisResultRepository: AnalysisResultRepository,
) : BaseViewModel<MyState, MyIntent, MyEffect>(initialState = MyState()) {
    override suspend fun initializeData() {
        observeInbodySource()
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

    /**
     * 키·체중이 인바디에서 온 값인지 본다.
     *
     * 인바디 분석이 끝나면 프로필을 말없이 덮어쓰므로, 어디서 온 값인지 보이지 않으면
     * 손수 넣은 값이 사라진 것처럼 보인다.
     */
    private fun observeInbodySource() {
        viewModelScope.launch {
            combine(
                analysisResultRepository.observeHistory(AnalysisKind.INBODY),
                userProfileRepository.observeProfile(),
                ::Pair,
            ).collect { (history, profile) ->
                updateState { it.copy(profileFromInbody = profile.matches(history.firstOrNull()?.measurement)) }
            }
        }
    }

    /**
     * 지금 프로필 값이 그 측정값에서 온 것인지.
     *
     * 인바디가 넣은 뒤 사용자가 손수 고쳤으면 배지를 떼야 한다. 남겨 두면 손으로 넣은 값을
     * 인바디가 넣은 값으로 오인하게 된다.
     */
    private fun UserProfile.matches(measurement: InbodyMeasurement?): Boolean {
        if (measurement == null) return false
        val fromWeight = measurement.weightKg?.roundToInt()?.let { it == weightKg } ?: false
        val fromHeight = measurement.heightCm?.roundToInt()?.let { it == heightCm } ?: false
        return fromWeight || fromHeight
    }

    override fun handleIntent(intent: MyIntent) {
        when (intent) {
            is MyIntent.ClickAiToken -> updateEffect(MyEffect.NavigateToAiToken)
            is MyIntent.ClickProfile -> updateEffect(MyEffect.NavigateToProfile)
            is MyIntent.ClickInbody -> updateEffect(MyEffect.NavigateToInbody)
        }
    }
}
