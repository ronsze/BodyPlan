package kr.sdbk.bodyplan.core.data.session

import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.RestTimer
import kr.sdbk.bodyplan.core.domain.model.WorkoutSession
import kr.sdbk.bodyplan.core.domain.model.WorkoutSetKey
import kr.sdbk.bodyplan.core.domain.repository.WorkoutSessionController

/**
 * 세션 상태 기계. Android를 모른다 — 서비스 시작은 [WorkoutSessionControllerImpl]이 덧붙인다.
 *
 * 경과는 시작 시각에서 빼지 않고 누적한다. 일시정지 동안 흐른 시간은 경과가 아니다.
 * 휴식만 초 단위로 줄인다 — 경과는 시계로 다시 세지만 휴식은 사용자가 더하고 뺀 값이라
 * 시계로 되돌릴 기준이 없다.
 *
 * 틱은 [scope]에서, 나머지 호출은 화면·알림에서 오므로 스레드가 섞인다. 상태를 바꾸는 함수는 전부 잠근다.
 */
internal class WorkoutSessionEngine(private val clock: Clock, private val scope: CoroutineScope) :
    WorkoutSessionController {
    private val current = MutableStateFlow<WorkoutSession?>(null)
    private val restEndedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var tickJob: Job? = null

    /** 일시정지 전까지 쌓인 경과. */
    private var accumulatedMillis = 0L

    /** 마지막으로 재개(또는 시작)한 시각. 일시정지 중이면 `null`. */
    private var resumedAtMillis: Long? = null

    override val session: StateFlow<WorkoutSession?> = current.asStateFlow()

    override val restEnded: SharedFlow<Unit> = restEndedEvents.asSharedFlow()

    @Synchronized
    override fun start() {
        if (current.value != null) return
        accumulatedMillis = 0L
        resumedAtMillis = clock.millis()
        current.value = WorkoutSession(elapsedSeconds = 0L, isPaused = false, completedSets = emptySet(), rest = null)
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(TICK_MILLIS)
                tick()
            }
        }
    }

    @Synchronized
    override fun pause() {
        val session = current.value ?: return
        val resumedAt = resumedAtMillis ?: return
        accumulatedMillis += clock.millis() - resumedAt
        resumedAtMillis = null
        current.value = session.copy(isPaused = true)
    }

    @Synchronized
    override fun resume() {
        val session = current.value ?: return
        if (resumedAtMillis != null) return
        resumedAtMillis = clock.millis()
        current.value = session.copy(isPaused = false)
    }

    @Synchronized
    override fun end() {
        tickJob?.cancel()
        tickJob = null
        resumedAtMillis = null
        current.value = null
    }

    @Synchronized
    override fun toggleSet(key: WorkoutSetKey) {
        val session = current.value ?: return
        val completing = key !in session.completedSets
        current.value = session.copy(
            completedSets = if (completing) session.completedSets + key else session.completedSets - key,
            rest = if (completing) RestTimer(REST_SECONDS) else session.rest,
        )
    }

    /** 줄여서 0 이하가 되면 바로 끝낸다. 진동은 없다 — 사용자가 스스로 끝낸 것이다. */
    @Synchronized
    override fun adjustRest(deltaSeconds: Int) {
        val session = current.value ?: return
        val rest = session.rest ?: return
        val remaining = rest.remainingSeconds + deltaSeconds
        current.value = session.copy(rest = if (remaining <= 0) null else RestTimer(remaining))
    }

    @Synchronized
    override fun skipRest() {
        val session = current.value ?: return
        current.value = session.copy(rest = null)
    }

    @Synchronized
    private fun tick() {
        val session = current.value ?: return
        val resumedAt = resumedAtMillis ?: return
        val elapsed = (accumulatedMillis + (clock.millis() - resumedAt)) / MILLIS_PER_SECOND
        val remaining = session.rest?.remainingSeconds?.minus(1)
        val restEndedNow = remaining != null && remaining <= 0
        current.value = session.copy(
            elapsedSeconds = elapsed,
            rest = if (remaining == null || restEndedNow) null else RestTimer(remaining),
        )
        if (restEndedNow) restEndedEvents.tryEmit(Unit)
    }

    private companion object {
        const val REST_SECONDS = 90
        const val TICK_MILLIS = 1_000L
        const val MILLIS_PER_SECOND = 1_000L
    }
}
