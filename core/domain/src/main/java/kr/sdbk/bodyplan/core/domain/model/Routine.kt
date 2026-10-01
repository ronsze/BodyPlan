package kr.sdbk.bodyplan.core.domain.model

/**
 * 미리 짜 둔 종목·세트 묶음. 이름으로 구분하며 모든 부위의 종목을 함께 담을 수 있다.
 *
 * 항목은 기록 한 건과 같은 모양이라 [WorkoutEntry]를 그대로 쓴다 — [WorkoutEntry.id]는 루틴 항목의 id다.
 * 일지에 불러올 때는 그 id를 버리고 새 기록으로 넣는다.
 */
data class Routine(val id: Long, val name: String, val entries: List<WorkoutEntry>)

/** 루틴의 세트 총합. */
val Routine.totalSetCount: Int get() = entries.sumOf { it.sets.size }

/**
 * 한 부위의 루틴 분량. [weightVolumeKg]는 그 부위에 무게 종목이 하나도 없으면 `null`이다 —
 * 각도·시간 종목만 있는 부위에 0kg을 적으면 "무게 운동을 했는데 0"으로 읽힌다.
 */
data class BodyPartWorkload(val bodyPart: BodyPart, val entryCount: Int, val setCount: Int, val weightVolumeKg: Int?)

/** 항목을 부위 순서([BodyPart.entries])로 묶어 분량을 센다. 항목이 없는 부위는 담지 않는다. */
fun List<WorkoutEntry>.summarizedByBodyPart(): List<BodyPartWorkload> {
    val byBodyPart = groupBy { it.bodyPart }
    return BodyPart.entries.mapNotNull { bodyPart ->
        val own = byBodyPart[bodyPart] ?: return@mapNotNull null
        val weightEntries = own.filter { it.intensityType == IntensityType.WEIGHT }
        BodyPartWorkload(
            bodyPart = bodyPart,
            entryCount = own.size,
            setCount = own.sumOf { it.sets.size },
            weightVolumeKg = if (weightEntries.isEmpty()) null else weightEntries.sumOf { it.weightVolumeKg },
        )
    }
}
