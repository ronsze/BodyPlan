package kr.sdbk.bodyplan.core.data.repository.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.local.dao.DietEntryDao
import kr.sdbk.bodyplan.core.local.entity.DateImage
import kr.sdbk.bodyplan.core.local.entity.DietEntryEntity

/** [DietEntryDao]의 인메모리 페이크. Room 없이 Repository만 검증하기 위한 것이다. */
internal class FakeDietEntryDao : DietEntryDao {
    private val state = MutableStateFlow<Map<Long, DietEntryEntity>>(emptyMap())
    private var nextId = 1L

    var insertFailure: Throwable? = null
    var updateFailure: Throwable? = null

    val stored: List<DietEntryEntity> get() = state.value.values.sortedBy { it.id }

    fun seed(entity: DietEntryEntity): Long {
        val id = if (entity.id != 0L) entity.id else nextId++
        state.value = state.value + (id to entity.copy(id = id))
        return id
    }

    override fun observeByDate(epochDay: Long): Flow<List<DietEntryEntity>> = state.map { entities ->
        entities.values
            .filter { it.dateEpochDay == epochDay }
            .sortedWith(compareBy({ it.createdAtMillis }, { it.id }))
    }

    override fun observeFirstImageInRange(from: Long, to: Long): Flow<List<DateImage>> = state.map { entities ->
        entities.values
            .filter { it.dateEpochDay in from..to }
            .groupBy { it.dateEpochDay }
            .mapNotNull { (date, rows) ->
                rows.minWithOrNull(compareBy({ it.createdAtMillis }, { it.id }))
                    ?.let { DateImage(date, it.imageFileName) }
            }
    }

    override suspend fun getById(id: Long): DietEntryEntity? = state.value[id]

    override suspend fun insert(entity: DietEntryEntity): Long {
        insertFailure?.let { throw it }
        return seed(entity)
    }

    override suspend fun update(entity: DietEntryEntity) {
        updateFailure?.let { throw it }
        state.value = state.value + (entity.id to entity)
    }

    override suspend fun deleteById(id: Long) {
        state.value = state.value - id
    }

    // 백업 스냅샷용. 이 페이크의 대상 테스트는 스냅샷을 쓰지 않아 최소 동작만 둔다.
    override suspend fun getAll(): List<DietEntryEntity> = stored

    override suspend fun insertAll(entities: List<DietEntryEntity>) {
        entities.forEach { seed(it) }
    }

    override suspend fun deleteAll() {
        state.value = emptyMap()
    }
}
