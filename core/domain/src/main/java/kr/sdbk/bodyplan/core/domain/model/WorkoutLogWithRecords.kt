package kr.sdbk.bodyplan.core.domain.model

/** 하루치 기록과 그날 최고값을 갱신한 종목. PR은 저장하지 않고 볼 때마다 판정하므로 [WorkoutLog]에 넣지 않는다. */
data class WorkoutLogWithRecords(val log: WorkoutLog, val personalRecordExerciseIds: Set<Long>)
