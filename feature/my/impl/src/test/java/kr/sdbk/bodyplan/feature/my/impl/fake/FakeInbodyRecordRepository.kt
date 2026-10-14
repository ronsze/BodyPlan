package kr.sdbk.bodyplan.feature.my.impl.fake

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kr.sdbk.bodyplan.core.domain.model.InbodyRecord
import kr.sdbk.bodyplan.core.domain.model.InbodyRecordWithResult
import kr.sdbk.bodyplan.core.domain.repository.InbodyRecordRepository

internal class FakeInbodyRecordRepository(initial: List<InbodyRecordWithResult> = emptyList()) :
    InbodyRecordRepository {
    val items = MutableStateFlow(initial)
    private var nextId: Long = (initial.maxOfOrNull { it.record.id } ?: 0L) + 1

    /** `insert` 호출 (사진 파일명, 측정일). */
    val insertCalls: MutableList<Pair<String, LocalDate>> = mutableListOf()
    var insertFailure: Throwable? = null

    override fun observeRecords(): Flow<List<InbodyRecordWithResult>> = items

    override suspend fun getRecords(): List<InbodyRecordWithResult> = items.value

    override suspend fun getRecord(id: Long): InbodyRecord? = items.value.firstOrNull { it.record.id == id }?.record

    override suspend fun insert(imageFileName: String, measuredOn: LocalDate): Long {
        insertCalls += imageFileName to measuredOn
        insertFailure?.let { throw it }
        val id = nextId++
        items.value = items.value +
            InbodyRecordWithResult(InbodyRecord(id, imageFileName, measuredOn, createdAtMillis = 0L), null)
        return id
    }

    override suspend fun setFailureReason(id: Long, reason: String?) {
        items.value = items.value.map {
            if (it.record.id == id) it.copy(record = it.record.copy(failureReason = reason)) else it
        }
    }
}
