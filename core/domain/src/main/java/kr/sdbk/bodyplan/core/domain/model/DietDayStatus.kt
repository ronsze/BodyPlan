package kr.sdbk.bodyplan.core.domain.model

/**
 * 식단 캘린더 셀 하나가 나타내는 상태.
 *
 * 저장하지 않고 기록 유무와 오늘 날짜로 매번 도출한다.
 * [Pending]이던 날은 시간이 지나면 저절로 [Missed]가 되므로 적어 둘 수 없다.
 */
sealed interface DietDayStatus {
    /** 기록이 있는 날. 그 날 첫 항목의 사진 경로를 담는다. */
    data class Recorded(val imagePath: String) : DietDayStatus

    /** 아직 쓸 수 있는 빈 날. 오늘과 어제다. */
    data object Pending : DietDayStatus

    /** 쓸 수 있는 기간이 지나도록 비어 있는 날. 기록하지 않은 것으로 확정한다. */
    data object Missed : DietDayStatus

    /** 아직 오지 않은 날. */
    data object Upcoming : DietDayStatus
}
