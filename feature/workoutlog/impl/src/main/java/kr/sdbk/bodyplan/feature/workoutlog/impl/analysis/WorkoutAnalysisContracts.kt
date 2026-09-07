package kr.sdbk.bodyplan.feature.workoutlog.impl.analysis

import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.ui.components.failureReason
import kr.sdbk.bodyplan.core.ui.components.isRunning
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class WorkoutAnalysisState(
    val periodLabel: String,
    /** 기록 분석과 기간 분석의 제목이 다르다. */
    val title: String = "운동 분석",
    val result: AnalysisResult? = null,
    val hasCredential: Boolean = false,
    val isLoading: Boolean = false,
    /** 화면 밖에서 도는 분석. 화면이 들고 있지 않아 나갔다 들어와도 그대로다. */
    val run: AnalysisRun? = null,
    val isTokenDialogVisible: Boolean = false,
    val errorMessage: String? = null,
) : State {
    val isAnalyzing: Boolean get() = run.isRunning

    val canAnalyze: Boolean get() = !isAnalyzing && !isLoading

    /** 화면이 바로 정한 문구가 먼저고, 없으면 돌던 작업이 남긴 사유다. */
    val message: String? get() = errorMessage ?: run.failureReason
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
