package kr.sdbk.bodyplan.core.data.repository.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.local.dao.WeightRecordDao
import kr.sdbk.bodyplan.core.local.entity.WeightRecordEntity

/** [WeightRecordDao]의 인메모리 페이크. Room 없이 Repository만 검증하기 위한 것이다. */
internal class FakeWeightRecordDao : WeightRecordDao {
    private val state = MutableStateFlow<Map<Long, WeightRecordEntity>>(emptyMap())

    var upsertFailure: Throwable? = null

    val stored: List<WeightRecordEntity> get() = state.value.values.sortedBy { it.dateEpochDay }

    fun seed(entity: WeightRecordEntity) {
        state.value = state.value + (entity.dateEpochDay to entity)
    }

    override fun observeInRange(from: Long, to: Long): Flow<List<WeightRecordEntity>> = state.map { entities ->
        entities.values.filter { it.dateEpochDay in from..to }.sortedBy { it.dateEpochDay }
    }

    override suspend fun upsert(entity: WeightRecordEntity) {
        upsertFailure?.let { throw it }
        // 날짜가 기본 키라 같은 날짜는 덮인다. Room의 REPLACE와 같은 결과다.
        state.value = state.value + (entity.dateEpochDay to entity)
    }
}
