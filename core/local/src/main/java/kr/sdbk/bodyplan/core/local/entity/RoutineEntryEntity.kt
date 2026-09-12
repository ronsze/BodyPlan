package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 루틴 항목. `workout_entry`와 같은 스냅샷 컬럼을 가지며 날짜 대신 루틴에 묶인다.
 * [exerciseName]·[bodyPart]·[intensityType]은 작성 시점 종목의 스냅샷이다.
 */
@Entity(
    tableName = "routine_entry",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineId")],
)
data class RoutineEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val bodyPart: String,
    val intensityType: String,
    val createdAtMillis: Long,
)
