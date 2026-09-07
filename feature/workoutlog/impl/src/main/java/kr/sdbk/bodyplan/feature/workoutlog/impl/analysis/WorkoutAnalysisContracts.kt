package kr.sdbk.bodyplan.feature.workoutlog.impl.analysis

import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class WorkoutAnalysisState(
    val periodLabel: String,
    val result: AnalysisResult? = null,
    val hasCredential: Boolean = false,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isTokenDialogVisible: Boolean = false,
    val errorMessage: String? = null,
) : State {
    val canAnalyze: Boolean get() = !isAnalyzing && !isLoading
}

internal sealed interface WorkoutAnalysisIntent : Intent {
    data object ClickAnalyze : WorkoutAnalysisIntent

    data object ClickBack : WorkoutAnalysisIntent

    data object ConfirmTokenDialog : WorkoutAnalysisIntent

    data object DismissTokenDialog : WorkoutAnalysisIntent
}

internal sealed interface WorkoutAnalysisEffect : Effect {
    data object GoBack : WorkoutAnalysisEffect

    data object NavigateToAiToken : WorkoutAnalysisEffect
}
