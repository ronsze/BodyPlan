package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate

/**
 * 분석이 지나는 단계.
 *
 * 진짜 진행률은 알 수 없다 — 답을 다 받은 뒤 한 번에 읽으므로 몇 퍼센트인지 셀 것이 없다.
 * 대신 지금 무엇을 하고 있는지를 낸다. 단계는 실제로 일하는 UseCase가 낸다.
 */
enum class AnalysisStage {
    COLLECTING,
    PREPARING_IMAGES,
    CALLING,
    PARSING,
}

enum class AnalysisRunState { RUNNING, SUCCEEDED, FAILED }

/** 지금 돌고 있거나 방금 끝난 분석 하나. 화면은 이것만 보고 그린다. */
data class AnalysisRun(
    val kind: AnalysisKind,
    val scopeKey: String,
    val state: AnalysisRunState,
    val stage: AnalysisStage? = null,
    val failureReason: String? = null,
)

/**
 * 분석 한 번을 시작하는 데 필요한 것.
 *
 * 기간 분석과 인바디가 쓰는 값이 달라 한 타입에 담고, 쓰지 않는 쪽은 비워 둔다.
 * 타입을 둘로 나누면 워커와 저장소가 둘 다 갈래를 알아야 한다.
 */
data class AnalysisRunRequest(
    val kind: AnalysisKind,
    val scopeKey: String,
    val periodLabel: String,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    /** 인바디만 쓴다. 고른 사진. */
    val sourceUri: String? = null,
)
