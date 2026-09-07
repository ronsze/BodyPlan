package kr.sdbk.bodyplan.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
}
