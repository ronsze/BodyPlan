package kr.sdbk.bodyplan.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.local.entity.ExerciseEntity

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise WHERE bodyPart = :bodyPart AND isDeleted = 0 ORDER BY id ASC")
    fun observeByBodyPart(bodyPart: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Insert
    suspend fun insert(entity: ExerciseEntity): Long

    @Update
    suspend fun update(entity: ExerciseEntity)

    @Query("UPDATE exercise SET isDeleted = 1 WHERE id = :id")
    suspend fun markDeleted(id: Long)
}
