package kr.sdbk.bodyplan.feature.home

import kr.sdbk.bodyplan.core.ui.mvi.UiEffect
import kr.sdbk.bodyplan.core.ui.mvi.UiIntent
import kr.sdbk.bodyplan.core.ui.mvi.UiState

data class HomeUiState(val isLoading: Boolean = false) : UiState

sealed interface HomeIntent : UiIntent

sealed interface HomeEffect : UiEffect
