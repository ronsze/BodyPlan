package kr.sdbk.bodyplan.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement

/** 분석 결과를 저장해 다시 열 때 부르지 않게 한다. 같은 대상은 마지막 것만 본다. */
interface AnalysisResultRepository {
    fun observeLatest(kind: AnalysisKind, scopeKey: String): Flow<AnalysisResult?>

    /** 열쇠마다 마지막 결과 하나씩. 결과가 없는 열쇠는 담기지 않는다. */
    suspend fun getLatestOf(kind: AnalysisKind, scopeKeys: List<String>): Map<String, AnalysisResult>

    /** 그 종류의 결과를 최신순으로. 인바디 이력이 쓴다. */
    fun observeHistory(kind: AnalysisKind): Flow<List<AnalysisResult>>

    /**
     * [imageFileName]과 [measurement]는 인바디만 채운다. 다른 분석은 넘기지 않는다.
     */
    suspend fun save(
        kind: AnalysisKind,
        scopeKey: String,
        content: AnalysisContent,
        imageFileName: String? = null,
        measurement: InbodyMeasurement? = null,
    )
}
