package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 미리 짜 둔 종목·세트 묶음의 머리. 항목은 [RoutineEntryEntity]가 이 행을 가리킨다. */
@Entity(tableName = "routine")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val bodyPart: String,
    val createdAtMillis: Long,
)
