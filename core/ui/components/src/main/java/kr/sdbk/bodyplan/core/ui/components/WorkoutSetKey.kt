package kr.sdbk.bodyplan.core.ui.components

/** 세션 중 체크한 세트를 가리키는 키. 세트에는 id가 없어 기록 id와 번호로 가리킨다. 저장하지 않으므로 도메인 모델이 아니다. */
data class WorkoutSetKey(val entryId: Long, val setIndex: Int)
