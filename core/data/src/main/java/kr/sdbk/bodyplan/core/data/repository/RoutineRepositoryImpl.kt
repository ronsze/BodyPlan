package kr.sdbk.bodyplan.core.data.repository

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.data.mapper.newRoutineEntity
import kr.sdbk.bodyplan.core.data.mapper.newRoutineEntryEntity
import kr.sdbk.bodyplan.core.data.mapper.toDomain
import kr.sdbk.bodyplan.core.data.mapper.toRoutineSetEntities
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.RoutineRepository
import kr.sdbk.bodyplan.core.local.dao.RoutineDao

internal class RoutineRepositoryImpl
@Inject
constructor(private val routineDao: RoutineDao) : RoutineRepository {
    override fun observeRoutines(): Flow<List<Routine>> =
        routineDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeRoutine(id: Long): Flow<Routine?> = routineDao.observeById(id).map { it?.toDomain() }

    override suspend fun getRoutine(id: Long): Routine? = routineDao.getById(id)?.toDomain()

    override suspend fun addRoutine(name: String, bodyPart: BodyPart): Long =
        routineDao.insert(newRoutineEntity(name, bodyPart, System.currentTimeMillis()))

    override suspend fun deleteRoutine(id: Long) {
        // 항목·세트는 외래 키 CASCADE가 함께 지운다.
        routineDao.deleteById(id)
    }

    override suspend fun getEntry(id: Long): WorkoutEntry? = routineDao.getEntryWithSets(id)?.toDomain()

    override suspend fun addEntry(routineId: Long, exercise: Exercise, sets: List<WorkoutSet>): Long {
        val entity = newRoutineEntryEntity(routineId, exercise, System.currentTimeMillis())
        return routineDao.insertEntryWithSets(entity, sets.toRoutineSetEntities(entryId = 0L))
    }

    override suspend fun updateEntry(entryId: Long, exercise: Exercise, sets: List<WorkoutSet>) {
        val stored = requireNotNull(routineDao.getEntryWithSets(entryId)) {
            "수정할 루틴 항목이 없습니다: $entryId"
        }
        // 소속 루틴과 생성 시각은 그대로 두고, 종목 스냅샷만 지금 고른 종목으로 갱신한다.
        val updated = stored.entry.copy(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            bodyPart = exercise.bodyPart.name,
            intensityType = exercise.intensityType.name,
        )
        routineDao.updateEntryWithSets(updated, sets.toRoutineSetEntities(entryId))
    }

    override suspend fun deleteEntry(id: Long) {
        // 세트는 외래 키 CASCADE가 함께 지운다.
        routineDao.deleteEntryById(id)
    }
}
