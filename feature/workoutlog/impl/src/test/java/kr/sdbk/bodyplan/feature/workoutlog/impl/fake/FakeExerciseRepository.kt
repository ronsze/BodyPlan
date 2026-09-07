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

    override fun observeExercises(bodyPart: BodyPart): Flow<List<Exercise>> = exercises.map { list ->
        observeFailure?.let { throw it }
        list.filter { it.bodyPart == bodyPart && !it.isDeleted }
    }

    override suspend fun getExercise(id: Long): Exercise? = exercises.value.firstOrNull { it.id == id }

    override suspend fun addExercise(bodyPart: BodyPart, name: String, intensityType: IntensityType): Long =
        throw UnsupportedOperationException("단위 4에서 쓴다")

    override suspend fun updateExercise(exercise: Exercise) = throw UnsupportedOperationException("단위 4에서 쓴다")

    override suspend fun deleteExercise(id: Long) = throw UnsupportedOperationException("단위 4에서 쓴다")
}
