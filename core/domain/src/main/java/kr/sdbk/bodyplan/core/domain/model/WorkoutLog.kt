package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate

/** 하루치 기록. */
data class WorkoutLog(val date: LocalDate, val entries: List<WorkoutEntry>)
