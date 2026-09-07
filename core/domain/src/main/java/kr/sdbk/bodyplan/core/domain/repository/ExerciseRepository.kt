package kr.sdbk.bodyplan.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.IntensityType

/** 운동 종목 저장소. 실패는 삼키지 않고 호출부로 던진다. */
interface ExerciseRepository {
    /** 삭제되지 않은 종목만 낸다. */
    fun observeExercises(bodyPart: BodyPart): Flow<List<Exercise>>

    /** 삭제된 종목도 낸다. 과거 기록을 수정할 때 원래 종목을 찾기 위한 것이다. */
    suspend fun getExercise(id: Long): Exercise?

    suspend fun addExercise(bodyPart: BodyPart, name: String, intensityType: IntensityType): Long

    suspend fun updateExercise(exercise: Exercise)

    suspend fun deleteExercise(id: Long)
}
