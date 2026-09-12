package kr.sdbk.bodyplan.core.data.repository.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kr.sdbk.bodyplan.core.local.dao.WorkoutEntryDao
import kr.sdbk.bodyplan.core.local.entity.DateBodyPart
import kr.sdbk.bodyplan.core.local.entity.WorkoutEntryEntity
import kr.sdbk.bodyplan.core.local.entity.WorkoutEntryWithSets
import kr.sdbk.bodyplan.core.local.entity.WorkoutSetEntity

/**
 * [WorkoutEntryDao]의 인메모리 페이크.
 *
 * `replaceSets`·`insertWithSets`·`updateWithSets`는 인터페이스의 기본 구현을 그대로 쓴다.
 * 실제 Room과 달리 외래 키 CASCADE는 흉내 내지 않는다 — 이 과제의 수용 조건 범위 밖이다.
 */
internal class FakeWorkoutEntryDao : WorkoutEntryDao {
    private val entries = MutableStateFlow<Map<Long, WorkoutEntryEntity>>(emptyMap())
    private val sets = MutableStateFlow<Map<Long, WorkoutSetEntity>>(emptyMap())
    private var nextEntryId = 1L
    private var nextSetId = 1L

    var insertFailure: Throwable? = null
    var updateFailure: Throwable? = null
    var deleteFailure: Throwable? = null

    fun setsFor(entryId: Long): List<WorkoutSetEntity> =
        sets.value.values.filter { it.entryId == entryId }.sortedBy { it.setNumber }

    override fun observeByDate(epochDay: Long): Flow<List<WorkoutEntryWithSets>> = combine(entries, sets) { e, s ->
        e.values
            .filter { it.dateEpochDay == epochDay }
            .sortedWith(compareBy({ it.createdAtMillis }, { it.id }))
            .map { entry -> WorkoutEntryWithSets(entry, s.values.filter { it.entryId == entry.id }) }
    }

    override fun observeInRange(from: Long, to: Long): Flow<List<DateBodyPart>> = entries.combine(sets) { e, _ ->
        e.values
            .filter { it.dateEpochDay in from..to }
            .map { DateBodyPart(it.dateEpochDay, it.bodyPart) }
            .distinct()
    }

    override fun observeWithSetsInRange(from: Long, to: Long): Flow<List<WorkoutEntryWithSets>> =
        combine(entries, sets) { e, s ->
            e.values
                .filter { it.dateEpochDay in from..to }
                .sortedWith(compareBy({ it.dateEpochDay }, { it.createdAtMillis }, { it.id }))
                .map { entry -> WorkoutEntryWithSets(entry, s.values.filter { it.entryId == entry.id }) }
        }

    override suspend fun getWithSets(id: Long): WorkoutEntryWithSets? {
        val entry = entries.value[id] ?: return null
        return WorkoutEntryWithSets(entry, setsFor(id))
    }

    override suspend fun insert(entity: WorkoutEntryEntity): Long {
        insertFailure?.let { throw it }
        val id = if (entity.id != 0L) entity.id else nextEntryId++
        entries.value = entries.value + (id to entity.copy(id = id))
        return id
    }

    override suspend fun update(entity: WorkoutEntryEntity) {
        updateFailure?.let { throw it }
        entries.value = entries.value + (entity.id to entity)
    }

    override suspend fun deleteById(id: Long) {
        deleteFailure?.let { throw it }
        entries.value = entries.value - id
    }

    override suspend fun insertSets(sets: List<WorkoutSetEntity>) {
        val withIds = sets.associateBy { nextSetId++ }
        this.sets.value = this.sets.value + withIds
    }

    override suspend fun deleteSetsByEntryId(entryId: Long) {
        sets.value = sets.value.filterValues { it.entryId != entryId }
    }

    // 백업 스냅샷용. 이 페이크의 대상 테스트는 스냅샷을 쓰지 않아 최소 동작만 둔다.
    override suspend fun getAll(): List<WorkoutEntryEntity> = entries.value.values.sortedBy { it.id }

    override suspend fun getAllSets(): List<WorkoutSetEntity> = sets.value.values.sortedBy { it.id }

    override suspend fun insertAll(entities: List<WorkoutEntryEntity>) {
        entities.forEach { insert(it) }
    }

    override suspend fun deleteAll() {
        entries.value = emptyMap()
        sets.value = emptyMap()
    }
}
