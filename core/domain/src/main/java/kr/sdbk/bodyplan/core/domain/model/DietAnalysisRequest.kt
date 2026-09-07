package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate

/** 분석에 넘길 식단 기록 한 건. 사진은 경로로만 넘기고 읽는 일은 데이터 계층이 한다. */
data class DietAnalysisEntry(val date: LocalDate, val memo: String?, val imagePath: String)

/**
 * 식단 분석 한 번에 필요한 것 전부.
 *
 * [periodLabel]은 사람이 읽는 기간 표기다. AI가 답에 그대로 쓸 수 있게 넘긴다.
 */
data class DietAnalysisRequest(
    val credential: AiCredential,
    val profile: UserProfile,
    val periodLabel: String,
    val entries: List<DietAnalysisEntry>,
)

/** 종합에 넘길 하루치 분석 하나. 사진은 이미 그 날 분석에서 읽혔으므로 다시 넘기지 않는다. */
data class DietDailySummary(val date: LocalDate, val content: AnalysisContent)

/**
 * 주간·월간 종합 한 번에 필요한 것 전부.
 *
 * 기간의 사진을 다시 보내면 요청이 커져 느리고 비싸다. 날짜별로 이미 분석한 글을 모아 넘긴다.
 */
data class DietSummaryRequest(
    val credential: AiCredential,
    val profile: UserProfile,
    val periodLabel: String,
    val dailyResults: List<DietDailySummary>,
)
