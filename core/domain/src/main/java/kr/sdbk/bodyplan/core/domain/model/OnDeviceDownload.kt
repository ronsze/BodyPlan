package kr.sdbk.bodyplan.core.domain.model

/** 온디바이스 모델 내려받기의 진행. [Completed] 또는 [Failed]로 끝난다. */
sealed interface OnDeviceDownload {
    data class Started(val totalBytes: Long) : OnDeviceDownload

    data class Progress(val downloadedBytes: Long) : OnDeviceDownload

    data object Completed : OnDeviceDownload

    /** [reason]은 화면에 그대로 보이는 한국어 사유다. */
    data class Failed(val reason: String) : OnDeviceDownload
}
