package kr.sdbk.bodyplan.core.domain.model

/** 세트 하나. 같은 기록 안에서도 세트마다 강도와 횟수가 다를 수 있다. */
data class WorkoutSet(val repeatCount: Int, val intensity: Intensity)
