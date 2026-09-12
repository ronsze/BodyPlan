package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * [imageFileName]은 파일명만 담는다. 앱 내부 저장소의 절대 경로는 재설치·백업 복원으로
 * 바뀌므로 적어 두면 사진을 잃는다.
 */
@Serializable
@Entity(tableName = "diet_entry", indices = [Index("dateEpochDay")])
data class DietEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateEpochDay: Long,
    val imageFileName: String,
    val memo: String?,
    val createdAtMillis: Long,
)
