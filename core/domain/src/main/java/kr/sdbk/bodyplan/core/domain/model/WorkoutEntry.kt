package kr.sdbk.bodyplan.core.domain.model

/**
 * 기록 한 건. 종목 하나와 그 종목으로 수행한 세트 목록이다.
 *
 * [exerciseName]·[bodyPart]·[intensityType]은 작성 시점의 스냅샷이다.
 * 종목을 지우거나 이름을 바꿔도 과거 기록의 표기가 따라 바뀌지 않게 한다.
 */
data class WorkoutEntry(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val bodyPart: BodyPart,
    val intensityType: IntensityType,
    val sets: List<WorkoutSet>,
)
