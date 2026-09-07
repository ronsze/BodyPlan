package kr.sdbk.bodyplan.feature.my.impl.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository

/**
 * 인바디는 같은 열쇠(빈 문자열)로 여러 결과를 쌓는다. 맵 하나로는 마지막 것만 남아
 * 다른 feature의 페이크처럼 `Map<Pair<Kind, String>, AnalysisResult>`를 쓰지 못하고 목록으로 쌓는다.
 */
internal class FakeAnalysisResultRepository(initialHistory: List<AnalysisResult> = emptyList()) :
    AnalysisResultRepository {
    private val history = MutableStateFlow(initialHistory)

    private var nextId: Long = (initialHistory.maxOfOrNull { it.id } ?: 0L) + 1

    var saveCount: Int = 0
        private set
    var lastSaved: AnalysisResult? = null
        private set

    override fun observeLatest(kind: AnalysisKind, scopeKey: String): Flow<AnalysisResult?> = error("사용하지 않음")

    override suspend fun getLatestOf(kind: AnalysisKind, scopeKeys: List<String>): Map<String, AnalysisResult> =
        error("사용하지 않음")

    override fun observeHistory(kind: AnalysisKind): Flow<List<AnalysisResult>> =
        history.map { all -> all.filter { it.kind == kind }.sortedByDescending { it.id } }

    override suspend fun save(
        kind: AnalysisKind,
        scopeKey: String,
        content: AnalysisContent,
        imageFileName: String?,
        measurement: InbodyMeasurement?,
    ) {
        saveCount++
        val result = AnalysisResult(
            id = nextId++,
            kind = kind,
            scopeKey = scopeKey,
            content = content,
            createdAtMillis = 0L,
            imagePath = imageFileName,
            measurement = measurement,
        )
        lastSaved = result
        history.value = history.value + result
    }
}
