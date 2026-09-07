package kr.sdbk.bodyplan.feature.dietlog.impl.analysis

import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.feature.dietlog.api.DietAnalysisPeriod

/** 화면의 기간 선택을 저장·분석이 쓰는 종류로 옮긴다. 기간 계산 자체는 공용이 한다. */
internal val DietAnalysisPeriod.analysisKind: AnalysisKind
    get() = when (this) {
        DietAnalysisPeriod.DAILY -> AnalysisKind.DIET_DAILY
        DietAnalysisPeriod.WEEKLY -> AnalysisKind.DIET_WEEKLY
        DietAnalysisPeriod.MONTHLY -> AnalysisKind.DIET_MONTHLY
    }
