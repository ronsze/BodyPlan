package kr.sdbk.bodyplan.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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

    /**
     * 삭제 표시를 뺀 나머지만 갱신한다. 엔티티를 통째로 덮으면 호출부가 들고 온 기본값이
     * 이미 지워진 종목을 되살린다.
     */
    @Query(
        "UPDATE exercise SET bodyPart = :bodyPart, name = :name, intensityType = :intensityType " +
            "WHERE id = :id",
    )
    suspend fun updateFields(id: Long, bodyPart: String, name: String, intensityType: String)

    @Query("UPDATE exercise SET isDeleted = 1 WHERE id = :id")
    suspend fun markDeleted(id: Long)
}
