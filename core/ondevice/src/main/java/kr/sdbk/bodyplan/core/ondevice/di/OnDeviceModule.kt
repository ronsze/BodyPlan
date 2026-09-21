package kr.sdbk.bodyplan.core.ondevice.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.sdbk.bodyplan.core.network.AiClient
import kr.sdbk.bodyplan.core.ondevice.GeminiNanoClient
import kr.sdbk.bodyplan.core.ondevice.OnDeviceApi

/** 온디바이스 클라이언트를 이름표로 내보낸다. 어느 것을 쓸지는 `core:data`가 고른다. */
@Module
@InstallIn(SingletonComponent::class)
internal object OnDeviceModule {
    @Provides
    @OnDeviceApi
    fun provideOnDeviceClient(client: GeminiNanoClient): AiClient = client
}
