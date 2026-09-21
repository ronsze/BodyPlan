package kr.sdbk.bodyplan.core.data.repository

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.OnDeviceDownload
import kr.sdbk.bodyplan.core.domain.model.OnDeviceModelStatus
import kr.sdbk.bodyplan.core.domain.repository.OnDeviceAiRepository
import kr.sdbk.bodyplan.core.ondevice.GeminiNano
import kr.sdbk.bodyplan.core.ondevice.OnDeviceDownloadEvent
import kr.sdbk.bodyplan.core.ondevice.OnDeviceFeatureStatus

internal class OnDeviceAiRepositoryImpl
@Inject
constructor(private val geminiNano: GeminiNano) :
    OnDeviceAiRepository {
    override suspend fun getStatus(): OnDeviceModelStatus = when (geminiNano.getStatus()) {
        OnDeviceFeatureStatus.UNAVAILABLE -> OnDeviceModelStatus.UNSUPPORTED
        OnDeviceFeatureStatus.DOWNLOADABLE -> OnDeviceModelStatus.DOWNLOADABLE
        OnDeviceFeatureStatus.DOWNLOADING -> OnDeviceModelStatus.DOWNLOADING
        OnDeviceFeatureStatus.AVAILABLE -> OnDeviceModelStatus.READY
    }

    override fun download(): Flow<OnDeviceDownload> = geminiNano.download().map { event ->
        when (event) {
            is OnDeviceDownloadEvent.Started -> OnDeviceDownload.Started(totalBytes = event.bytesToDownload)
            is OnDeviceDownloadEvent.Progress -> OnDeviceDownload.Progress(downloadedBytes = event.bytesDownloaded)
            is OnDeviceDownloadEvent.Completed -> OnDeviceDownload.Completed
            is OnDeviceDownloadEvent.Failed -> OnDeviceDownload.Failed(reason = event.exception.reason)
        }
    }
}
