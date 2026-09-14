package kr.sdbk.bodyplan.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.local.entity.DateBodyPart
import kr.sdbk.bodyplan.core.local.entity.ExerciseBestRow
import kr.sdbk.bodyplan.core.local.entity.WorkoutEntryEntity
import kr.sdbk.bodyplan.core.local.entity.WorkoutEntryWithSets
import kr.sdbk.bodyplan.core.local.entity.WorkoutSetEntity

@Dao
interface WorkoutEntryDao {
    @Transaction
    @Query("SELECT * FROM workout_entry WHERE dateEpochDay = :epochDay ORDER BY createdAtMillis ASC, id ASC")
    fun observeByDate(epochDay: Long): Flow<List<WorkoutEntryWithSets>>

    @Query(
        "SELECT DISTINCT dateEpochDay, bodyPart FROM workout_entry " +
            "WHERE dateEpochDay BETWEEN :from AND :to",
    )
    fun observeInRange(from: Long, to: Long): Flow<List<DateBodyPart>>

    /** 기간 집계용. 부위만 보는 [observeInRange]와 달리 세트까지 싣는다. */
    @Transaction
    @Query(
        "SELECT * FROM workout_entry WHERE dateEpochDay BETWEEN :from AND :to " +
            "ORDER BY dateEpochDay ASC, createdAtMillis ASC, id ASC",
    )
    fun observeWithSetsInRange(from: Long, to: Long): Flow<List<WorkoutEntryWithSets>>

    @Transaction
    @Query("SELECT * FROM workout_entry WHERE id = :id")
    suspend fun getWithSets(id: Long): WorkoutEntryWithSets?

    /**
     * 종목·축마다 그 날짜 이전 세트의 최고 강도값·최고 횟수. 기록이 없는 종목은 행이 없다.
     *
     * 축까지 갈라 세는 것은 종목의 축을 바꿀 수 있어서다 — 무게로 재던 시절의 80이 각도 종목의 최고값으로 남으면 안 된다.
     */
    @Query(
        "SELECT e.exerciseId AS exerciseId, e.intensityType AS intensityType, " +
            "MAX(s.intensityValue) AS maxIntensityValue, MAX(s.repeatCount) AS maxRepeatCount " +
            "FROM workout_entry e JOIN workout_set s ON s.entryId = e.id " +
            "WHERE e.dateEpochDay < :beforeEpochDay GROUP BY e.exerciseId, e.intensityType",
    )
    fun observeBestBefore(beforeEpochDay: Long): Flow<List<ExerciseBestRow>>

    /** 그 종목의 가장 최근 기록 하나. 같은 날이면 나중에 만든 것. */
    @Transaction
    @Query(
        "SELECT * FROM workout_entry WHERE exerciseId = :exerciseId AND dateEpochDay <= :untilEpochDay " +
            "ORDER BY dateEpochDay DESC, createdAtMillis DESC, id DESC LIMIT 1",
    )
    suspend fun getLatestByExercise(exerciseId: Long, untilEpochDay: Long): WorkoutEntryWithSets?

    @Insert
    suspend fun insert(entity: WorkoutEntryEntity): Long

    @Update
    suspend fun update(entity: WorkoutEntryEntity)

    @Query("DELETE FROM workout_entry WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Query("DELETE FROM workout_set WHERE entryId = :entryId")
    suspend fun deleteSetsByEntryId(entryId: Long)

    /** 세트는 최대 열 줄이라 줄 단위로 비교하지 않고 통째로 갈아끼운다. */
    @Transaction
    suspend fun replaceSets(entryId: Long, sets: List<WorkoutSetEntity>) {
        deleteSetsByEntryId(entryId)
        insertSets(sets.map { it.copy(entryId = entryId) })
    }

    @Transaction
    suspend fun insertWithSets(entity: WorkoutEntryEntity, sets: List<WorkoutSetEntity>): Long {
        val entryId = insert(entity)
        insertSets(sets.map { it.copy(entryId = entryId) })
        return entryId
    }

    /** 루틴 불러오기처럼 여러 건을 한 번에 넣을 때 쓴다. 하나라도 실패하면 아무것도 남지 않는다. */
    @Transaction
    suspend fun insertAllWithSets(items: List<WorkoutEntryWithSets>) {
        items.forEach { insertWithSets(it.entry, it.sets) }
    }

    @Transaction
    suspend fun updateWithSets(entity: WorkoutEntryEntity, sets: List<WorkoutSetEntity>) {
        update(entity)
        replaceSets(entity.id, sets)
    }

    // 백업 스냅샷용. 표 전체를 읽고 통째로 바꾼다 — SnapshotStore만 부른다.
    @Query("SELECT * FROM workout_entry")
    suspend fun getAll(): List<WorkoutEntryEntity>

    @Query("SELECT * FROM workout_set")
    suspend fun getAllSets(): List<WorkoutSetEntity>

    @Insert
    suspend fun insertAll(entities: List<WorkoutEntryEntity>)

    /** 세트는 외래 키 CASCADE가 함께 지운다. */
    @Query("DELETE FROM workout_entry")
    suspend fun deleteAll()
}
