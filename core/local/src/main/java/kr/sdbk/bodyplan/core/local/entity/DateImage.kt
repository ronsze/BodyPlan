package kr.sdbk.bodyplan.core.local.entity

/** 캘린더 전용 조회 결과. 날짜마다 사진 한 장만 읽으려고 두 컬럼만 담는다. */
data class DateImage(val dateEpochDay: Long, val imageFileName: String)
