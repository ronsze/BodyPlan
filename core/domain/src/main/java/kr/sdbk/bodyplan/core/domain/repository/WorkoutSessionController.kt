package kr.sdbk.bodyplan.core.domain.repository

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kr.sdbk.bodyplan.core.domain.model.WorkoutSession
import kr.sdbk.bodyplan.core.domain.model.WorkoutSetKey

/**
 * 지금 하고 있는 운동 세션을 화면 밖에서 굴린다.
 *
 * 화면에 두면 나가는 순간 사라지는데, 알림은 세션이 남아 있다고 보여 준다. 화면과 알림이
 * 같은 것을 보도록 한 곳에 둔다. 세션이 없을 때의 호출은 아무 일도 하지 않는다.
 */
interface WorkoutSessionController {
    /** 세션이 없으면 `null`. 세션 중에는 매초 새 값이 나온다. */
    val session: StateFlow<WorkoutSession?>

    /** 휴식이 0에 닿을 때 한 번. 건너뛰기·`-30초`로 끝낼 때는 나가지 않는다 — 사용자가 스스로 끝낸 것이다. */
    val restEnded: SharedFlow<Unit>

    /** 세션 중이면 무시한다. */
    fun start()

    /** 경과 시간과 휴식이 함께 멈춘다. 이미 멈춰 있으면 무시한다. */
    fun pause()

    fun resume()

    fun end()

    /** 켤 때만 휴식을 새로 시작한다. 이미 쉬는 중이어도 처음부터다 — 방금 한 세트 뒤의 휴식이다. */
    fun toggleSet(key: WorkoutSetKey)

    /** 줄여서 0 이하가 되면 바로 끝낸다. */
    fun adjustRest(deltaSeconds: Int)

    fun skipRest()
}
