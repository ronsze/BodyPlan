package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate

/** 하루치 기록. [memo]는 그날 하나만 남기는 자유 메모이며, 없으면 null이다. */
data class WorkoutLog(val date: LocalDate, val entries: List<WorkoutEntry>, val memo: String? = null)
