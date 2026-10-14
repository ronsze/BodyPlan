package kr.sdbk.bodyplan.core.data.repository.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.local.dao.InbodyRecordDao
import kr.sdbk.bodyplan.core.local.entity.InbodyRecordEntity

/** [InbodyRecordDao]의 인메모리 페이크. */
internal class FakeInbodyRecordDao : InbodyRecordDao {
    private val state = MutableStateFlow<Map<Long, InbodyRecordEntity>>(emptyMap())
    private var nextId = 1L

    val stored: List<InbodyRecordEntity> get() = state.value.values.sortedBy { it.id }

    fun seed(entity: InbodyRecordEntity): Long {
        val id = if (entity.id != 0L) entity.id else nextId++
        if (id >= nextId) nextId = id + 1
        state.value = state.value + (id to entity.copy(id = id))
        return id
    }

    private fun ordered(map: Map<Long, InbodyRecordEntity>) = map.values.sortedWith(
        compareByDescending<InbodyRecordEntity> {
            it.measuredOnEpochDay
        }.thenByDescending { it.id },
    )

    override fun observeAll(): Flow<List<InbodyRecordEntity>> = state.map { ordered(it) }

    override fun observe(id: Long): Flow<InbodyRecordEntity?> = state.map { it[id] }

    override suspend fun getById(id: Long): InbodyRecordEntity? = state.value[id]

    override suspend fun insert(entity: InbodyRecordEntity): Long = seed(entity)

    override suspend fun updateFailureReason(id: Long, reason: String?) {
        val current = state.value[id] ?: return
        state.value = state.value + (id to current.copy(failureReason = reason))
    }

    override suspend fun getAll(): List<InbodyRecordEntity> = ordered(state.value)

    override suspend fun insertAll(entities: List<InbodyRecordEntity>) {
        entities.forEach { seed(it) }
    }

    override suspend fun deleteAll() {
        state.value = emptyMap()
    }
}
