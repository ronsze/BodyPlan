package kr.sdbk.bodyplan.core.domain.model

/**
 * 인바디 결과지에서 읽어 낸 값.
 *
 * 사진을 AI가 읽은 추정값이라 틀릴 수 있다. 화면은 그 사실을 함께 보여준다.
 * 결과지가 항목을 늘 싣지는 않아 값마다 없을 수 있다 — 특히 키는 기기가 물어보고
 * 출력에 찍지 않는 경우가 흔하다.
 */
data class InbodyMeasurement(
    val weightKg: Double? = null,
    val skeletalMuscleKg: Double? = null,
    val bodyFatKg: Double? = null,
    val heightCm: Double? = null,
) {
    val isEmpty: Boolean
        get() = weightKg == null && skeletalMuscleKg == null && bodyFatKg == null && heightCm == null
}

/** 인바디 분석 한 번의 결과. 사람이 읽을 글과 기계가 쓸 숫자를 함께 낸다. */
data class InbodyAnalysis(val content: AnalysisContent, val measurement: InbodyMeasurement)
