package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_set",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId")],
)
data class RoutineSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val entryId: Long,
    val setNumber: Int,
    val repeatCount: Int,
    val intensityValue: Int,
)
