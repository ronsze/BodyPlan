package kr.sdbk.bodyplan.feature.dietlog.impl.analysis

import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.ui.components.failureReason
import kr.sdbk.bodyplan.core.ui.components.isRunning
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class DietAnalysisState(
    val periodLabel: String,
    /** 끼니 분석과 기간 분석의 제목이 다르다. */
    val title: String = "식단 분석",
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
