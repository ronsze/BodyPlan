package kr.sdbk.bodyplan.core.ondevice

import javax.inject.Qualifier

/** 온디바이스 `AiClient`의 이름표. core:network의 제공자별 한정자와 같은 방식이다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OnDeviceApi
