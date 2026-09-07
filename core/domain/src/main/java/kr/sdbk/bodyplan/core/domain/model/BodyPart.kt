package kr.sdbk.bodyplan.core.domain.model

/**
 * 운동 부위 분할. 기록과 종목이 이 단위로 묶인다.
 *
 * [CARDIO]는 몸의 부위가 아니지만, 종목을 고르는 갈래가 이것 하나뿐이라 같은 자리에 둔다.
 * 갈래를 따로 만들면 종목·기록·캘린더가 전부 두 축을 알아야 한다.
 */
enum class BodyPart {
    CHEST,
    BACK,
    SHOULDER,
    LEG,
    BICEPS,
    TRICEPS,
    CARDIO,
}
