package kr.sdbk.bodyplan.core.domain.model

/** 종합에 넣는 아래 계층의 결과 하나. */
data class AnalysisChild(
    /** 사람이 읽는 이름. `2026년 9월 8일`이나 `첫 끼니`처럼 무엇을 분석한 것인지. */
    val label: String,
    val content: AnalysisContent,
)

/**
 * 아래 계층의 분석을 모아 위 계층을 만드는 요청.
 *
 * 끼니→하루→주→월이 모두 같은 모양이라 계층마다 타입을 두지 않는다. 무엇을 종합하는지는
 * [kind]가 정하고, 지시문이 그것으로 묶음 제목을 고른다.
 *
 * 사진이나 기록 원문을 다시 보내지 않는다 — 아래에서 이미 읽었고, 다시 보내면 한 달치가
 * 요청 하나에 실린다.
 */
data class AnalysisSummaryRequest(
    val credential: AiCredential,
    val profile: UserProfile,
    val kind: AnalysisKind,
    val periodLabel: String,
    val children: List<AnalysisChild>,
)
