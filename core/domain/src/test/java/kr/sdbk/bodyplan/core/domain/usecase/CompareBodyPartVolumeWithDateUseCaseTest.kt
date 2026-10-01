package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.ExerciseBest
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutLog
import kr.sdbk.bodyplan.core.domain.model.WorkoutMemo
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareBodyPartVolumeWithDateUseCaseTest {
    private val comparedDate = LocalDate.of(2026, 9, 1)

    private fun entry(bodyPart: BodyPart, weightKg: Int = 20, repeatCount: Int = 10): WorkoutEntry = WorkoutEntry(
        id = 1L,
        exerciseId = 1L,
        exerciseName = "종목",
        bodyPart = bodyPart,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = repeatCount, intensity = Intensity.Weight(weightKg))),
    )

    private fun nonWeightEntry(bodyPart: BodyPart): WorkoutEntry = WorkoutEntry(
        id = 2L,
        exerciseId = 2L,
        exerciseName = "각도",
        bodyPart = bodyPart,
        intensityType = IntensityType.ANGLE,
        sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Angle(30))),
    )

    private fun useCaseWith(
        entriesByDate: Map<LocalDate, List<WorkoutEntry>>,
        onObserve: (from: LocalDate, to: LocalDate) -> Unit = { _, _ -> },
    ) = CompareBodyPartVolumeWithDateUseCase(
        workoutLogRepository = FakeWorkoutLogRepository(entriesByDate, onObserve),
        summarizeBodyPartVolume = SummarizeBodyPartVolumeUseCase(),
    )

    @Test
    fun `지정일의 같은 부위 볼륨과 증감이 실린다`() = runTest {
        val useCase = useCaseWith(mapOf(comparedDate to listOf(entry(BodyPart.CHEST, weightKg = 20))))

        val result = useCase(listOf(entry(BodyPart.CHEST, weightKg = 30)), comparedDate).single()

        assertEquals(BodyPart.CHEST, result.bodyPart)
        assertEquals(300, result.volumeKg)
        assertEquals(200, result.comparedVolumeKg)
        assertEquals(100, result.changeKg)
    }

    @Test
    fun `지정일에 없는 부위는 견준 볼륨과 증감이 null이다`() = runTest {
        val useCase = useCaseWith(mapOf(comparedDate to listOf(entry(BodyPart.BACK))))

        val result = useCase(listOf(entry(BodyPart.CHEST)), comparedDate).single()

        assertNull(result.comparedVolumeKg)
        assertNull(result.changeKg)
    }

    @Test
    fun `오늘 기록에 무게 볼륨이 없으면 빈 목록이고 저장소를 조회하지 않는다`() = runTest {
        var observed = false
        val useCase = useCaseWith(emptyMap()) { _, _ -> observed = true }

        val result = useCase(listOf(nonWeightEntry(BodyPart.CHEST)), comparedDate)

        assertTrue(result.isEmpty())
        assertFalse(observed)
    }

    @Test
    fun `볼륨이 줄면 증감이 음수다`() = runTest {
        val useCase = useCaseWith(mapOf(comparedDate to listOf(entry(BodyPart.CHEST, weightKg = 30))))

        val result = useCase(listOf(entry(BodyPart.CHEST, weightKg = 20)), comparedDate).single()

        assertEquals(-100, result.changeKg)
    }

    @Test
    fun `지정일에 무게가 아닌 종목만 있으면 그 부위는 null이다`() = runTest {
        val useCase = useCaseWith(mapOf(comparedDate to listOf(nonWeightEntry(BodyPart.CHEST))))

        val result = useCase(listOf(entry(BodyPart.CHEST)), comparedDate).single()

        assertNull(result.comparedVolumeKg)
        assertNull(result.changeKg)
    }

    @Test
    fun `지정일 하루만 조회한다`() = runTest {
        var range: Pair<LocalDate, LocalDate>? = null
        val useCase = useCaseWith(emptyMap()) { from, to -> range = from to to }

        useCase(listOf(entry(BodyPart.CHEST)), comparedDate)

        assertEquals(comparedDate to comparedDate, range)
    }

    private class FakeWorkoutLogRepository(
        private val entriesByDate: Map<LocalDate, List<WorkoutEntry>>,
        private val onObserve: (from: LocalDate, to: LocalDate) -> Unit = { _, _ -> },
    ) : WorkoutLogRepository {
        override fun observeLog(date: LocalDate): Flow<WorkoutLog> = error("사용하지 않음")

        override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
            error("사용하지 않음")

        override fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>> {
            onObserve(from, to)
            return flowOf(entriesByDate.filterKeys { it in from..to })
        }

        override suspend fun getEntry(id: Long): WorkoutEntry? = error("사용하지 않음")

        override fun observeBestBefore(date: LocalDate): Flow<List<ExerciseBest>> = error("사용하지 않음")

        override suspend fun getLatestEntry(exerciseId: Long, until: LocalDate): WorkoutEntry? = error("사용하지 않음")

        override suspend fun addEntry(date: LocalDate, exercise: Exercise, sets: List<WorkoutSet>): Long =
            error("사용하지 않음")

        override suspend fun addEntries(date: LocalDate, entries: List<WorkoutEntry>) {
            error("사용하지 않음")
        }

        override suspend fun updateEntry(entryId: Long, exercise: Exercise, sets: List<WorkoutSet>) {
            error("사용하지 않음")
        }

        override suspend fun deleteEntry(id: Long) {
            error("사용하지 않음")
        }

        override suspend fun saveMemo(date: LocalDate, memo: WorkoutMemo) {
            error("사용하지 않음")
        }
    }
}
