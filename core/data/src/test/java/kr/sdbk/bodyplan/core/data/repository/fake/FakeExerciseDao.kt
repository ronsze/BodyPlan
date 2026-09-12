package kr.sdbk.bodyplan.core.data.repository.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.local.dao.ExerciseDao
import kr.sdbk.bodyplan.core.local.entity.ExerciseEntity

/** [ExerciseDao]의 인메모리 페이크. Room 인스트루먼트 테스트 없이 Repository만 검증하기 위한 것이다. */
internal class FakeExerciseDao : ExerciseDao {
    private val state = MutableStateFlow<Map<Long, ExerciseEntity>>(emptyMap())
    private var nextId = 1L

    var insertFailure: Throwable? = null
    var updateFailure: Throwable? = null
    var markDeletedFailure: Throwable? = null

    fun seed(entity: ExerciseEntity): Long {
        val id = if (entity.id != 0L) entity.id else nextId++
        val stored = entity.copy(id = id)
        state.value = state.value + (id to stored)
        return id
    }

    override fun observeByBodyPart(bodyPart: String): Flow<List<ExerciseEntity>> = state.map { entities ->
        entities.values.filter { it.bodyPart == bodyPart && !it.isDeleted }.sortedBy { it.id }
    }

    override suspend fun getById(id: Long): ExerciseEntity? = state.value[id]

    override suspend fun insert(entity: ExerciseEntity): Long {
        insertFailure?.let { throw it }
        return seed(entity)
    }

    override suspend fun updateFields(id: Long, bodyPart: String, name: String, intensityType: String) {
        updateFailure?.let { throw it }
        val existing = state.value[id] ?: return
        // 실제 쿼리와 같이 isDeleted는 건드리지 않는다.
        state.value = state.value +
            (id to existing.copy(bodyPart = bodyPart, name = name, intensityType = intensityType))
    }

    override suspend fun markDeleted(id: Long) {
        markDeletedFailure?.let { throw it }
        val existing = state.value[id] ?: return
        state.value = state.value + (id to existing.copy(isDeleted = true))
    }

    // 백업 스냅샷용. 이 페이크의 대상 테스트는 스냅샷을 쓰지 않아 최소 동작만 둔다.
    override suspend fun getAll(): List<ExerciseEntity> = state.value.values.sortedBy { it.id }

    override suspend fun insertAll(entities: List<ExerciseEntity>) {
        entities.forEach { seed(it) }
    }

    override suspend fun deleteAll() {
        state.value = emptyMap()
    }
}
