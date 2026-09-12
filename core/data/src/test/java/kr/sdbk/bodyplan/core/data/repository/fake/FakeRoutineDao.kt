package kr.sdbk.bodyplan.core.data.repository.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kr.sdbk.bodyplan.core.local.dao.RoutineDao
import kr.sdbk.bodyplan.core.local.entity.RoutineEntity
import kr.sdbk.bodyplan.core.local.entity.RoutineEntryEntity
import kr.sdbk.bodyplan.core.local.entity.RoutineEntryWithSets
import kr.sdbk.bodyplan.core.local.entity.RoutineSetEntity
import kr.sdbk.bodyplan.core.local.entity.RoutineWithEntries

/**
 * [RoutineDao]의 인메모리 페이크.
 *
 * `insertEntryWithSets`·`updateEntryWithSets`는 인터페이스의 기본 구현을 그대로 쓴다.
 * 실제 Room과 달리 외래 키 CASCADE는 흉내 내지 않는다.
 */
internal class FakeRoutineDao : RoutineDao {
    private val routines = MutableStateFlow<Map<Long, RoutineEntity>>(emptyMap())
    private val entries = MutableStateFlow<Map<Long, RoutineEntryEntity>>(emptyMap())
    private val sets = MutableStateFlow<Map<Long, RoutineSetEntity>>(emptyMap())
    private var nextRoutineId = 1L
    private var nextEntryId = 1L
    private var nextSetId = 1L

    var insertFailure: Throwable? = null
    var deleteFailure: Throwable? = null

    fun setsFor(entryId: Long): List<RoutineSetEntity> =
        sets.value.values.filter { it.entryId == entryId }.sortedBy { it.setNumber }

    private fun withEntries(
        routine: RoutineEntity,
        entries: Map<Long, RoutineEntryEntity>,
        sets: Map<Long, RoutineSetEntity>,
    ): RoutineWithEntries = RoutineWithEntries(
        routine = routine,
        // Room의 @Relation처럼 순서를 보장하지 않는 것을 흉내 내려고 뒤집어 준다.
        entries = entries.values
            .filter { it.routineId == routine.id }
            .reversed()
            .map { entry -> RoutineEntryWithSets(entry, sets.values.filter { it.entryId == entry.id }) },
    )

    override fun observeAll(): Flow<List<RoutineWithEntries>> = combine(routines, entries, sets) { r, e, s ->
        r.values
            .sortedWith(compareBy({ it.createdAtMillis }, { it.id }))
            .map { withEntries(it, e, s) }
    }

    override fun observeById(id: Long): Flow<RoutineWithEntries?> = combine(routines, entries, sets) { r, e, s ->
        r[id]?.let { withEntries(it, e, s) }
    }

    override suspend fun getById(id: Long): RoutineWithEntries? =
        routines.value[id]?.let { withEntries(it, entries.value, sets.value) }

    override suspend fun insert(entity: RoutineEntity): Long {
        insertFailure?.let { throw it }
        val id = if (entity.id != 0L) entity.id else nextRoutineId++
        routines.value = routines.value + (id to entity.copy(id = id))
        return id
    }

    override suspend fun deleteById(id: Long) {
        deleteFailure?.let { throw it }
        routines.value = routines.value - id
    }

    override suspend fun getEntryWithSets(id: Long): RoutineEntryWithSets? {
        val entry = entries.value[id] ?: return null
        return RoutineEntryWithSets(entry, setsFor(id))
    }

    override suspend fun insertEntry(entity: RoutineEntryEntity): Long {
        insertFailure?.let { throw it }
        val id = if (entity.id != 0L) entity.id else nextEntryId++
        entries.value = entries.value + (id to entity.copy(id = id))
        return id
    }

    override suspend fun updateEntry(entity: RoutineEntryEntity) {
        entries.value = entries.value + (entity.id to entity)
    }

    override suspend fun deleteEntryById(id: Long) {
        deleteFailure?.let { throw it }
        entries.value = entries.value - id
    }

    override suspend fun insertSets(sets: List<RoutineSetEntity>) {
        val withIds = sets.associateBy { nextSetId++ }
        this.sets.value = this.sets.value + withIds
    }

    override suspend fun deleteSetsByEntryId(entryId: Long) {
        sets.value = sets.value.filterValues { it.entryId != entryId }
    }
}
