package kr.sdbk.bodyplan.feature.workoutlog.impl.analysis

import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutAnalysisPeriod

/** 화면의 기간 선택을 저장·분석이 쓰는 종류로 옮긴다. 기간 계산 자체는 공용이 한다. */
internal val WorkoutAnalysisPeriod.analysisKind: AnalysisKind
    get() = when (this) {
        WorkoutAnalysisPeriod.DAILY -> AnalysisKind.WORKOUT_DAILY
        WorkoutAnalysisPeriod.WEEKLY -> AnalysisKind.WORKOUT_WEEKLY
        WorkoutAnalysisPeriod.MONTHLY -> AnalysisKind.WORKOUT_MONTHLY
    }
