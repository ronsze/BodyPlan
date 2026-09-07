package kr.sdbk.bodyplan.core.data.image

import javax.inject.Qualifier

/** 식단 사진 저장소. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DietImages

/** 인바디 사진 저장소. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class InbodyImages
