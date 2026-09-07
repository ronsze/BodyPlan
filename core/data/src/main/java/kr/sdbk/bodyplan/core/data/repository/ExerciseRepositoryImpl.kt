package kr.sdbk.bodyplan.core.data.repository

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.data.mapper.toDomain
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.repository.ExerciseRepository
import kr.sdbk.bodyplan.core.local.dao.ExerciseDao
import kr.sdbk.bodyplan.core.local.entity.ExerciseEntity

internal class ExerciseRepositoryImpl
@Inject
constructor(private val exerciseDao: ExerciseDao) : ExerciseRepository {
    override fun observeExercises(bodyPart: BodyPart): Flow<List<Exercise>> =
        exerciseDao.observeByBodyPart(bodyPart.name).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getExercise(id: Long): Exercise? = exerciseDao.getById(id)?.toDomain()

    override suspend fun addExercise(bodyPart: BodyPart, name: String, intensityType: IntensityType): Long =
        exerciseDao.insert(
            ExerciseEntity(
                bodyPart = bodyPart.name,
                name = name,
                intensityType = intensityType.name,
            ),
        )

    override suspend fun updateExercise(exercise: Exercise) {
        exerciseDao.updateFields(
            id = exercise.id,
            bodyPart = exercise.bodyPart.name,
            name = exercise.name,
            intensityType = exercise.intensityType.name,
        )
    }

    override suspend fun deleteExercise(id: Long) {
        // 과거 기록이 이 종목을 가리키고 있어 행을 지우지 않고 숨기기만 한다.
        exerciseDao.markDeleted(id)
    }
}
