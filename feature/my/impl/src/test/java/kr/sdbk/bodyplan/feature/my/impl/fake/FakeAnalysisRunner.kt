package kr.sdbk.bodyplan.feature.my.impl.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunRequest
import kr.sdbk.bodyplan.core.domain.repository.AnalysisRunner

/**
 * `start` 호출을 기록하고, `observe`·`observeAll`이 내는 값을 테스트가 [flowFor]·[runsFor]로 밀어 넣을 수 있다.
 * [startFailure]가 있으면 `start`가 호출을 기록한 뒤 던진다.
 */
internal class FakeAnalysisRunner : AnalysisRunner {
    val startCalls: MutableList<AnalysisRunRequest> = mutableListOf()
    var startFailure: Throwable? = null
    private val flows = mutableMapOf<Pair<AnalysisKind, String>, MutableStateFlow<AnalysisRun?>>()
    private val allFlows = mutableMapOf<AnalysisKind, MutableStateFlow<List<AnalysisRun>>>()

    override suspend fun start(request: AnalysisRunRequest) {
        startCalls += request
        startFailure?.let { throw it }
    }

    override fun observe(kind: AnalysisKind, scopeKey: String): Flow<AnalysisRun?> = flowFor(kind, scopeKey)

    override fun observeAll(kind: AnalysisKind): Flow<List<AnalysisRun>> = runsFor(kind)

    fun flowFor(kind: AnalysisKind, scopeKey: String): MutableStateFlow<AnalysisRun?> =
        flows.getOrPut(kind to scopeKey) { MutableStateFlow(null) }

    fun runsFor(kind: AnalysisKind): MutableStateFlow<List<AnalysisRun>> =
        allFlows.getOrPut(kind) { MutableStateFlow(emptyList()) }
}
