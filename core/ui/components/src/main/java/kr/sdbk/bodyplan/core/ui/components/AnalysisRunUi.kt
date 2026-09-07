package kr.sdbk.bodyplan.core.ui.components

import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunState
import kr.sdbk.bodyplan.core.domain.model.AnalysisStage

/** 단계를 화면 문구로. 세 분석 화면이 같은 말을 쓴다. */
val AnalysisStage.label: String
    get() = when (this) {
        AnalysisStage.COLLECTING -> "기록을 모으는 중"
        AnalysisStage.PREPARING_IMAGES -> "사진을 준비하는 중"
        AnalysisStage.CALLING -> "AI에게 보내는 중"
        AnalysisStage.PARSING -> "정리하는 중"
    }

/** 돌고 있는지. 화면이 이것을 저장하지 않아야 나갔다 들어와도 그대로다. */
val AnalysisRun?.isRunning: Boolean get() = this?.state == AnalysisRunState.RUNNING

/** 끝난 작업이 남긴 실패 사유. 성공했거나 돌고 있으면 `null`. */
val AnalysisRun?.failureReason: String?
    get() = this?.takeIf { it.state == AnalysisRunState.FAILED }?.failureReason
