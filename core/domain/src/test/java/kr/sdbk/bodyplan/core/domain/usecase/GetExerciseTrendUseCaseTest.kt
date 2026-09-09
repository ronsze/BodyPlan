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
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.RecordedExercise
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutLog
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class GetExerciseTrendUseCaseTest {
    // 목요일. 그 주의 월요일 시작은 2026-09-07이다.
    private val today = LocalDate.of(2026, 9, 10)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val summarizeExerciseTrend = SummarizeExerciseTrendUseCase()

    private fun entry(
        exerciseId: Long,
        exerciseName: String,
        bodyPart: BodyPart = BodyPart.CHEST,
        intensityType: IntensityType = IntensityType.WEIGHT,
        sets: List<WorkoutSet> = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))),
    ): WorkoutEntry = WorkoutEntry(
        id = 1L,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        bodyPart = bodyPart,
        intensityType = intensityType,
        sets = sets,
    )

    private fun useCaseWith(
        entriesByDate: Map<LocalDate, List<WorkoutEntry>>,
        onObserve: (from: LocalDate, to: LocalDate) -> Unit = { _, _ -> },
    ): GetExerciseTrendUseCase = GetExerciseTrendUseCase(
        workoutLogRepository = FakeWorkoutLogRepository(entriesByDate, onObserve),
        summarizeExerciseTrend = summarizeExerciseTrend,
        clock = clock,
    )

    @Test
    fun `오늘이 속한 주부터 뒤로 8주를 오래된 주부터 조회한다`() = runTest {
        var observedFrom: LocalDate? = null
        var observedTo: LocalDate? = null
        val useCase = useCaseWith(emptyMap()) { from, to ->
            observedFrom = from
            observedTo = to
        }

        useCase(null).first()

        assertEquals(LocalDate.of(2026, 7, 20), observedFrom)
        assertEquals(today, observedTo)
    }

    @Test
    fun `exercises는 기록에 나온 종목을 이름 오름차순으로 중복 없이 낸다`() = runTest {
        val entries = mapOf(
            LocalDate.of(2026, 9, 1) to listOf(entry(exerciseId = 2L, exerciseName = "스쿼트")),
            LocalDate.of(2026, 9, 2) to listOf(entry(exerciseId = 1L, exerciseName = "벤치프레스")),
        )

        val result = useCaseWith(entries)(null).first()

        assertEquals(
            listOf(
                RecordedExercise(
                    id = 1L,
                    name = "벤치프레스",
                    bodyPart = BodyPart.CHEST,
                    intensityType = IntensityType.WEIGHT,
                ),
                RecordedExercise(
                    id = 2L,
                    name = "스쿼트",
                    bodyPart = BodyPart.CHEST,
                    intensityType = IntensityType.WEIGHT,
                ),
            ),
            result.exercises,
        )
    }

    @Test
    fun `같은 종목이 여러 번 나오면 가장 최근 기록의 스냅샷을 쓴다`() = runTest {
        val entries = mapOf(
            LocalDate.of(2026, 9, 1) to listOf(entry(exerciseId = 1L, exerciseName = "옛 이름")),
            LocalDate.of(2026, 9, 3) to listOf(entry(exerciseId = 1L, exerciseName = "새 이름")),
        )

        val result = useCaseWith(entries)(null).first()

        assertEquals(
            listOf(
                RecordedExercise(
                    id = 1L,
                    name = "새 이름",
                    bodyPart = BodyPart.CHEST,
                    intensityType = IntensityType.WEIGHT,
                ),
            ),
            result.exercises,
        )
    }

    @Test
    fun `exerciseId가 null이면 trend는 null이고 exercises는 그대로 나온다`() = runTest {
        val entries = mapOf(
            LocalDate.of(2026, 9, 1) to listOf(entry(exerciseId = 1L, exerciseName = "벤치프레스")),
        )

        val result = useCaseWith(entries)(null).first()

        assertNull(result.trend)
        assertEquals(1, result.exercises.size)
    }

    @Test
    fun `exerciseId를 주면 그 종목의 추이를 낸다`() = runTest {
        val entries = mapOf(
            LocalDate.of(2026, 9, 1) to listOf(entry(exerciseId = 1L, exerciseName = "벤치프레스")),
        )

        val result = useCaseWith(entries)(1L).first()

        assertEquals(1L, result.trend?.exercise?.id)
    }

    @Test
    fun `저장소가 던지면 그대로 위로 전파된다`() = runTest {
        val useCase = GetExerciseTrendUseCase(
            workoutLogRepository = ThrowingWorkoutLogRepository(),
            summarizeExerciseTrend = summarizeExerciseTrend,
            clock = clock,
        )

        assertThrows(IllegalStateException::class.java) {
            useCase(null)
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

        override suspend fun addEntry(date: LocalDate, exercise: Exercise, sets: List<WorkoutSet>): Long =
            error("사용하지 않음")

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

        override suspend fun addEntry(date: LocalDate, exercise: Exercise, sets: List<WorkoutSet>): Long =
            error("사용하지 않음")

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
