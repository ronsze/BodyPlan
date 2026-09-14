package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.ExerciseBest
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutLog
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsPersonalRecordUseCaseTest {
    private val date = LocalDate.of(2026, 9, 7)

    @Test
    fun `무게 종목에서 세트 중 최고 무게가 이전 최고 무게를 넘기면 PR이다`() = runTest {
        val repository = FakeWorkoutLogRepository(
            listOf(
                ExerciseBest(
                    exerciseId = 1L,
                    intensityType = IntensityType.WEIGHT,
                    maxIntensityValue = 50,
                    maxRepeatCount = 0,
                ),
            ),
        )
        val useCase = IsPersonalRecordUseCase(repository)

        val result = useCase(
            date,
            1L,
            IntensityType.WEIGHT,
            listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60))),
        )

        assertTrue(result)
    }

    @Test
    fun `이전 최고값과 같으면 PR이 아니다`() = runTest {
        val repository = FakeWorkoutLogRepository(
            listOf(
                ExerciseBest(
                    exerciseId = 1L,
                    intensityType = IntensityType.WEIGHT,
                    maxIntensityValue = 50,
                    maxRepeatCount = 0,
                ),
            ),
        )
        val useCase = IsPersonalRecordUseCase(repository)

        val result = useCase(
            date,
            1L,
            IntensityType.WEIGHT,
            listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(50))),
        )

        assertFalse(result)
    }

    @Test
    fun `이전 기록이 없는 종목은 PR이 아니다`() = runTest {
        val repository = FakeWorkoutLogRepository(emptyList())
        val useCase = IsPersonalRecordUseCase(repository)

        val result = useCase(
            date,
            1L,
            IntensityType.WEIGHT,
            listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(100))),
        )

        assertFalse(result)
    }

    @Test
    fun `이전 기록이 다른 축뿐이면 PR이 아니다`() = runTest {
        val repository = FakeWorkoutLogRepository(
            listOf(
                ExerciseBest(
                    exerciseId = 1L,
                    intensityType = IntensityType.ANGLE,
                    maxIntensityValue = 0,
                    maxRepeatCount = 5,
                ),
            ),
        )
        val useCase = IsPersonalRecordUseCase(repository)

        val result = useCase(
            date,
            1L,
            IntensityType.WEIGHT,
            listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(100))),
        )

        assertFalse(result)
    }

    @Test
    fun `각도 종목은 횟수로 견준다`() = runTest {
        val repository = FakeWorkoutLogRepository(
            listOf(
                ExerciseBest(
                    exerciseId = 1L,
                    intensityType = IntensityType.ANGLE,
                    maxIntensityValue = 999,
                    maxRepeatCount = 15,
                ),
            ),
        )
        val useCase = IsPersonalRecordUseCase(repository)

        val result = useCase(
            date,
            1L,
            IntensityType.ANGLE,
            listOf(WorkoutSet(repeatCount = 20, intensity = Intensity.Angle(10))),
        )

        assertTrue(result)
    }

    private class FakeWorkoutLogRepository(private val bestBefore: List<ExerciseBest>) : WorkoutLogRepository {
        override fun observeLog(date: LocalDate): Flow<WorkoutLog> = error("사용하지 않음")

        override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
            error("사용하지 않음")

        override fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>> =
            error("사용하지 않음")

        override suspend fun getEntry(id: Long): WorkoutEntry? = error("사용하지 않음")

        override fun observeBestBefore(date: LocalDate): Flow<List<ExerciseBest>> = MutableStateFlow(bestBefore)

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

        override suspend fun saveMemo(date: LocalDate, text: String) {
            error("사용하지 않음")
        }
    }
}
