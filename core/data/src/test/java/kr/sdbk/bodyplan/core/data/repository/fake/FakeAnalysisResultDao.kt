package kr.sdbk.bodyplan.core.data.repository.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.local.dao.AnalysisResultDao
import kr.sdbk.bodyplan.core.local.entity.AnalysisResultEntity

/** [AnalysisResultDao]의 인메모리 페이크. replace는 인터페이스 기본 본문(deleteByScope + insert)을 쓴다. */
internal class FakeAnalysisResultDao : AnalysisResultDao {
    private val state = MutableStateFlow<List<AnalysisResultEntity>>(emptyList())
    private var nextId = 1L

    val stored: List<AnalysisResultEntity> get() = state.value

    fun seed(entity: AnalysisResultEntity): Long = insertNow(entity)

    private fun insertNow(entity: AnalysisResultEntity): Long {
        val id = if (entity.id != 0L) entity.id else nextId++
        if (id >= nextId) nextId = id + 1
        state.value = state.value + entity.copy(id = id)
        return id
    }

    private fun latestFirst(rows: List<AnalysisResultEntity>) =
        rows.sortedWith(compareByDescending<AnalysisResultEntity> { it.createdAtMillis }.thenByDescending { it.id })

    override fun observeLatest(kind: String, scopeKey: String): Flow<AnalysisResultEntity?> = state.map { rows ->
        latestFirst(rows.filter { it.kind == kind && it.scopeKey == scopeKey }).firstOrNull()
    }

    override suspend fun getByScopeKeys(kind: String, scopeKeys: List<String>): List<AnalysisResultEntity> =
        latestFirst(state.value.filter { it.kind == kind && it.scopeKey in scopeKeys })

    override fun observeByKind(kind: String): Flow<List<AnalysisResultEntity>> =
        state.map { rows -> latestFirst(rows.filter { it.kind == kind }) }

    override suspend fun insert(entity: AnalysisResultEntity): Long = insertNow(entity)

    override suspend fun deleteByScope(kind: String, scopeKey: String) {
        state.value = state.value.filterNot { it.kind == kind && it.scopeKey == scopeKey }
    }

    override suspend fun getAll(): List<AnalysisResultEntity> = state.value

    override suspend fun insertAll(entities: List<AnalysisResultEntity>) {
        entities.forEach { insertNow(it) }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }
}
