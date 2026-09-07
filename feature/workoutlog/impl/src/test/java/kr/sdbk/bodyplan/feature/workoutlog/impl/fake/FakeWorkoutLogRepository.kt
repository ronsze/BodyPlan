package kr.sdbk.bodyplan.feature.workoutlog.impl.fake

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutLog
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

internal class FakeWorkoutLogRepository(
    initial: List<WorkoutEntry> = emptyList(),
    private val bodyPartsByDate: Map<LocalDate, Set<BodyPart>> = emptyMap(),
) : WorkoutLogRepository {
    private val entries = MutableStateFlow(initial)

    var observeFailure: Throwable? = null
    var mutateFailure: Throwable? = null

    var addedCount: Int = 0
        private set
    var lastUpdatedEntryId: Long? = null
        private set
    var lastSavedSets: List<WorkoutSet>? = null
        private set
    var lastSavedExercise: Exercise? = null
        private set

    override fun observeLog(date: LocalDate): Flow<WorkoutLog> = entries.map { list ->
        observeFailure?.let { throw it }
        WorkoutLog(date = date, entries = list)
    }

    override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
        entries.map {
            observeFailure?.let { failure -> throw failure }
            bodyPartsByDate.filterKeys { date -> !date.isBefore(from) && !date.isAfter(to) }
        }

    override suspend fun getEntry(id: Long): WorkoutEntry? = entries.value.firstOrNull { it.id == id }

    override suspend fun addEntry(date: LocalDate, exercise: Exercise, sets: List<WorkoutSet>): Long {
        mutateFailure?.let { throw it }
        addedCount++
        lastSavedExercise = exercise
        lastSavedSets = sets
        return 100L
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
}
