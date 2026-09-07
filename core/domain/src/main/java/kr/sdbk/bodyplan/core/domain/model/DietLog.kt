package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate

/** 하루치 식단 기록. 추가한 순서대로 담긴다. */
data class DietLog(val date: LocalDate, val entries: List<DietEntry>)
