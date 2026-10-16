package kr.sdbk.bodyplan.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.local.entity.AnalysisResultEntity

@Dao
interface AnalysisResultDao {
    @Query(
        "SELECT * FROM analysis_result WHERE kind = :kind AND scopeKey = :scopeKey " +
            "ORDER BY createdAtMillis DESC, id DESC LIMIT 1",
    )
    fun observeLatest(kind: String, scopeKey: String): Flow<AnalysisResultEntity?>

    @Query(
        "SELECT * FROM analysis_result WHERE kind = :kind AND scopeKey IN (:scopeKeys) " +
            "ORDER BY createdAtMillis DESC, id DESC",
    )
    suspend fun getByScopeKeys(kind: String, scopeKeys: List<String>): List<AnalysisResultEntity>

    @Query("SELECT * FROM analysis_result WHERE kind = :kind ORDER BY createdAtMillis DESC, id DESC")
    fun observeByKind(kind: String): Flow<List<AnalysisResultEntity>>

    @Insert
    suspend fun insert(entity: AnalysisResultEntity): Long

    @Query("DELETE FROM analysis_result WHERE kind = :kind AND scopeKey = :scopeKey")
    suspend fun deleteByScope(kind: String, scopeKey: String)

    /** 같은 열쇠의 결과를 지우고 하나만 남긴다. 중간에 실패하면 이전 결과가 그대로여야 해 한 트랜잭션이다. */
    @Transaction
    suspend fun replace(entity: AnalysisResultEntity) {
        deleteByScope(entity.kind, entity.scopeKey)
        insert(entity)
    }

    // 백업 스냅샷용. 표 전체를 읽고 통째로 바꾼다 — SnapshotStore만 부른다.
    @Query("SELECT * FROM analysis_result")
    suspend fun getAll(): List<AnalysisResultEntity>

    @Insert
    suspend fun insertAll(entities: List<AnalysisResultEntity>)

    @Query("DELETE FROM analysis_result")
    suspend fun deleteAll()
}
