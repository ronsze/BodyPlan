package kr.sdbk.bodyplan.core.data.repository

import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.data.repository.fake.FakeExerciseDao
import kr.sdbk.bodyplan.core.data.repository.fake.FakeWorkoutEntryDao
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.local.entity.ExerciseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutLogRepositoryImplTest {
    private val dao = FakeWorkoutEntryDao()
    private val repository = WorkoutLogRepositoryImpl(dao)
    private val date = LocalDate.of(2026, 9, 7)
    private val exercise = Exercise(
        id = 1L,
        bodyPart = BodyPart.LEG,
        name = "스쿼트",
        intensityType = IntensityType.WEIGHT,
    )

    @Test
    fun `세트별 무게와 횟수가 다른 기록을 저장하고 그대로 다시 읽을 수 있다`() = runTest {
        val sets = listOf(
            WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40)),
            WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60)),
        )

        val id = repository.addEntry(date, exercise, sets)
        val stored = repository.getEntry(id)!!

        assertEquals(sets, stored.sets)
    }

    @Test
    fun `기록을 읽을 때 세트가 저장한 순서대로 나온다`() = runTest {
        val sets = listOf(
            WorkoutSet(repeatCount = 20, intensity = Intensity.Weight(5)),
            WorkoutSet(repeatCount = 16, intensity = Intensity.Weight(10)),
            WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(15)),
        )

        val id = repository.addEntry(date, exercise, sets)
        val stored = repository.getEntry(id)!!

        assertEquals(sets, stored.sets)
    }

    @Test
    fun `기록을 수정해 세트 수를 줄이면 사라진 세트가 저장소에서도 지워진다`() = runTest {
        val id = repository.addEntry(
            date,
            exercise,
            listOf(
                WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40)),
                WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60)),
                WorkoutSet(repeatCount = 6, intensity = Intensity.Weight(80)),
            ),
        )

        val reduced = listOf(WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40)))
        repository.updateEntry(id, exercise, reduced)

        val stored = repository.getEntry(id)!!
        assertEquals(reduced, stored.sets)
        assertEquals(1, dao.setsFor(id).size)
    }

    @Test
    fun `종목을 삭제한 뒤에도 과거 기록의 종목명과 부위가 그대로 조회된다`() = runTest {
        val exerciseDao = FakeExerciseDao()
        val exerciseId = exerciseDao.seed(
            ExerciseEntity(bodyPart = BodyPart.LEG.name, name = "스쿼트", intensityType = IntensityType.WEIGHT.name),
        )
        val exerciseRepository = ExerciseRepositoryImpl(exerciseDao)
        val storedExercise = exercise.copy(id = exerciseId)
        val id = repository.addEntry(date, storedExercise, listOf(WorkoutSet(12, Intensity.Weight(40))))

        exerciseRepository.deleteExercise(exerciseId)

        val stored = repository.getEntry(id)!!
        assertEquals("스쿼트", stored.exerciseName)
        assertEquals(BodyPart.LEG, stored.bodyPart)
    }

    @Test
    fun `종목 이름을 바꿔도 이미 저장된 기록의 종목명은 바뀌지 않는다`() = runTest {
        val exerciseDao = FakeExerciseDao()
        val exerciseId = exerciseDao.seed(
            ExerciseEntity(bodyPart = BodyPart.LEG.name, name = "스쿼트", intensityType = IntensityType.WEIGHT.name),
        )
        val exerciseRepository = ExerciseRepositoryImpl(exerciseDao)
        val storedExercise = exercise.copy(id = exerciseId)
        val id = repository.addEntry(date, storedExercise, listOf(WorkoutSet(12, Intensity.Weight(40))))

        exerciseRepository.updateExercise(storedExercise.copy(name = "핵스쿼트"))

        val stored = repository.getEntry(id)!!
        assertEquals("스쿼트", stored.exerciseName)
    }

    @Test
    fun `observeBodyPartsInRange가 기록이 없는 날짜를 결과 맵에서 뺀다`() = runTest {
        repository.addEntry(date, exercise, listOf(WorkoutSet(12, Intensity.Weight(40))))

        val result = repository.observeBodyPartsInRange(date.minusDays(3), date.plusDays(3)).first()

        assertEquals(setOf(date), result.keys)
        assertFalse(result.containsKey(date.minusDays(1)))
    }

    @Test
    fun `저장 실패는 예외로 전파된다`() = runTest {
        dao.insertFailure = IOException("insert failed")

        var thrown = false
        try {
            repository.addEntry(date, exercise, listOf(WorkoutSet(12, Intensity.Weight(40))))
        } catch (e: IOException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun `삭제 실패는 예외로 전파된다`() = runTest {
        val id = repository.addEntry(date, exercise, listOf(WorkoutSet(12, Intensity.Weight(40))))
        dao.deleteFailure = IOException("delete failed")

        var thrown = false
        try {
            repository.deleteEntry(id)
        } catch (e: IOException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
