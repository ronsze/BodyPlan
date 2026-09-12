package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val bodyPart: String,
    val name: String,
    val intensityType: String,
    val isDeleted: Boolean = false,
)
