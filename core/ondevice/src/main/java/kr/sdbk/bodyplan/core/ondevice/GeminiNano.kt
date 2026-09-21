package kr.sdbk.bodyplan.core.ondevice

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * 기기 안 Gemini Nano의 준비 상태와 내려받기.
 *
 * 모델 핸들은 하나만 두고 앱이 사는 동안 닫지 않는다. 열고 닫기를 반복하면 매번 AICore와 다시 붙는다.
 */
@Singleton
class GeminiNano
@Inject
constructor() {
    internal val model: GenerativeModel by lazy { Generation.getClient() }

    suspend fun getStatus(): OnDeviceFeatureStatus = when (model.checkStatus()) {
        FeatureStatus.AVAILABLE -> OnDeviceFeatureStatus.AVAILABLE
        FeatureStatus.DOWNLOADABLE -> OnDeviceFeatureStatus.DOWNLOADABLE
        FeatureStatus.DOWNLOADING -> OnDeviceFeatureStatus.DOWNLOADING
        else -> OnDeviceFeatureStatus.UNAVAILABLE
    }

    /** 실패는 흐름을 끊지 않고 [OnDeviceDownloadEvent.Failed]로 낸다. 화면이 사유를 보이고 다시 시도할 수 있게. */
    fun download(): Flow<OnDeviceDownloadEvent> = model.download()
        .map { status ->
            when (status) {
                is DownloadStatus.DownloadStarted -> OnDeviceDownloadEvent.Started(status.bytesToDownload)
                is DownloadStatus.DownloadProgress -> OnDeviceDownloadEvent.Progress(status.totalBytesDownloaded)
                is DownloadStatus.DownloadFailed -> OnDeviceDownloadEvent.Failed(status.e.toOnDeviceFailure())
                else -> OnDeviceDownloadEvent.Completed
            }
        }
        .catch { throwable ->
            if (throwable !is GenAiException) throw throwable
            emit(OnDeviceDownloadEvent.Failed(throwable.toOnDeviceFailure()))
        }
}
