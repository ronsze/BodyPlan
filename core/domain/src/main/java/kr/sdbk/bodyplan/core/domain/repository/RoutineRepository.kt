package kr.sdbk.bodyplan.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet

/** 루틴 저장소. 실패는 삼키지 않고 호출부로 던진다. */
interface RoutineRepository {
    /** 만든 순서대로 낸다. 부위별 묶음은 화면이 한다. */
    fun observeRoutines(): Flow<List<Routine>>

    /** 루틴이 없으면 null을 흘린다. */
    fun observeRoutine(id: Long): Flow<Routine?>

    suspend fun getRoutine(id: Long): Routine?

    suspend fun addRoutine(name: String, bodyPart: BodyPart): Long

    /** 항목과 세트도 함께 지운다. */
    suspend fun deleteRoutine(id: Long)

    suspend fun getEntry(id: Long): WorkoutEntry?

    suspend fun addEntry(routineId: Long, exercise: Exercise, sets: List<WorkoutSet>): Long

    /** 종목까지 바꿀 수 있으므로 [exercise]를 받아 스냅샷을 갱신하고 세트를 갈아끼운다. */
    suspend fun updateEntry(entryId: Long, exercise: Exercise, sets: List<WorkoutSet>)

    suspend fun deleteEntry(id: Long)
}
