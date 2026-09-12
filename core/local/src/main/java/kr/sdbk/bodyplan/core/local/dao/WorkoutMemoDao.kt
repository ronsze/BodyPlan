package kr.sdbk.bodyplan.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.local.entity.WorkoutMemoEntity

@Dao
interface WorkoutMemoDao {
    @Query("SELECT * FROM workout_memo WHERE dateEpochDay = :dateEpochDay")
    fun observeByDate(dateEpochDay: Long): Flow<WorkoutMemoEntity?>

    /** 날짜마다 한 행만 두므로 덮어쓴다. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorkoutMemoEntity)

    @Query("DELETE FROM workout_memo WHERE dateEpochDay = :dateEpochDay")
    suspend fun deleteByDate(dateEpochDay: Long)

    // 백업 스냅샷용. 표 전체를 읽고 통째로 바꾼다 — SnapshotStore만 부른다.
    @Query("SELECT * FROM workout_memo")
    suspend fun getAll(): List<WorkoutMemoEntity>

    @Insert
    suspend fun insertAll(entities: List<WorkoutMemoEntity>)

    @Query("DELETE FROM workout_memo")
    suspend fun deleteAll()
}
