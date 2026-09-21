package kr.sdbk.bodyplan.core.domain.model

/** 세션 중 체크한 세트를 가리키는 키. 세트에는 id가 없어 기록 id와 번호로 가리킨다. */
data class WorkoutSetKey(val entryId: Long, val setIndex: Int)

data class RestTimer(val remainingSeconds: Int)

/**
 * 지금 하고 있는 운동. 저장하지 않는다 — 앱이 죽으면 세션도 끝난다.
 *
 * 시작 시각은 들지 않는다. 화면과 알림이 시계를 읽지 않게 하려는 것이고, 일시정지가 있어
 * 시작 시각만으로는 경과를 셀 수도 없다. 경과는 세션을 굴리는 쪽이 매초 채운다.
 */
data class WorkoutSession(
    val elapsedSeconds: Long,
    val isPaused: Boolean,
    val completedSets: Set<WorkoutSetKey>,
    val rest: RestTimer?,
)

val WorkoutSession.elapsedText: String get() = clockText(elapsedSeconds)

val RestTimer.text: String get() = clockText(remainingSeconds.toLong())

/** 초를 `12:34`로, 한 시간을 넘으면 `1:02:34`로 적는다. */
fun clockText(totalSeconds: Long): String {
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
