package kr.sdbk.bodyplan.feature.home

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kr.sdbk.bodyplan.core.ui.mvi.MviViewModel

@HiltViewModel
class HomeViewModel
@Inject
constructor() : MviViewModel<HomeUiState, HomeIntent, HomeEffect>(HomeUiState()) {
    override fun onIntent(intent: HomeIntent) = Unit
}
