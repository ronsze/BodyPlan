package kr.sdbk.bodyplan.feature.dietlog.impl.analysis

import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class DietAnalysisState(
    val periodLabel: String,
    val kind: AnalysisKind,
    val result: AnalysisResult? = null,
    val hasCredential: Boolean = false,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isTokenDialogVisible: Boolean = false,
    val errorMessage: String? = null,
) : State {
    val canAnalyze: Boolean get() = !isAnalyzing && !isLoading
}

internal sealed interface DietAnalysisIntent : Intent {
    data object ClickAnalyze : DietAnalysisIntent

    data object ClickBack : DietAnalysisIntent

    data object ConfirmTokenDialog : DietAnalysisIntent

    data object DismissTokenDialog : DietAnalysisIntent
}

internal sealed interface DietAnalysisEffect : Effect {
    data object GoBack : DietAnalysisEffect

    data object NavigateToAiToken : DietAnalysisEffect
}
