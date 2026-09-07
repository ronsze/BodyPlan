package kr.sdbk.bodyplan.feature.my.impl.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunRequest
import kr.sdbk.bodyplan.core.domain.repository.AnalysisRunner

/**
 * `start` 호출을 기록하고, `observe`가 내는 값을 테스트가 [flowFor]로 밀어 넣을 수 있다.
 *
 * 화면을 나갔다 들어와도(ViewModel을 새로 만들어도) 흐름이 이어지는 것을 검증하려면,
 * ViewModel을 만들기 전에 [flowFor]로 값을 미리 넣어 둘 수 있어야 한다.
 */
internal class FakeAnalysisRunner : AnalysisRunner {
    val startCalls: MutableList<AnalysisRunRequest> = mutableListOf()
    private val flows = mutableMapOf<Pair<AnalysisKind, String>, MutableStateFlow<AnalysisRun?>>()

    override suspend fun start(request: AnalysisRunRequest) {
        startCalls += request
    }

    override fun observe(kind: AnalysisKind, scopeKey: String): Flow<AnalysisRun?> = flowFor(kind, scopeKey)

    fun flowFor(kind: AnalysisKind, scopeKey: String): MutableStateFlow<AnalysisRun?> =
        flows.getOrPut(kind to scopeKey) { MutableStateFlow(null) }
}
