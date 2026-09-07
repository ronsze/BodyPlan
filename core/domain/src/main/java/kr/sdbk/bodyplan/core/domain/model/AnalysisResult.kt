package kr.sdbk.bodyplan.core.domain.model

/** 어떤 대상을 분석한 것인지. 저장된 결과를 다시 찾을 때 범위와 함께 열쇠가 된다. */
enum class AnalysisKind {
    DIET_DAILY,
    DIET_WEEKLY,
    DIET_MONTHLY,

    WORKOUT_DAILY,
    WORKOUT_WEEKLY,
    WORKOUT_MONTHLY,
    INBODY,
}

/** 분석이 덮는 기간의 단위. 인바디는 기간이 없어 [NONE]이다. */
enum class AnalysisPeriodUnit { DAY, WEEK, MONTH, NONE }

val AnalysisKind.periodUnit: AnalysisPeriodUnit
    get() = when (this) {
        AnalysisKind.DIET_DAILY, AnalysisKind.WORKOUT_DAILY -> AnalysisPeriodUnit.DAY
        AnalysisKind.DIET_WEEKLY, AnalysisKind.WORKOUT_WEEKLY -> AnalysisPeriodUnit.WEEK
        AnalysisKind.DIET_MONTHLY, AnalysisKind.WORKOUT_MONTHLY -> AnalysisPeriodUnit.MONTH
        AnalysisKind.INBODY -> AnalysisPeriodUnit.NONE
    }

/** 결과를 이루는 묶음 하나. 제목은 AI가 붙인 것을 그대로 쓴다. */
data class AnalysisSection(val title: String, val body: String)

/**
 * AI가 답한 내용.
 *
 * 한눈에 보는 요약과, 갈라 보여줄 묶음 목록이다. 묶음의 제목과 개수는 분석 종류마다 달라
 * 여기서 고정하지 않는다 — 식단 날짜별은 먹은 음식·칼로리·영양 성분·개선 방향이고,
 * 주간·월간은 기간 흐름·칼로리 흐름·영양 균형·개선 방향이다.
 */
data class AnalysisContent(val summary: String, val sections: List<AnalysisSection>)

/**
 * 저장된 분석 결과.
 *
 * [scopeKey]는 같은 대상의 결과를 다시 찾는 열쇠다. 날짜별은 epochDay, 주간은 그 주 첫날의
 * epochDay, 월간은 `yyyy-MM`이다. 인바디는 매번 새 결과를 쌓으므로 빈 문자열이다.
 *
 * [imagePath]는 인바디가 분석한 사진이다. 다른 분석은 `null`이다.
 */
data class AnalysisResult(
    val id: Long,
    val kind: AnalysisKind,
    val scopeKey: String,
    val content: AnalysisContent,
    val createdAtMillis: Long,
    val imagePath: String? = null,
)
