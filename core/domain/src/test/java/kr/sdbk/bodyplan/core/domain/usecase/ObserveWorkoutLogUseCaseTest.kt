package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveWorkoutLogUseCaseTest {
    private val date = LocalDate.of(2026, 9, 7)

    private fun entry(id: Long, exerciseId: Long, weight: Int) = WorkoutEntry(
        id = id,
        exerciseId = exerciseId,
        exerciseName = "종목$exerciseId",
        bodyPart = BodyPart.CHEST,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(weight))),
    )

    @Test
    fun `기록과 지난 최고값을 합쳐 그날의 PR 종목 집합을 낸다`() = runTest {
        val entries = listOf(entry(id = 1L, exerciseId = 1L, weight = 60))
        val repository = FakeWorkoutLogRepository(
            log = WorkoutLog(date = date, entries = entries),
            bestBefore = listOf(
                ExerciseBest(
                    exerciseId = 1L,
                    intensityType = IntensityType.WEIGHT,
                    maxIntensityValue = 50,
                    maxRepeatCount = 0,
                ),
            ),
        )
        val useCase = ObserveWorkoutLogUseCase(repository, FindPersonalRecordsUseCase())

        val result = useCase(date).first()

        assertEquals(entries, result.log.entries)
        assertEquals(setOf(1L), result.personalRecordExerciseIds)
    }

    @Test
    fun `이전 최고값을 넘기지 못하면 PR 집합이 비어 있다`() = runTest {
        val entries = listOf(entry(id = 1L, exerciseId = 1L, weight = 40))
        val repository = FakeWorkoutLogRepository(
            log = WorkoutLog(date = date, entries = entries),
            bestBefore = listOf(
                ExerciseBest(
                    exerciseId = 1L,
                    intensityType = IntensityType.WEIGHT,
                    maxIntensityValue = 50,
                    maxRepeatCount = 0,
                ),
            ),
        )
        val useCase = ObserveWorkoutLogUseCase(repository, FindPersonalRecordsUseCase())

        val result = useCase(date).first()

        assertTrue(result.personalRecordExerciseIds.isEmpty())
    }

    @Test
    fun `기록 조회가 실패하면 예외가 그대로 전파된다`() = runTest {
        val repository = FakeWorkoutLogRepository(
            log = WorkoutLog(date = date, entries = emptyList()),
            bestBefore = emptyList(),
        )
        repository.logFailure = IllegalStateException("boom")
        val useCase = ObserveWorkoutLogUseCase(repository, FindPersonalRecordsUseCase())

        var thrown: Throwable? = null
        try {
            useCase(date).first()
        } catch (e: IllegalStateException) {
            thrown = e
        }
        assertTrue(thrown is IllegalStateException)
    }

    @Test
    fun `최고값 조회가 실패하면 예외가 그대로 전파된다`() = runTest {
        val repository = FakeWorkoutLogRepository(
            log = WorkoutLog(date = date, entries = emptyList()),
            bestBefore = emptyList(),
        )
        repository.bestBeforeFailure = IllegalStateException("boom")
        val useCase = ObserveWorkoutLogUseCase(repository, FindPersonalRecordsUseCase())

        var thrown: Throwable? = null
        try {
            useCase(date).first()
        } catch (e: IllegalStateException) {
            thrown = e
        }
        assertTrue(thrown is IllegalStateException)
    }

    private class FakeWorkoutLogRepository(private val log: WorkoutLog, private val bestBefore: List<ExerciseBest>) :
        WorkoutLogRepository {
        var logFailure: Throwable? = null
        var bestBeforeFailure: Throwable? = null

        override fun observeLog(date: LocalDate): Flow<WorkoutLog> {
            logFailure?.let { throw it }
            return MutableStateFlow(log)
        }

        override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
            error("사용하지 않음")

        override fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>> =
            error("사용하지 않음")

        override suspend fun getEntry(id: Long): WorkoutEntry? = error("사용하지 않음")

        override fun observeBestBefore(date: LocalDate): Flow<List<ExerciseBest>> {
            bestBeforeFailure?.let { throw it }
            return MutableStateFlow(bestBefore)
        }

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
