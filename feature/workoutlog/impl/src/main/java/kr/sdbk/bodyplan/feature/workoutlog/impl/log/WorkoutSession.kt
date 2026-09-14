package kr.sdbk.bodyplan.feature.workoutlog.impl.log

import kr.sdbk.bodyplan.core.ui.components.WorkoutSetKey

internal data class RestTimer(val remainingSeconds: Int)

/**
 * 지금 하고 있는 운동. 저장하지 않는다 — 앱이 죽으면 세션도 끝난다.
 *
 * [elapsedSeconds]를 시작 시각에서 매번 셈하지 않고 드는 것은 화면이 시계를 읽지 않게 하기 위해서다.
 */
internal data class WorkoutSession(
    val startedAtMillis: Long,
    val elapsedSeconds: Long = 0L,
    val completedSets: Set<WorkoutSetKey> = emptySet(),
    val rest: RestTimer? = null,
)
