package kr.sdbk.bodyplan.core.domain.model

/**
 * 종목의 강도를 재는 축.
 *
 * 기구를 쓰지 않는 종목은 무게 대신 각도로 재고, 유산소는 시간으로 잰다.
 * 유산소는 거리로도 잴 수 있지만 기구가 거리를 늘 주지는 않아 시간을 쓴다.
 */
enum class IntensityType {
    WEIGHT,
    ANGLE,
    DURATION,
}

/** 이 축은 횟수를 세지 않는다. 한 세트가 곧 한 회차다. */
val IntensityType.countsRepeats: Boolean get() = this != IntensityType.DURATION
