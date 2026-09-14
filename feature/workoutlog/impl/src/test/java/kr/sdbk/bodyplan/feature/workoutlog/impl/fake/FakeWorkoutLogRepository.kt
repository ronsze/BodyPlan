package kr.sdbk.bodyplan.feature.workoutlog.impl.fake

import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.ExerciseBest
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutLog
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

internal class FakeWorkoutLogRepository(
    initial: List<WorkoutEntry> = emptyList(),
    private val bodyPartsByDate: Map<LocalDate, Set<BodyPart>> = emptyMap(),
    private val entriesByDate: Map<LocalDate, List<WorkoutEntry>> = emptyMap(),
    private val bestBeforeByDate: Map<LocalDate, List<ExerciseBest>> = emptyMap(),
    private val latestEntriesByExercise: Map<Long, WorkoutEntry> = emptyMap(),
) : WorkoutLogRepository {
    private val entries = MutableStateFlow(initial)
    private val memo = MutableStateFlow<String?>(null)

    var observeFailure: Throwable? = null
    var mutateFailure: Throwable? = null
    var getLatestEntryFailure: Throwable? = null

    var getLatestEntryCallCount: Int = 0
        private set

    var addedCount: Int = 0
        private set
    var lastUpdatedEntryId: Long? = null
        private set
    var lastSavedSets: List<WorkoutSet>? = null
        private set
    var lastSavedExercise: Exercise? = null
        private set
    var lastSavedMemo: String? = null
        private set
    var lastAddedEntries: List<WorkoutEntry>? = null
        private set
    var saveMemoCallCount: Int = 0
        private set

    // 종목을 바꿀 때마다 새로 구독하지 않는지 보려고 둔다 — ExerciseTrendViewModel이 확인한다.
    var observeEntriesInRangeCallCount: Int = 0
        private set

    // 캘린더를 접었을 때 이미 이번 달이면 다시 구독하지 않는지 보려고 둔다.
    var observeBodyPartsInRangeCallCount: Int = 0
        private set

    // 저장이 끝나지 않은 채로 두 번째 인텐트가 오는 상황을 만들려면 이 걸쇠로 완료 시점을 붙잡아 둔다.
    var saveMemoGate: CompletableDeferred<Unit>? = null

    override fun observeLog(date: LocalDate): Flow<WorkoutLog> = combine(entries, memo) { list, text ->
        observeFailure?.let { throw it }
        WorkoutLog(date = date, entries = list, memo = text)
    }

    override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
        entries.map {
            observeFailure?.let { failure -> throw failure }
            bodyPartsByDate.filterKeys { date -> !date.isBefore(from) && !date.isAfter(to) }
        }.onStart { observeBodyPartsInRangeCallCount++ }

    override fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>> =
        entries.map {
            observeFailure?.let { failure -> throw failure }
            entriesByDate.filterKeys { date -> !date.isBefore(from) && !date.isAfter(to) }
        }.onStart { observeEntriesInRangeCallCount++ }

    override suspend fun getEntry(id: Long): WorkoutEntry? = entries.value.firstOrNull { it.id == id }

    override fun observeBestBefore(date: LocalDate): Flow<List<ExerciseBest>> = entries.map {
        observeFailure?.let { failure -> throw failure }
        bestBeforeByDate[date].orEmpty()
    }

    override suspend fun getLatestEntry(exerciseId: Long, until: LocalDate): WorkoutEntry? {
        getLatestEntryCallCount++
        getLatestEntryFailure?.let { throw it }
        return latestEntriesByExercise[exerciseId]
    }

    override suspend fun addEntry(date: LocalDate, exercise: Exercise, sets: List<WorkoutSet>): Long {
        mutateFailure?.let { throw it }
        addedCount++
        lastSavedExercise = exercise
        lastSavedSets = sets
        return 100L
    }

    override suspend fun addEntries(date: LocalDate, entries: List<WorkoutEntry>) {
        mutateFailure?.let { throw it }
        lastAddedEntries = entries
        // 실제 구현과 같이 id를 새로 매겨 뒤에 덧붙인다.
        val nextId = (this.entries.value.maxOfOrNull { it.id } ?: 0L) + 1
        this.entries.value = this.entries.value + entries.mapIndexed { index, entry -> entry.copy(id = nextId + index) }
    }

    override suspend fun updateEntry(entryId: Long, exercise: Exercise, sets: List<WorkoutSet>) {
        mutateFailure?.let { throw it }
        lastUpdatedEntryId = entryId
        lastSavedExercise = exercise
        lastSavedSets = sets
    }

    override suspend fun deleteEntry(id: Long) {
        mutateFailure?.let { throw it }
        entries.value = entries.value.filterNot { it.id == id }
    }

    override suspend fun saveMemo(date: LocalDate, text: String) {
        mutateFailure?.let { throw it }
        saveMemoCallCount++
        saveMemoGate?.await()
        lastSavedMemo = text
        // 실제 구현과 같이 공백뿐인 메모는 남기지 않는다.
        memo.value = text.trim().ifEmpty { null }
    }
}
