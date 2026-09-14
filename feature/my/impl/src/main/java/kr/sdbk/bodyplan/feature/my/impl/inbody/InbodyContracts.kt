package kr.sdbk.bodyplan.feature.my.impl.inbody

import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.ui.components.failureReason
import kr.sdbk.bodyplan.core.ui.components.isRunning
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class InbodyState(
    /** 고르기만 하고 아직 분석하지 않은 사진. */
    val pickedImageUri: String? = null,
    val history: List<AnalysisResult> = emptyList(),
    /** 이력에서 고른 결과. `null`이면 가장 최근 것을 본다. */
    val selectedResultId: Long? = null,
    val hasCredential: Boolean = false,
    val isLoading: Boolean = false,
    /** 화면 밖에서 도는 분석. 화면이 들고 있지 않아 나갔다 들어와도 그대로다. */
    val run: AnalysisRun? = null,
    val isTokenDialogVisible: Boolean = false,
    val errorMessage: String? = null,
) : State {
    val isAnalyzing: Boolean get() = run.isRunning

    /** 사진을 고르지 않으면 부를 것이 없다. */
    val canAnalyze: Boolean get() = pickedImageUri != null && !isAnalyzing

    /** 화면이 바로 정한 문구가 먼저고, 없으면 돌던 작업이 남긴 사유다. */
    val message: String? get() = errorMessage ?: run.failureReason

    /**
     * 지금 보고 있는 결과. 고른 것이 없으면 가장 최근 것이다.
     *
     * 분석 직후 이력이 갱신되는 순서에 기대지 않으려고 상태로 들고 있지 않고 여기서 고른다.
     */
    val result: AnalysisResult?
        get() {
            // 새 사진을 고르는 중에는 지난 결과를 보이지 않는다. 그대로 두면 그 결과가
            // 방금 고른 사진의 것으로 보인다.
            if (pickedImageUri != null) return null
            return history.firstOrNull { it.id == selectedResultId } ?: history.firstOrNull()
        }

    /** 사진 자리에 그릴 것. */
    val shownImage: String? get() = pickedImageUri ?: result?.imagePath
}

internal sealed interface InbodyIntent : Intent {
    data class PickImage(val uri: String) : InbodyIntent

    data object ClickAnalyze : InbodyIntent

    data class ClickHistory(val id: Long) : InbodyIntent

    data object ConfirmTokenDialog : InbodyIntent

    data object DismissTokenDialog : InbodyIntent
}

internal sealed interface InbodyEffect : Effect {
    data object NavigateToAiToken : InbodyEffect
}
