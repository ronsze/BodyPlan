package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/** [sets]의 순서는 보장되지 않는다. 도메인으로 옮길 때 `setNumber`로 정렬한다. */
data class WorkoutEntryWithSets(
    @Embedded val entry: WorkoutEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val sets: List<WorkoutSetEntity>,
)
