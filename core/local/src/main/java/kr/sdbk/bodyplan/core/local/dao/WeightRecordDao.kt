package kr.sdbk.bodyplan.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.local.entity.WeightRecordEntity

@Dao
interface WeightRecordDao {
    @Query("SELECT * FROM weight_record WHERE dateEpochDay BETWEEN :from AND :to ORDER BY dateEpochDay ASC")
    fun observeInRange(from: Long, to: Long): Flow<List<WeightRecordEntity>>

    /** 날짜마다 한 행만 두므로 덮어쓴다. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeightRecordEntity)
}
