package kr.sdbk.bodyplan.feature.workoutlog.impl.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository

internal class FakeAnalysisResultRepository(initial: Map<Pair<AnalysisKind, String>, AnalysisResult> = emptyMap()) :
    AnalysisResultRepository {
    private val results = MutableStateFlow(initial)

    var nextId: Long = 1L
    var saveCount: Int = 0
        private set
    var lastSavedContent: AnalysisContent? = null
        private set

    override fun observeLatest(kind: AnalysisKind, scopeKey: String): Flow<AnalysisResult?> =
        results.map { it[kind to scopeKey] }

    override suspend fun getLatestOf(kind: AnalysisKind, scopeKeys: List<String>): Map<String, AnalysisResult> =
        results.value
            .filterKeys { (resultKind, scopeKey) -> resultKind == kind && scopeKey in scopeKeys }
            .mapKeys { (key, _) -> key.second }

    override fun observeHistory(kind: AnalysisKind): Flow<List<AnalysisResult>> =
        results.map { all -> all.filterKeys { it.first == kind }.values.sortedByDescending { it.id } }

    override suspend fun save(kind: AnalysisKind, scopeKey: String, content: AnalysisContent, imageFileName: String?) {
        saveCount++
        lastSavedContent = content
        val result = AnalysisResult(
            id = nextId++,
            kind = kind,
            scopeKey = scopeKey,
            content = content,
            createdAtMillis = 0L,
        )
        results.value = results.value + ((kind to scopeKey) to result)
    }
}
