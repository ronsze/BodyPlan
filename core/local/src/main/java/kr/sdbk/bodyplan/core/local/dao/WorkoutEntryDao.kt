package kr.sdbk.bodyplan.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.local.entity.DateBodyPart
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

    @Transaction
    suspend fun updateWithSets(entity: WorkoutEntryEntity, sets: List<WorkoutSetEntity>) {
        update(entity)
        replaceSets(entity.id, sets)
    }
}
