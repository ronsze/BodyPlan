package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 사용자 정보는 한 벌뿐이라 [id]를 0으로 고정해 항상 한 행만 둔다.
 * 저장은 덮어쓰기로 하고 행을 늘리지 않는다.
 */
@Serializable
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val ageYears: Int?,
    val heightCm: Int?,
    val weightKg: Int?,
    val gender: String?,
    /** `Goal.name`을 쉼표로 이어 붙인 것. 비어 있으면 고른 것이 없다. */
    val goals: String,
    val targetWeightKg: Int?,
    val targetNote: String?,
    // 기본값이 있어야 이 컬럼이 생기기 전의 스냅샷도 읽힌다.
    val weeklyWorkoutGoal: Int? = null,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
