package kr.sdbk.bodyplan.core.domain.model

/**
 * 캘린더 셀 하나가 나타내는 상태.
 *
 * 저장하지 않고 기록 유무와 오늘 날짜로 매번 도출한다.
 * [Pending]이던 날은 시간이 지나면 저절로 [Rest]가 되므로 적어 둘 수 없다.
 */
sealed interface DayStatus {
    /** 기록이 있는 날. */
    data class Recorded(val bodyParts: Set<BodyPart>) : DayStatus

    /** 아직 작성할 수 있는 빈 날. */
    data object Pending : DayStatus

    /** 작성 기간이 지나도록 비어 있어 휴식으로 확정된 날. */
    data object Rest : DayStatus

    /** 아직 오지 않은 날. */
    data object Upcoming : DayStatus
}
