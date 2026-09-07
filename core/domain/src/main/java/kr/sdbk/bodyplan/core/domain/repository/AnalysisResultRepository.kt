package kr.sdbk.bodyplan.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult

/** 분석 결과를 저장해 다시 열 때 부르지 않게 한다. 같은 대상은 마지막 것만 본다. */
interface AnalysisResultRepository {
    fun observeLatest(kind: AnalysisKind, scopeKey: String): Flow<AnalysisResult?>

    /** 열쇠마다 마지막 결과 하나씩. 결과가 없는 열쇠는 담기지 않는다. */
    suspend fun getLatestOf(kind: AnalysisKind, scopeKeys: List<String>): Map<String, AnalysisResult>

    suspend fun save(kind: AnalysisKind, scopeKey: String, content: AnalysisContent)
}
