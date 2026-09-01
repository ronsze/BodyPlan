package kr.sdbk.bodyplan.feature.home.impl

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class HomeViewModel
@Inject
constructor() :
    BaseViewModel<HomeState, HomeIntent, HomeEffect>(
        initialState = HomeState(),
    ) {
    override fun handleIntent(intent: HomeIntent) = Unit
}
