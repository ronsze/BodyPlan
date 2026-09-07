package kr.sdbk.bodyplan.core.local.entity

/** 캘린더 전용 조회 결과. 세트를 읽지 않으려고 두 컬럼만 담는다. */
data class DateBodyPart(val dateEpochDay: Long, val bodyPart: String)
