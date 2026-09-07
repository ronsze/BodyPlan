package kr.sdbk.bodyplan.core.domain.model

/**
 * 운동 종목. 기본 목록이 심어져 있고 사용자가 더하거나 고칠 수 있다.
 *
 * [isDeleted]는 목록에서만 숨긴다. 과거 기록이 이 종목을 가리키고 있어 행을 지우지 않는다.
 */
data class Exercise(
    val id: Long,
    val bodyPart: BodyPart,
    val name: String,
    val intensityType: IntensityType,
    val isDeleted: Boolean = false,
)
