package kr.sdbk.bodyplan.core.data.session

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.WorkoutSetKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 코루틴 가상 시간(`advanceTimeBy`)과 별개로 흐르는 시각. 틱마다 같이 움직여야 경과가 맞는다. */
private class MutableClock(private var current: Instant, private val zoneId: ZoneId = ZoneOffset.UTC) : Clock() {
    override fun getZone(): ZoneId = zoneId
    override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
    override fun instant(): Instant = current
    fun advanceSeconds(seconds: Long) {
        current = current.plusSeconds(seconds)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class WorkoutSessionEngineTest {
    private val clock = MutableClock(Instant.EPOCH)

    /** 틱 하나(시계 1초 + 가상 시간 1초)를 진행한다. */
    private fun TestScope.tickEngine() {
        clock.advanceSeconds(1)
        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()
    }

    @Test
    fun `start 뒤 세션은 빈 상태이고 시계가 갈수록 경과가 는다`() = runTest {
        val engine = WorkoutSessionEngine(clock, backgroundScope)

        engine.start()
        runCurrent()

        val session = engine.session.value
        assertEquals(0L, session?.elapsedSeconds)
        assertEquals(false, session?.isPaused)
        assertTrue(session?.completedSets.orEmpty().isEmpty())
        assertNull(session?.rest)

        tickEngine()
        assertEquals(1L, engine.session.value?.elapsedSeconds)
        tickEngine()
        assertEquals(2L, engine.session.value?.elapsedSeconds)
    }

    @Test
    fun `pause 중에는 시계가 가도 경과와 휴식이 그대로다가 resume하면 다시 흐른다`() = runTest {
        val engine = WorkoutSessionEngine(clock, backgroundScope)
        engine.start()
        engine.toggleSet(WorkoutSetKey(entryId = 1L, setIndex = 0))
        runCurrent()
        tickEngine()
        tickEngine()

        val elapsedBeforePause = engine.session.value?.elapsedSeconds
        val restBeforePause = engine.session.value?.rest?.remainingSeconds

        engine.pause()
        assertEquals(true, engine.session.value?.isPaused)

        tickEngine()
        tickEngine()
        assertEquals(elapsedBeforePause, engine.session.value?.elapsedSeconds)
        assertEquals(restBeforePause, engine.session.value?.rest?.remainingSeconds)

        engine.resume()
        assertEquals(false, engine.session.value?.isPaused)
        tickEngine()

        assertEquals(elapsedBeforePause?.plus(1), engine.session.value?.elapsedSeconds)
        assertEquals(restBeforePause?.minus(1), engine.session.value?.rest?.remainingSeconds)
    }

    @Test
    fun `세션이 없으면 pause resume toggleSet adjustRest skipRest는 아무 일도 하지 않는다`() = runTest {
        val engine = WorkoutSessionEngine(clock, backgroundScope)

        engine.pause()
        engine.resume()
        engine.toggleSet(WorkoutSetKey(entryId = 1L, setIndex = 0))
        engine.adjustRest(30)
        engine.skipRest()

        assertNull(engine.session.value)
    }

    @Test
    fun `세션 중 start는 무시된다`() = runTest {
        val engine = WorkoutSessionEngine(clock, backgroundScope)
        engine.start()
        runCurrent()
        tickEngine()
        val session = engine.session.value

        engine.start()
        runCurrent()

        assertEquals(session, engine.session.value)
    }

    @Test
    fun `일시정지가 아닐 때 resume은 무시되고 일시정지 중 pause도 무시된다`() = runTest {
        val engine = WorkoutSessionEngine(clock, backgroundScope)
        engine.start()
        runCurrent()

        engine.resume()
        assertEquals(false, engine.session.value?.isPaused)

        engine.pause()
        tickEngine()
        val elapsedWhilePaused = engine.session.value?.elapsedSeconds

        engine.pause()
        tickEngine()

        assertEquals(elapsedWhilePaused, engine.session.value?.elapsedSeconds)
    }

    @Test
    fun `toggleSet은 켤 때 완료 집합에 담고 휴식을 90초로 시작하며 끌 때는 타이머를 그대로 둔다`() = runTest {
        val engine = WorkoutSessionEngine(clock, backgroundScope)
        val key = WorkoutSetKey(entryId = 1L, setIndex = 0)
        engine.start()
        runCurrent()

        engine.toggleSet(key)
        runCurrent()
        assertEquals(setOf(key), engine.session.value?.completedSets)
        assertEquals(90, engine.session.value?.rest?.remainingSeconds)

        tickEngine()
        engine.toggleSet(key)
        runCurrent()

        assertTrue(engine.session.value?.completedSets.orEmpty().isEmpty())
        assertEquals(89, engine.session.value?.rest?.remainingSeconds)
    }

    @Test
    fun `휴식 중 다른 세트를 켜면 90초로 다시 시작한다`() = runTest {
        val engine = WorkoutSessionEngine(clock, backgroundScope)
        val first = WorkoutSetKey(entryId = 1L, setIndex = 0)
        val second = WorkoutSetKey(entryId = 1L, setIndex = 1)
        engine.start()
        engine.toggleSet(first)
        runCurrent()
        tickEngine()
        tickEngine()

        engine.toggleSet(second)
        runCurrent()

        assertEquals(90, engine.session.value?.rest?.remainingSeconds)
    }

    @Test
    fun `휴식은 매초 줄고 0에 닿으면 사라지며 restEnded가 한 번 나간다`() = runTest {
        val engine = WorkoutSessionEngine(clock, backgroundScope)
        var restEndedCount = 0
        backgroundScope.launch { engine.restEnded.collect { restEndedCount++ } }

        engine.start()
        engine.toggleSet(WorkoutSetKey(entryId = 1L, setIndex = 0))
        runCurrent()

        repeat(89) { tickEngine() }
        assertEquals(1, engine.session.value?.rest?.remainingSeconds)
        assertEquals(0, restEndedCount)

        tickEngine()
        assertNull(engine.session.value?.rest)
        assertEquals(1, restEndedCount)
    }

    @Test
    fun `adjustRest로 0 이하가 되면 즉시 사라지고 restEnded는 나가지 않는다`() = runTest {
        val engine = WorkoutSessionEngine(clock, backgroundScope)
        var restEndedCount = 0
        backgroundScope.launch { engine.restEnded.collect { restEndedCount++ } }

        engine.start()
        engine.toggleSet(WorkoutSetKey(entryId = 1L, setIndex = 0))
        runCurrent()

        engine.adjustRest(-30)
        engine.adjustRest(-30)
        engine.adjustRest(-30)
        runCurrent()

        assertNull(engine.session.value?.rest)
        assertEquals(0, restEndedCount)
    }

    @Test
    fun `adjustRest 더하기는 제한 없이 누적된다`() = runTest {
        val engine = WorkoutSessionEngine(clock, backgroundScope)
        engine.start()
        engine.toggleSet(WorkoutSetKey(entryId = 1L, setIndex = 0))
        runCurrent()

        engine.adjustRest(30)
        engine.adjustRest(30)
        runCurrent()

        assertEquals(150, engine.session.value?.rest?.remainingSeconds)
    }

    @Test
    fun `skipRest는 restEnded 없이 휴식을 끝낸다`() = runTest {
        val engine = WorkoutSessionEngine(clock, backgroundScope)
        var restEndedCount = 0
        backgroundScope.launch { engine.restEnded.collect { restEndedCount++ } }

        engine.start()
        engine.toggleSet(WorkoutSetKey(entryId = 1L, setIndex = 0))
        runCurrent()

        engine.skipRest()
        runCurrent()

        assertNull(engine.session.value?.rest)
        assertEquals(0, restEndedCount)
    }

    @Test
    fun `end 뒤에는 세션이 없고 시계가 가도 바뀌지 않는다`() = runTest {
        val engine = WorkoutSessionEngine(clock, backgroundScope)
        engine.start()
        runCurrent()
        tickEngine()

        engine.end()
        assertNull(engine.session.value)

        tickEngine()
        assertNull(engine.session.value)
    }
}
