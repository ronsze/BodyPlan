package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** [exerciseName]·[bodyPart]·[intensityType]은 작성 시점 종목의 스냅샷이다. */
@Entity(tableName = "workout_entry", indices = [Index("dateEpochDay")])
data class WorkoutEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateEpochDay: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val bodyPart: String,
    val intensityType: String,
    val createdAtMillis: Long,
)
