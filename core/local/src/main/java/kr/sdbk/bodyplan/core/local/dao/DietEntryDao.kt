package kr.sdbk.bodyplan.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.local.entity.DateImage
import kr.sdbk.bodyplan.core.local.entity.DietEntryEntity

@Dao
interface DietEntryDao {
    @Query("SELECT * FROM diet_entry WHERE dateEpochDay = :epochDay ORDER BY createdAtMillis ASC, id ASC")
    fun observeByDate(epochDay: Long): Flow<List<DietEntryEntity>>

    /** 날짜마다 가장 먼저 들어온 한 행만 읽는다. 항목이 많은 날에도 목록 전체를 끌어오지 않는다. */
    @Query(
        "SELECT dateEpochDay, imageFileName FROM diet_entry AS e " +
            "WHERE e.dateEpochDay BETWEEN :from AND :to " +
            "AND e.createdAtMillis = (" +
            "SELECT MIN(createdAtMillis) FROM diet_entry AS inner_entry " +
            "WHERE inner_entry.dateEpochDay = e.dateEpochDay" +
            ") " +
            "GROUP BY e.dateEpochDay",
    )
    fun observeFirstImageInRange(from: Long, to: Long): Flow<List<DateImage>>

    @Query("SELECT * FROM diet_entry WHERE id = :id")
    suspend fun getById(id: Long): DietEntryEntity?

    @Insert
    suspend fun insert(entity: DietEntryEntity): Long

    @Update
    suspend fun update(entity: DietEntryEntity)

    @Query("DELETE FROM diet_entry WHERE id = :id")
    suspend fun deleteById(id: Long)

    // 백업 스냅샷용. 표 전체를 읽고 통째로 바꾼다 — SnapshotStore만 부른다.
    @Query("SELECT * FROM diet_entry")
    suspend fun getAll(): List<DietEntryEntity>

    @Insert
    suspend fun insertAll(entities: List<DietEntryEntity>)

    @Query("DELETE FROM diet_entry")
    suspend fun deleteAll()
}
