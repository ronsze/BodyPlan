package kr.sdbk.bodyplan.feature.my.impl.fake

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.core.domain.repository.WeightLogRepository

internal class FakeWeightLogRepository(initialRecords: List<WeightRecord> = emptyList()) : WeightLogRepository {
    private val records = MutableStateFlow(initialRecords)

    var loadFailure: Throwable? = null

    var saveFailure: Throwable? = null

    var saveCount: Int = 0
        private set

    override fun observeRecordsInRange(from: LocalDate, to: LocalDate): Flow<List<WeightRecord>> = flow {
        loadFailure?.let { throw it }
        records.collect { list ->
            emit(list.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }.sortedBy { it.date })
        }
    }

    override suspend fun save(date: LocalDate, weightKg: Double) {
        saveFailure?.let { throw it }
        saveCount++
        records.update { current -> current.filterNot { it.date == date } + WeightRecord(date, weightKg) }
    }

    fun currentRecords(): List<WeightRecord> = records.value
}
