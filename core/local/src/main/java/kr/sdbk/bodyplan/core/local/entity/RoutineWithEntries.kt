package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/** [entries]의 순서는 보장되지 않는다. 도메인으로 옮길 때 `createdAtMillis`, `id`로 정렬한다. */
data class RoutineWithEntries(
    @Embedded val routine: RoutineEntity,
    @Relation(entity = RoutineEntryEntity::class, parentColumn = "id", entityColumn = "routineId")
    val entries: List<RoutineEntryWithSets>,
)
