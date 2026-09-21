package kr.sdbk.bodyplan.feature.workoutlog.impl.fake

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kr.sdbk.bodyplan.core.domain.model.WorkoutSession
import kr.sdbk.bodyplan.core.domain.model.WorkoutSetKey
import kr.sdbk.bodyplan.core.domain.repository.WorkoutSessionController

/**
 * 세션의 시간·휴식 규칙은 [kr.sdbk.bodyplan.core.data.session.WorkoutSessionEngine]에서 검증한다.
 * 여기서는 ViewModel이 호출을 위임하고 세션 값을 그대로 비추는지만 본다 — [session]은 테스트가
 * 직접 밀어 넣는다.
 */
internal class FakeWorkoutSessionController : WorkoutSessionController {
    override val session = MutableStateFlow<WorkoutSession?>(null)

    private val restEndedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val restEnded: SharedFlow<Unit> get() = restEndedEvents

    var startCallCount = 0
        private set
    var pauseCallCount = 0
        private set
    var resumeCallCount = 0
        private set
    var endCallCount = 0
        private set
    var skipRestCallCount = 0
        private set
    val toggledKeys = mutableListOf<WorkoutSetKey>()
    val adjustRestDeltas = mutableListOf<Int>()

    override fun start() {
        startCallCount++
        // 실제 엔진처럼 빈 세션으로 바꿔 둬야 "세션 중 재시작 무시"를 화면 쪽에서 그대로 검증할 수 있다.
        session.value = WorkoutSession(elapsedSeconds = 0L, isPaused = false, completedSets = emptySet(), rest = null)
    }

    override fun pause() {
        pauseCallCount++
    }

    override fun resume() {
        resumeCallCount++
    }

    override fun end() {
        endCallCount++
        session.value = null
    }

    override fun toggleSet(key: WorkoutSetKey) {
        toggledKeys += key
    }

    override fun adjustRest(deltaSeconds: Int) {
        adjustRestDeltas += deltaSeconds
    }

    override fun skipRest() {
        skipRestCallCount++
    }
}
