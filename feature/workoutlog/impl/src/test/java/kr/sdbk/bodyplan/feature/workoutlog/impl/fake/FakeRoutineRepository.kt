package kr.sdbk.bodyplan.feature.workoutlog.impl.fake

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.RoutineRepository

internal class FakeRoutineRepository(initial: List<Routine> = emptyList()) : RoutineRepository {
    private val routines = MutableStateFlow(initial)
    private var nextRoutineId = (initial.maxOfOrNull { it.id } ?: 0L) + 1
    private var nextEntryId = (initial.flatMap { it.entries }.maxOfOrNull { it.id } ?: 0L) + 1

    var observeFailure: Throwable? = null
    var mutateFailure: Throwable? = null

    // 루틴을 적용하는 도중에 다른 인텐트가 들어오는 상황을 만들려면 이 걸쇠로 완료 시점을 붙잡아 둔다.
    var getRoutineGate: CompletableDeferred<Unit>? = null

    var lastAddedRoutineName: String? = null
        private set
    var lastAddedRoutineBodyPart: BodyPart? = null
        private set
    var addedEntryCount: Int = 0
        private set
    var lastUpdatedEntryId: Long? = null
        private set
    var lastSavedExercise: Exercise? = null
        private set
    var lastSavedSets: List<WorkoutSet>? = null
        private set

    override fun observeRoutines(): Flow<List<Routine>> = routines.map {
        observeFailure?.let { failure -> throw failure }
        it
    }

    override fun observeRoutine(id: Long): Flow<Routine?> = routines.map { list ->
        observeFailure?.let { failure -> throw failure }
        list.firstOrNull { it.id == id }
    }

    override suspend fun getRoutine(id: Long): Routine? {
        getRoutineGate?.await()
        return routines.value.firstOrNull { it.id == id }
    }

    override suspend fun addRoutine(name: String, bodyPart: BodyPart): Long {
        mutateFailure?.let { throw it }
        lastAddedRoutineName = name
        lastAddedRoutineBodyPart = bodyPart
        val id = nextRoutineId++
        routines.value = routines.value + Routine(id = id, name = name, bodyPart = bodyPart, entries = emptyList())
        return id
    }

    override suspend fun deleteRoutine(id: Long) {
        mutateFailure?.let { throw it }
        routines.value = routines.value.filterNot { it.id == id }
    }

    override suspend fun getEntry(id: Long): WorkoutEntry? =
        routines.value.flatMap { it.entries }.firstOrNull { it.id == id }

    override suspend fun addEntry(routineId: Long, exercise: Exercise, sets: List<WorkoutSet>): Long {
        mutateFailure?.let { throw it }
        addedEntryCount++
        lastSavedExercise = exercise
        lastSavedSets = sets
        val id = nextEntryId++
        val entry = WorkoutEntry(
            id = id,
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            bodyPart = exercise.bodyPart,
            intensityType = exercise.intensityType,
            sets = sets,
        )
        routines.value = routines.value.map {
            if (it.id == routineId) it.copy(entries = it.entries + entry) else it
        }
        return id
    }

    override suspend fun updateEntry(entryId: Long, exercise: Exercise, sets: List<WorkoutSet>) {
        mutateFailure?.let { throw it }
        lastUpdatedEntryId = entryId
        lastSavedExercise = exercise
        lastSavedSets = sets
        routines.value = routines.value.map { routine ->
            routine.copy(
                entries = routine.entries.map { entry ->
                    if (entry.id == entryId) {
                        entry.copy(
                            exerciseId = exercise.id,
                            exerciseName = exercise.name,
                            bodyPart = exercise.bodyPart,
                            intensityType = exercise.intensityType,
                            sets = sets,
                        )
                    } else {
                        entry
                    }
                },
            )
        }
    }

    override suspend fun deleteEntry(id: Long) {
        mutateFailure?.let { throw it }
        routines.value = routines.value.map { it.copy(entries = it.entries.filterNot { entry -> entry.id == id }) }
    }
}
