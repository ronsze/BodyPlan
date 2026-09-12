package kr.sdbk.bodyplan.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.local.entity.RoutineEntity
import kr.sdbk.bodyplan.core.local.entity.RoutineEntryEntity
import kr.sdbk.bodyplan.core.local.entity.RoutineEntryWithSets
import kr.sdbk.bodyplan.core.local.entity.RoutineSetEntity
import kr.sdbk.bodyplan.core.local.entity.RoutineWithEntries

@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT * FROM routine ORDER BY createdAtMillis ASC, id ASC")
    fun observeAll(): Flow<List<RoutineWithEntries>>

    @Transaction
    @Query("SELECT * FROM routine WHERE id = :id")
    fun observeById(id: Long): Flow<RoutineWithEntries?>

    @Transaction
    @Query("SELECT * FROM routine WHERE id = :id")
    suspend fun getById(id: Long): RoutineWithEntries?

    @Insert
    suspend fun insert(entity: RoutineEntity): Long

    /** 항목과 세트는 외래 키 CASCADE가 함께 지운다. */
    @Query("DELETE FROM routine WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    @Query("SELECT * FROM routine_entry WHERE id = :id")
    suspend fun getEntryWithSets(id: Long): RoutineEntryWithSets?

    @Insert
    suspend fun insertEntry(entity: RoutineEntryEntity): Long

    @Update
    suspend fun updateEntry(entity: RoutineEntryEntity)

    @Query("DELETE FROM routine_entry WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Insert
    suspend fun insertSets(sets: List<RoutineSetEntity>)

    @Query("DELETE FROM routine_set WHERE entryId = :entryId")
    suspend fun deleteSetsByEntryId(entryId: Long)

    @Transaction
    suspend fun insertEntryWithSets(entity: RoutineEntryEntity, sets: List<RoutineSetEntity>): Long {
        val entryId = insertEntry(entity)
        insertSets(sets.map { it.copy(entryId = entryId) })
        return entryId
    }

    /** 세트는 최대 열 줄이라 줄 단위로 비교하지 않고 통째로 갈아끼운다. */
    @Transaction
    suspend fun updateEntryWithSets(entity: RoutineEntryEntity, sets: List<RoutineSetEntity>) {
        updateEntry(entity)
        deleteSetsByEntryId(entity.id)
        insertSets(sets.map { it.copy(entryId = entity.id) })
    }

    // 백업 스냅샷용. 표 전체를 읽고 통째로 바꾼다 — SnapshotStore만 부른다.
    @Query("SELECT * FROM routine")
    suspend fun getAll(): List<RoutineEntity>

    @Query("SELECT * FROM routine_entry")
    suspend fun getAllEntries(): List<RoutineEntryEntity>

    @Query("SELECT * FROM routine_set")
    suspend fun getAllSets(): List<RoutineSetEntity>

    @Insert
    suspend fun insertAll(entities: List<RoutineEntity>)

    @Insert
    suspend fun insertEntries(entities: List<RoutineEntryEntity>)

    /** 항목과 세트는 외래 키 CASCADE가 함께 지운다. */
    @Query("DELETE FROM routine")
    suspend fun deleteAll()
}
