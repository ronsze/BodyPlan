package kr.sdbk.bodyplan.core.data.repository

import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.data.mapper.newEntryEntity
import kr.sdbk.bodyplan.core.data.mapper.newMemoEntity
import kr.sdbk.bodyplan.core.data.mapper.toBodyPartsByDate
import kr.sdbk.bodyplan.core.data.mapper.toDomain
import kr.sdbk.bodyplan.core.data.mapper.toEntities
import kr.sdbk.bodyplan.core.data.mapper.toEntriesByDate
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutLog
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import kr.sdbk.bodyplan.core.local.dao.WorkoutEntryDao
import kr.sdbk.bodyplan.core.local.dao.WorkoutMemoDao

internal class WorkoutLogRepositoryImpl
@Inject
constructor(
    private val workoutEntryDao: WorkoutEntryDao,
    private val workoutMemoDao: WorkoutMemoDao,
) : WorkoutLogRepository {
    // 항목과 메모는 표가 다르지만 화면에는 하루치 기록 하나로 간다. 여기서 합쳐 스트림을 하나로 둔다.
    override fun observeLog(date: LocalDate): Flow<WorkoutLog> = combine(
        workoutEntryDao.observeByDate(date.toEpochDay()),
        workoutMemoDao.observeByDate(date.toEpochDay()),
    ) { rows, memo ->
        WorkoutLog(date = date, entries = rows.map { it.toDomain() }, memo = memo?.text)
    }

    override fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>> =
        workoutEntryDao.observeWithSetsInRange(from.toEpochDay(), to.toEpochDay())
            .map { rows -> rows.toEntriesByDate() }

    override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
        workoutEntryDao.observeInRange(from.toEpochDay(), to.toEpochDay())
            .map { rows -> rows.toBodyPartsByDate() }

    override suspend fun getEntry(id: Long): WorkoutEntry? = workoutEntryDao.getWithSets(id)?.toDomain()

    override suspend fun addEntry(date: LocalDate, exercise: Exercise, sets: List<WorkoutSet>): Long {
        val entity = newEntryEntity(date, exercise, System.currentTimeMillis())
        return workoutEntryDao.insertWithSets(entity, sets.toEntities(entryId = 0L))
    }

    override suspend fun updateEntry(entryId: Long, exercise: Exercise, sets: List<WorkoutSet>) {
        val stored = requireNotNull(workoutEntryDao.getWithSets(entryId)) {
            "수정할 기록이 없습니다: $entryId"
        }
        // 날짜와 생성 시각은 그대로 두고, 종목 스냅샷만 지금 고른 종목으로 갱신한다.
        val updated = stored.entry.copy(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            bodyPart = exercise.bodyPart.name,
            intensityType = exercise.intensityType.name,
        )
        workoutEntryDao.updateWithSets(updated, sets.toEntities(entryId))
    }

    override suspend fun deleteEntry(id: Long) {
        // 세트는 외래 키 CASCADE가 함께 지운다.
        workoutEntryDao.deleteById(id)
    }

    override suspend fun saveMemo(date: LocalDate, text: String) {
        val trimmed = text.trim()
        // 빈 메모를 행으로 남기지 않는다 — 메모 없음과 빈 메모를 구분할 이유가 없다.
        if (trimmed.isEmpty()) {
            workoutMemoDao.deleteByDate(date.toEpochDay())
        } else {
            workoutMemoDao.upsert(newMemoEntity(date, trimmed, System.currentTimeMillis()))
        }
    }
}
