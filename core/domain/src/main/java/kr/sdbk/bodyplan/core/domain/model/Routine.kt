package kr.sdbk.bodyplan.core.domain.model

/**
 * 미리 짜 둔 종목·세트 묶음. 부위 하나에 묶이고 이름으로 구분한다.
 *
 * 항목은 기록 한 건과 같은 모양이라 [WorkoutEntry]를 그대로 쓴다 — [WorkoutEntry.id]는 루틴 항목의 id다.
 * 일지에 불러올 때는 그 id를 버리고 새 기록으로 넣는다.
 */
data class Routine(val id: Long, val name: String, val bodyPart: BodyPart, val entries: List<WorkoutEntry>)

/** 루틴을 부위 순서([BodyPart.entries])로 묶는다. 루틴이 없는 부위는 담지 않는다. */
fun List<Routine>.groupedByBodyPart(): List<Pair<BodyPart, List<Routine>>> {
    val byBodyPart = groupBy { it.bodyPart }
    return BodyPart.entries.mapNotNull { bodyPart -> byBodyPart[bodyPart]?.let { bodyPart to it } }
}
