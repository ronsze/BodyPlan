package kr.sdbk.bodyplan.core.ondevice

/** ML Kit의 `FeatureStatus` Int 상수를 감싼 것. 이 모듈 밖으로 SDK 타입을 내보내지 않는다. */
enum class OnDeviceFeatureStatus {
    UNAVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    AVAILABLE,
}

/** 내려받기 진행. ML Kit의 `DownloadStatus`를 감싼 것. */
sealed interface OnDeviceDownloadEvent {
    data class Started(val bytesToDownload: Long) : OnDeviceDownloadEvent

    data class Progress(val bytesDownloaded: Long) : OnDeviceDownloadEvent

    data object Completed : OnDeviceDownloadEvent

    data class Failed(val exception: OnDeviceAiException) : OnDeviceDownloadEvent
}
