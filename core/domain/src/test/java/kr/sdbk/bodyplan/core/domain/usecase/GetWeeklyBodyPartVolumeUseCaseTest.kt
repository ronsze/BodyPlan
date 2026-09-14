package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
import org.junit.Assert.assertThrows
import org.junit.Test

class GetWeeklyBodyPartVolumeUseCaseTest {
    // 목요일. 그 주의 월요일 시작은 2026-09-07이다.
    private val today = LocalDate.of(2026, 9, 10)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val summarizeBodyPartVolume = SummarizeBodyPartVolumeUseCase()

    private fun entry(bodyPart: BodyPart, weightKg: Int = 20, repeatCount: Int = 10): WorkoutEntry = WorkoutEntry(
        id = 1L,
        exerciseId = 1L,
        exerciseName = "종목",
        bodyPart = bodyPart,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = repeatCount, intensity = Intensity.Weight(weightKg))),
    )

    private fun useCaseWith(
        entriesByDate: Map<LocalDate, List<WorkoutEntry>>,
        onObserve: (from: LocalDate, to: LocalDate) -> Unit = { _, _ -> },
    ): GetWeeklyBodyPartVolumeUseCase = GetWeeklyBodyPartVolumeUseCase(
        workoutLogRepository = FakeWorkoutLogRepository(entriesByDate, onObserve),
        summarizeBodyPartVolume = summarizeBodyPartVolume,
        clock = clock,
    )

    @Test
    fun `이번 주 월요일부터 오늘까지를 조회한다`() = runTest {
        var observedFrom: LocalDate? = null
        var observedTo: LocalDate? = null
        val useCase = useCaseWith(emptyMap()) { from, to ->
            observedFrom = from
            observedTo = to
        }

        useCase().first()

        assertEquals(LocalDate.of(2026, 9, 7), observedFrom)
        assertEquals(today, observedTo)
    }

    @Test
    fun `조회한 기록을 부위별로 줄인다`() = runTest {
        val entries = mapOf(
            LocalDate.of(2026, 9, 7) to listOf(entry(BodyPart.CHEST)),
            LocalDate.of(2026, 9, 9) to listOf(entry(BodyPart.BACK, weightKg = 10, repeatCount = 5)),
        )

        val result = useCaseWith(entries)().first()

        assertEquals(
            listOf(BodyPart.CHEST, BodyPart.BACK),
            result.map { it.bodyPart },
        )
    }

    @Test
    fun `저장소가 실패하면 Flow로 그대로 전파된다`() = runTest {
        val useCase = GetWeeklyBodyPartVolumeUseCase(
            workoutLogRepository = ThrowingWorkoutLogRepository(),
            summarizeBodyPartVolume = summarizeBodyPartVolume,
            clock = clock,
        )

        assertThrows(IllegalStateException::class.java) {
            useCase()
        }
    }

    private class FakeWorkoutLogRepository(
        private val entriesByDate: Map<LocalDate, List<WorkoutEntry>>,
        private val onObserve: (from: LocalDate, to: LocalDate) -> Unit,
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

        override suspend fun saveMemo(date: LocalDate, text: String) {
            error("사용하지 않음")
        }
    }

    private class ThrowingWorkoutLogRepository : WorkoutLogRepository {
        override fun observeLog(date: LocalDate): Flow<WorkoutLog> = error("사용하지 않음")

        override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
            error("사용하지 않음")

        override fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>> =
            throw IllegalStateException("조회 실패")

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

        override suspend fun saveMemo(date: LocalDate, text: String) {
            error("사용하지 않음")
        }
    }
}
