package kr.sdbk.bodyplan.core.domain.model

/** 이 기기에서 온디바이스 모델을 쓸 수 있는지. [UNSUPPORTED]면 선택지 자체를 보이지 않는다. */
enum class OnDeviceModelStatus {
    UNSUPPORTED,
    DOWNLOADABLE,
    DOWNLOADING,
    READY,
}
