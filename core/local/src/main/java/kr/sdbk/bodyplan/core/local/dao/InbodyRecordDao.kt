package kr.sdbk.bodyplan.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.local.entity.InbodyRecordEntity

@Dao
interface InbodyRecordDao {
    @Query("SELECT * FROM inbody_record ORDER BY measuredOnEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<InbodyRecordEntity>>

    @Query("SELECT * FROM inbody_record WHERE id = :id")
    fun observe(id: Long): Flow<InbodyRecordEntity?>

    @Query("SELECT * FROM inbody_record WHERE id = :id")
    suspend fun getById(id: Long): InbodyRecordEntity?

    @Insert
    suspend fun insert(entity: InbodyRecordEntity): Long

    @Query("UPDATE inbody_record SET failureReason = :reason WHERE id = :id")
    suspend fun updateFailureReason(id: Long, reason: String?)

    // 백업 스냅샷용. 표 전체를 읽고 통째로 바꾼다 — SnapshotStore만 부른다.
    @Query("SELECT * FROM inbody_record")
    suspend fun getAll(): List<InbodyRecordEntity>

    @Insert
    suspend fun insertAll(entities: List<InbodyRecordEntity>)

    @Query("DELETE FROM inbody_record")
    suspend fun deleteAll()
}
