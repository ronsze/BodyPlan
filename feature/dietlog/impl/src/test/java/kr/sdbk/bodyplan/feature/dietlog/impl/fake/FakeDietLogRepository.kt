package kr.sdbk.bodyplan.feature.dietlog.impl.fake

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.DietEntry
import kr.sdbk.bodyplan.core.domain.model.DietLog
import kr.sdbk.bodyplan.core.domain.repository.DietLogRepository

internal class FakeDietLogRepository(
    initial: List<DietEntry> = emptyList(),
    private val imagesByDate: Map<LocalDate, String> = emptyMap(),
) : DietLogRepository {
    private val entries = MutableStateFlow(initial)

    var observeFailure: Throwable? = null
    var mutateFailure: Throwable? = null

    var addedCount: Int = 0
        private set
    var lastAddedUri: String? = null
        private set
    var lastUpdatedEntryId: Long? = null
        private set
    var lastUpdatedUri: String? = null
        private set
    var lastSavedMemo: String? = null
        private set

    override fun observeLog(date: LocalDate): Flow<DietLog> = entries.map { list ->
        observeFailure?.let { throw it }
        DietLog(date = date, entries = list)
    }

    override fun observeFirstImageInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, String>> = entries.map {
        observeFailure?.let { failure -> throw failure }
        imagesByDate.filterKeys { date -> !date.isBefore(from) && !date.isAfter(to) }
    }

    override suspend fun getEntry(id: Long): DietEntry? = entries.value.firstOrNull { it.id == id }

    override suspend fun addEntry(date: LocalDate, sourceImageUri: String, memo: String?): Long {
        mutateFailure?.let { throw it }
        addedCount++
        lastAddedUri = sourceImageUri
        lastSavedMemo = memo
        return 100L
    }

    override suspend fun updateEntry(entryId: Long, sourceImageUri: String?, memo: String?) {
        mutateFailure?.let { throw it }
        lastUpdatedEntryId = entryId
        lastUpdatedUri = sourceImageUri
        lastSavedMemo = memo
    }

    override suspend fun deleteEntry(id: Long) {
        mutateFailure?.let { throw it }
        entries.value = entries.value.filterNot { it.id == id }
    }
}
