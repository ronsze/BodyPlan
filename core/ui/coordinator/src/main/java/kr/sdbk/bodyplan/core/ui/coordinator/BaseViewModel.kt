package kr.sdbk.bodyplan.core.ui.coordinator

import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel<S : State, I : Intent, E : Effect>(
    protected val initialState: S,
    enableReInitialization: Boolean = false,
) : BaseViewModelImpl<S, S, I, E>(initialState, enableReInitialization) {
    override val uiState: StateFlow<S> = state.setup(initialUiState = initialState)
}
