package kr.sdbk.bodyplan.core.data.repository

import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.data.repository.fake.FakeExerciseDao
import kr.sdbk.bodyplan.core.data.repository.fake.FakeWorkoutEntryDao
import kr.sdbk.bodyplan.core.data.repository.fake.FakeWorkoutMemoDao
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.local.entity.ExerciseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutLogRepositoryImplTest {
    private val dao = FakeWorkoutEntryDao()
    private val memoDao = FakeWorkoutMemoDao()
    private val repository = WorkoutLogRepositoryImpl(dao, memoDao)
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

    @Test
    fun `메모를 저장하면 같은 날짜의 기록을 읽을 때 함께 나온다`() = runTest {
        repository.saveMemo(date, "어깨가 뻐근했다")

        val log = repository.observeLog(date).first()

        assertEquals("어깨가 뻐근했다", log.memo)
    }

    @Test
    fun `메모를 저장한 적 없는 날짜의 기록은 메모가 비어 있다`() = runTest {
        val log = repository.observeLog(date).first()

        assertNull(log.memo)
    }

    @Test
    fun `메모를 다시 저장하면 앞의 메모를 덮어쓴다`() = runTest {
        repository.saveMemo(date, "처음 메모")
        repository.saveMemo(date, "고친 메모")

        val log = repository.observeLog(date).first()

        assertEquals("고친 메모", log.memo)
        assertEquals(1, memoDao.stored.size)
    }

    @Test
    fun `메모를 공백만 남기고 저장하면 저장된 메모가 지워진다`() = runTest {
        repository.saveMemo(date, "지울 메모")

        repository.saveMemo(date, "   ")

        val log = repository.observeLog(date).first()
        assertNull(log.memo)
        assertTrue(memoDao.stored.isEmpty())
    }

    @Test
    fun `메모는 앞뒤 공백을 떼고 저장된다`() = runTest {
        repository.saveMemo(date, "  가벼운 날  ")

        assertEquals("가벼운 날", repository.observeLog(date).first().memo)
    }

    @Test
    fun `메모 저장이 실패하면 예외를 호출부로 던진다`() = runTest {
        memoDao.upsertFailure = IOException("저장 실패")

        var thrown: Throwable? = null
        try {
            repository.saveMemo(date, "메모")
        } catch (e: IOException) {
            thrown = e
        }

        assertTrue(thrown is IOException)
    }

    @Test
    fun `메모는 다른 날짜의 기록에 섞이지 않는다`() = runTest {
        repository.saveMemo(date, "이 날의 메모")

        assertNull(repository.observeLog(date.plusDays(1)).first().memo)
    }

    @Test
    fun `기간 조회는 날짜별로 묶어 낸다`() = runTest {
        repository.addEntry(date, exercise, listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))))
        repository.addEntry(
            date.plusDays(1),
            exercise,
            listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(30))),
        )

        val byDate = repository.observeEntriesInRange(date, date.plusDays(1)).first()

        assertEquals(setOf(date, date.plusDays(1)), byDate.keys)
        assertEquals(1, byDate.getValue(date).size)
        assertEquals(20, byDate.getValue(date).first().sets.first().intensity.value)
    }

    @Test
    fun `기간 조회는 범위 밖의 기록을 담지 않는다`() = runTest {
        repository.addEntry(date, exercise, listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))))

        val byDate = repository.observeEntriesInRange(date.plusDays(1), date.plusDays(7)).first()

        assertTrue(byDate.isEmpty())
    }

    @Test
    fun `기간 조회는 기록이 없는 날짜를 담지 않는다`() = runTest {
        repository.addEntry(date, exercise, listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))))

        val byDate = repository.observeEntriesInRange(date.minusDays(3), date).first()

        assertEquals(setOf(date), byDate.keys)
    }

    @Test
    fun `기간 조회의 세트는 저장한 순서를 지킨다`() = runTest {
        repository.addEntry(
            date,
            exercise,
            listOf(
                WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(10)),
                WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20)),
                WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(30)),
            ),
        )

        val sets = repository.observeEntriesInRange(date, date).first().getValue(date).first().sets

        assertEquals(listOf(10, 20, 30), sets.map { it.intensity.value })
    }

    @Test
    fun `addEntries는 목록 순서 그대로 저장한다`() = runTest {
        val entries = listOf(
            WorkoutEntry(
                id = 100L,
                exerciseId = exercise.id,
                exerciseName = "스쿼트",
                bodyPart = BodyPart.LEG,
                intensityType = IntensityType.WEIGHT,
                sets = listOf(WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40))),
            ),
            WorkoutEntry(
                id = 200L,
                exerciseId = 2L,
                exerciseName = "런지",
                bodyPart = BodyPart.LEG,
                intensityType = IntensityType.WEIGHT,
                sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))),
            ),
        )

        repository.addEntries(date, entries)

        val stored = repository.observeLog(date).first().entries
        assertEquals(listOf("스쿼트", "런지"), stored.map { it.exerciseName })
    }

    @Test
    fun `addEntries는 항목의 id를 무시하고 새 id로 저장한다`() = runTest {
        val entry = WorkoutEntry(
            id = 999L,
            exerciseId = exercise.id,
            exerciseName = "스쿼트",
            bodyPart = BodyPart.LEG,
            intensityType = IntensityType.WEIGHT,
            sets = listOf(WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40))),
        )

        repository.addEntries(date, listOf(entry))

        val stored = repository.observeLog(date).first().entries.single()
        assertTrue(stored.id != 999L)
    }

    @Test
    fun `addEntries에 빈 목록을 주면 아무 일도 하지 않는다`() = runTest {
        repository.addEntries(date, emptyList())

        val stored = repository.observeLog(date).first().entries
        assertTrue(stored.isEmpty())
    }

    @Test
    fun `observeBestBefore는 그 날짜 이전 기록에서 종목별 최고값을 낸다`() = runTest {
        repository.addEntry(
            date.minusDays(2),
            exercise,
            listOf(WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40))),
        )
        repository.addEntry(
            date.minusDays(1),
            exercise,
            listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60))),
        )

        val best = repository.observeBestBefore(date).first()

        assertEquals(60, best.single { it.exerciseId == exercise.id }.maxIntensityValue)
    }

    @Test
    fun `observeBestBefore는 조회 날짜 당일 기록은 포함하지 않는다`() = runTest {
        repository.addEntry(date, exercise, listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60))))

        val best = repository.observeBestBefore(date).first()

        assertTrue(best.none { it.exerciseId == exercise.id })
    }

    @Test
    fun `observeBestBefore는 기록이 없는 종목을 목록에 담지 않는다`() = runTest {
        val best = repository.observeBestBefore(date).first()

        assertTrue(best.isEmpty())
    }

    @Test
    fun `observeBestBefore는 종목의 축을 그대로 담아 낸다`() = runTest {
        val angleExercise = exercise.copy(id = 2L, intensityType = IntensityType.ANGLE)
        repository.addEntry(
            date.minusDays(1),
            angleExercise,
            listOf(WorkoutSet(repeatCount = 20, intensity = Intensity.Angle(30))),
        )

        val best = repository.observeBestBefore(date).first().single()

        assertEquals(IntensityType.ANGLE, best.intensityType)
        assertEquals(20, best.maxRepeatCount)
    }

    @Test
    fun `observeBestBefore는 같은 종목을 다른 축으로 잰 기록을 축별로 따로 낸다`() = runTest {
        // 무게로 재다가 각도로 바꾼 종목. 축을 바꾸기 전과 후의 최고값은 서로 섞이면 안 된다.
        repository.addEntry(
            date.minusDays(3),
            exercise,
            listOf(WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40))),
        )
        val angleExercise = exercise.copy(intensityType = IntensityType.ANGLE)
        repository.addEntry(
            date.minusDays(1),
            angleExercise,
            listOf(WorkoutSet(repeatCount = 20, intensity = Intensity.Angle(30))),
        )

        val best = repository.observeBestBefore(date).first()

        assertEquals(2, best.size)
        val weightRow = best.single { it.intensityType == IntensityType.WEIGHT }
        val angleRow = best.single { it.intensityType == IntensityType.ANGLE }
        assertEquals(40, weightRow.maxIntensityValue)
        assertEquals(20, angleRow.maxRepeatCount)
    }

    @Test
    fun `getLatestEntry는 그 종목의 가장 최근 기록의 세트를 낸다`() = runTest {
        repository.addEntry(
            date.minusDays(2),
            exercise,
            listOf(WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40))),
        )
        repository.addEntry(
            date.minusDays(1),
            exercise,
            listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60))),
        )

        val latest = repository.getLatestEntry(exercise.id, date)

        assertEquals(listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60))), latest?.sets)
    }

    @Test
    fun `getLatestEntry는 기록이 없으면 null이다`() = runTest {
        val latest = repository.getLatestEntry(exercise.id, date)

        assertNull(latest)
    }
}
