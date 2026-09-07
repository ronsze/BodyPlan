package kr.sdbk.bodyplan.feature.dietlog.impl

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class DietLogViewModel
@Inject
constructor() :
    BaseViewModel<DietLogState, DietLogIntent, DietLogEffect>(
        initialState = DietLogState(),
    ) {
    override fun handleIntent(intent: DietLogIntent) = Unit
}
