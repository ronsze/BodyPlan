package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate

/** 분석에 넘길 식단 기록 한 건. 사진은 경로로만 넘기고 읽는 일은 데이터 계층이 한다. */
data class DietAnalysisEntry(val date: LocalDate, val memo: String?, val imagePath: String)

/**
 * 끼니 하나를 분석하는 데 필요한 것 전부.
 *
 * [periodLabel]은 사람이 읽는 표기다. AI가 답에 그대로 쓸 수 있게 넘긴다.
 * [entries]가 목록인 것은 한 끼니에 사진이 여럿일 수 있어서다.
 */
data class DietAnalysisRequest(
    val credential: AiCredential,
    val profile: UserProfile,
    val periodLabel: String,
    val entries: List<DietAnalysisEntry>,
)
