package kr.sdbk.bodyplan.feature.my.impl.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kr.sdbk.bodyplan.core.domain.model.OnDeviceDownload
import kr.sdbk.bodyplan.core.domain.model.OnDeviceModelStatus
import kr.sdbk.bodyplan.core.domain.repository.OnDeviceAiRepository

internal class FakeOnDeviceAiRepository(var status: OnDeviceModelStatus = OnDeviceModelStatus.UNSUPPORTED) :
    OnDeviceAiRepository {
    var statusFailure: Throwable? = null

    /** 테스트가 진행 이벤트를 하나씩 밀어 넣는다. 수집 전에 밀어 넣은 것은 버려진다. */
    val downloads = MutableSharedFlow<OnDeviceDownload>()

    var downloadCallCount: Int = 0
        private set

    override suspend fun getStatus(): OnDeviceModelStatus {
        statusFailure?.let { throw it }
        return status
    }

    override fun download(): Flow<OnDeviceDownload> {
        downloadCallCount++
        return downloads
    }
}
