package kr.sdbk.bodyplan.core.network

import javax.inject.Qualifier

/**
 * 제공자별 [AiClient]를 구분하는 이름표.
 *
 * 문자열 대신 한정자를 쓰는 것은 오타가 컴파일에서 걸리게 하기 위해서다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClaudeApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GptApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiApi
