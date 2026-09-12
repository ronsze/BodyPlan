package kr.sdbk.bodyplan.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.local.entity.UserProfileEntity

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = ${UserProfileEntity.SINGLE_ROW_ID}")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = ${UserProfileEntity.SINGLE_ROW_ID}")
    suspend fun getProfile(): UserProfileEntity?

    /** 한 행만 두므로 덮어쓴다. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserProfileEntity)

    // 백업 스냅샷용. 표 전체를 읽고 통째로 바꾼다 — SnapshotStore만 부른다.
    @Query("SELECT * FROM user_profile")
    suspend fun getAll(): List<UserProfileEntity>

    @Insert
    suspend fun insertAll(entities: List<UserProfileEntity>)

    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}
