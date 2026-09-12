package kr.sdbk.bodyplan.core.data.repository.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.local.dao.WorkoutMemoDao
import kr.sdbk.bodyplan.core.local.entity.WorkoutMemoEntity

/** [WorkoutMemoDao]의 인메모리 페이크. Room 없이 Repository만 검증하기 위한 것이다. */
internal class FakeWorkoutMemoDao : WorkoutMemoDao {
    private val state = MutableStateFlow<Map<Long, WorkoutMemoEntity>>(emptyMap())

    var upsertFailure: Throwable? = null

    val stored: List<WorkoutMemoEntity> get() = state.value.values.sortedBy { it.dateEpochDay }

    fun seed(entity: WorkoutMemoEntity) {
        state.value = state.value + (entity.dateEpochDay to entity)
    }

    override fun observeByDate(dateEpochDay: Long): Flow<WorkoutMemoEntity?> = state.map { it[dateEpochDay] }

    override suspend fun upsert(entity: WorkoutMemoEntity) {
        upsertFailure?.let { throw it }
        // 날짜가 기본 키라 같은 날짜는 덮인다. Room의 REPLACE와 같은 결과다.
        state.value = state.value + (entity.dateEpochDay to entity)
    }

    override suspend fun deleteByDate(dateEpochDay: Long) {
        state.value = state.value - dateEpochDay
    }

    // 백업 스냅샷용. 이 페이크의 대상 테스트는 스냅샷을 쓰지 않아 최소 동작만 둔다.
    override suspend fun getAll(): List<WorkoutMemoEntity> = stored

    override suspend fun insertAll(entities: List<WorkoutMemoEntity>) {
        entities.forEach { seed(it) }
    }

    override suspend fun deleteAll() {
        state.value = emptyMap()
    }
}
