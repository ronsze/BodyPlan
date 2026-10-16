package kr.sdbk.bodyplan.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunRequest

/**
 * 분석을 화면 밖에서 돌린다.
 *
 * 화면의 수명에 묶어 돌리면 나가는 순간 요청이 취소돼, 사용자 키로 부른 호출이 결과 없이
 * 버려진다. 여기 맡긴 요청은 화면을 나가도, 앱이 죽었다 살아나도 이어진다.
 */
interface AnalysisRunner {
    /** 같은 대상이 이미 돌고 있으면 새로 넣지 않는다. */
    suspend fun start(request: AnalysisRunRequest)

    /** 그 대상의 지금 상태. 돌린 적이 없으면 `null`. */
    fun observe(kind: AnalysisKind, scopeKey: String): Flow<AnalysisRun?>

    /** 그 종류의 모든 작업 상태. 목록 화면이 기록마다 따로 구독하지 않게 한다. */
    fun observeAll(kind: AnalysisKind): Flow<List<AnalysisRun>>
}
