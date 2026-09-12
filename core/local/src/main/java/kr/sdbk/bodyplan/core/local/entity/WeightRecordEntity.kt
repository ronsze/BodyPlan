package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 하루치 체중. 날짜가 곧 기본 키다 — 하루에 한 값만 남기므로 대리 키를 두지 않는다.
 * 같은 날짜에 다시 넣으면 덮어쓴다.
 *
 * [updatedAtMillis]는 아직 화면에 쓰지 않는다. 언제 고친 값인지 나중에 보이려고 남긴다.
 */
@Serializable
@Entity(tableName = "weight_record")
data class WeightRecordEntity(@PrimaryKey val dateEpochDay: Long, val weightKg: Double, val updatedAtMillis: Long)
