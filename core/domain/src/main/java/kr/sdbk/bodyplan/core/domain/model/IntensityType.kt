package kr.sdbk.bodyplan.core.domain.model

/** 종목의 강도를 재는 축. 기구를 쓰지 않는 종목은 무게 대신 각도로 잰다. */
enum class IntensityType {
    WEIGHT,
    ANGLE,
}
