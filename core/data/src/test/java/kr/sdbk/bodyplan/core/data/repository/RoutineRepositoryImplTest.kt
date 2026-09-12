package kr.sdbk.bodyplan.core.data.repository

import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.data.repository.fake.FakeRoutineDao
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineRepositoryImplTest {
    private val dao = FakeRoutineDao()
    private val repository = RoutineRepositoryImpl(dao)
    private val exercise = Exercise(
        id = 1L,
        bodyPart = BodyPart.LEG,
        name = "스쿼트",
        intensityType = IntensityType.WEIGHT,
    )

    @Test
    fun `루틴을 만들면 부위와 이름 그대로 조회된다`() = runTest {
        val id = repository.addRoutine("하체 루틴", BodyPart.LEG)

        val stored = repository.getRoutine(id)!!

        assertEquals("하체 루틴", stored.name)
        assertEquals(BodyPart.LEG, stored.bodyPart)
        assertTrue(stored.entries.isEmpty())
    }

    @Test
    fun `여러 루틴을 만든 순서대로 조회된다`() = runTest {
        val firstId = repository.addRoutine("루틴A", BodyPart.CHEST)
        val secondId = repository.addRoutine("루틴B", BodyPart.BACK)

        val routines = repository.observeRoutines().first()

        assertEquals(listOf(firstId, secondId), routines.map { it.id })
    }

    @Test
    fun `루틴에 항목을 더하면 세트와 함께 조회된다`() = runTest {
        val routineId = repository.addRoutine("하체 루틴", BodyPart.LEG)
        val sets = listOf(
            WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40)),
            WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60)),
        )

        repository.addEntry(routineId, exercise, sets)

        val stored = repository.getRoutine(routineId)!!
        assertEquals(1, stored.entries.size)
        assertEquals(sets, stored.entries.single().sets)
    }

    @Test
    fun `항목을 수정하면 종목 스냅샷만 바뀐다`() = runTest {
        val routineId = repository.addRoutine("하체 루틴", BodyPart.LEG)
        val entryId = repository.addEntry(
            routineId,
            exercise,
            listOf(WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40))),
        )
        val other = Exercise(id = 2L, bodyPart = BodyPart.LEG, name = "런지", intensityType = IntensityType.WEIGHT)
        val reduced = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(30)))

        repository.updateEntry(entryId, other, reduced)

        val stored = repository.getEntry(entryId)!!
        assertEquals("런지", stored.exerciseName)
        assertEquals(reduced, stored.sets)
    }

    @Test
    fun `없는 항목을 수정하면 예외를 던진다`() = runTest {
        var thrown: Throwable? = null
        try {
            repository.updateEntry(999L, exercise, listOf(WorkoutSet(12, Intensity.Weight(40))))
        } catch (e: IllegalArgumentException) {
            thrown = e
        }
        assertTrue(thrown is IllegalArgumentException)
    }

    @Test
    fun `항목을 지우면 조회되지 않는다`() = runTest {
        val routineId = repository.addRoutine("하체 루틴", BodyPart.LEG)
        val entryId = repository.addEntry(routineId, exercise, listOf(WorkoutSet(12, Intensity.Weight(40))))

        repository.deleteEntry(entryId)

        assertNull(repository.getEntry(entryId))
    }

    @Test
    fun `루틴을 지우면 조회되지 않는다`() = runTest {
        val routineId = repository.addRoutine("하체 루틴", BodyPart.LEG)

        repository.deleteRoutine(routineId)

        assertNull(repository.getRoutine(routineId))
    }

    @Test
    fun `루틴 생성 실패는 예외로 전파된다`() = runTest {
        dao.insertFailure = IOException("insert failed")

        var thrown = false
        try {
            repository.addRoutine("하체 루틴", BodyPart.LEG)
        } catch (e: IOException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun `루틴 삭제 실패는 예외로 전파된다`() = runTest {
        val routineId = repository.addRoutine("하체 루틴", BodyPart.LEG)
        dao.deleteFailure = IOException("delete failed")

        var thrown = false
        try {
            repository.deleteRoutine(routineId)
        } catch (e: IOException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
