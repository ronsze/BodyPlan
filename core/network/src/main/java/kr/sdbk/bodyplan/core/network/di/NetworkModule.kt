package kr.sdbk.bodyplan.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kr.sdbk.bodyplan.core.network.AiClient
import kr.sdbk.bodyplan.core.network.ClaudeApi
import kr.sdbk.bodyplan.core.network.ClaudeClient
import kr.sdbk.bodyplan.core.network.GeminiApi
import kr.sdbk.bodyplan.core.network.GeminiClient
import kr.sdbk.bodyplan.core.network.GptApi
import kr.sdbk.bodyplan.core.network.GptClient
import okhttp3.OkHttpClient

/** 제공자별 클라이언트를 이름표로 구분해 내보낸다. 어느 것을 쓸지는 `core:data`가 고른다. */
@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        // 분석은 답이 길어 오래 걸린다. 기본 10초로는 정상 응답도 끊긴다.
        .callTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @ClaudeApi
    fun provideClaudeClient(client: ClaudeClient): AiClient = client

    @Provides
    @GptApi
    fun provideGptClient(client: GptClient): AiClient = client

    @Provides
    @GeminiApi
    fun provideGeminiClient(client: GeminiClient): AiClient = client
}
