package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 하루치 운동 메모. 날짜가 곧 기본 키다 — 하루에 한 개만 남기므로 대리 키를 두지 않는다.
 * 같은 날짜에 다시 넣으면 덮어쓰고, 내용을 비우면 행을 지운다.
 *
 * [text]는 칩만 고른 날 빈 글이다. [condition]·[effort]는 enum 이름, [painParts]는 부위 이름을 쉼표로 이은 글이며
 * 고르지 않았으면 `null`이다 — 칩 컬럼은 나중에 더해져 기존 행이 값을 갖지 않는다.
 *
 * [updatedAtMillis]는 아직 화면에 쓰지 않는다. 언제 고친 메모인지 나중에 보이려고 남긴다.
 */
@Serializable
@Entity(tableName = "workout_memo")
data class WorkoutMemoEntity(
    @PrimaryKey val dateEpochDay: Long,
    val text: String,
    val updatedAtMillis: Long,
    val condition: String? = null,
    val effort: String? = null,
    val painParts: String? = null,
)
