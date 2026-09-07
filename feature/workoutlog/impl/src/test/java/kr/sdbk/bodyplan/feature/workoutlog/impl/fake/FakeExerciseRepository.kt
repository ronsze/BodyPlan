package kr.sdbk.bodyplan.feature.workoutlog.impl.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.repository.ExerciseRepository

internal class FakeExerciseRepository(initial: List<Exercise> = emptyList()) : ExerciseRepository {
    private val exercises = MutableStateFlow(initial)

    var observeFailure: Throwable? = null
    var mutateFailure: Throwable? = null

    private var nextId: Long = 1000L

    override fun observeExercises(bodyPart: BodyPart): Flow<List<Exercise>> = exercises.map { list ->
        observeFailure?.let { throw it }
        list.filter { it.bodyPart == bodyPart && !it.isDeleted }
    }

    override suspend fun getExercise(id: Long): Exercise? = exercises.value.firstOrNull { it.id == id }

    override suspend fun addExercise(bodyPart: BodyPart, name: String, intensityType: IntensityType): Long {
        mutateFailure?.let { throw it }
        val id = nextId++
        exercises.value = exercises.value + Exercise(id, bodyPart, name, intensityType)
        return id
    }

    override suspend fun updateExercise(exercise: Exercise) {
        mutateFailure?.let { throw it }
        // 실제 구현은 삭제 표시를 건드리지 않는다. 페이크도 저장된 값을 유지한다.
        exercises.value = exercises.value.map { stored ->
            if (stored.id == exercise.id) exercise.copy(isDeleted = stored.isDeleted) else stored
        }
    }

    override suspend fun deleteExercise(id: Long) {
        mutateFailure?.let { throw it }
        exercises.value = exercises.value.map { if (it.id == id) it.copy(isDeleted = true) else it }
    }
}
