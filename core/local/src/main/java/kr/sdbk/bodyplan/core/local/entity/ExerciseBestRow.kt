package kr.sdbk.bodyplan.core.local.entity

/** 개인 기록 판정용 조회 결과. 종목마다 세트 전체를 읽지 않으려고 집계값만 담는다. */
data class ExerciseBestRow(
    val exerciseId: Long,
    val intensityType: String,
    val maxIntensityValue: Int,
    val maxRepeatCount: Int,
)
