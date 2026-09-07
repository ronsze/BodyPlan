package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.DayStatus
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutLog
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetMonthlyDayStatusUseCaseTest {
    private val today = LocalDate.of(2026, 9, 7)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val isEditableDate = IsEditableDateUseCase(clock)

    private fun useCaseWith(recorded: Map<LocalDate, Set<BodyPart>>): GetMonthlyDayStatusUseCase =
        GetMonthlyDayStatusUseCase(
            workoutLogRepository = FakeWorkoutLogRepository(recorded),
            isEditableDate = isEditableDate,
            clock = clock,
        )

    @Test
    fun `기록이 있는 날은 Recorded이고 부위 집합을 담는다`() = runTest {
        val recordedDay = LocalDate.of(2026, 9, 3)
        val bodyParts = setOf(BodyPart.CHEST, BodyPart.BACK)
        val result = useCaseWith(mapOf(recordedDay to bodyParts))(YearMonth.of(2026, 9)).first()

        assertEquals(DayStatus.Recorded(bodyParts), result[recordedDay])
    }

    @Test
    fun `기록이 없는 오늘과 어제는 Pending이다`() = runTest {
        val result = useCaseWith(emptyMap())(YearMonth.of(2026, 9)).first()

        assertEquals(DayStatus.Pending, result[today])
        assertEquals(DayStatus.Pending, result[today.minusDays(1)])
    }

    @Test
    fun `기록이 없는 그저께 이전은 Rest다`() = runTest {
        val result = useCaseWith(emptyMap())(YearMonth.of(2026, 9)).first()

        assertEquals(DayStatus.Rest, result[today.minusDays(2)])
        assertEquals(DayStatus.Rest, result[LocalDate.of(2026, 9, 1)])
    }

    @Test
    fun `오늘 이후는 기록 유무와 무관하게 Upcoming이다`() = runTest {
        val futureDay = today.plusDays(3)
        val result = useCaseWith(mapOf(futureDay to setOf(BodyPart.LEG)))(YearMonth.of(2026, 9)).first()

        assertEquals(DayStatus.Upcoming, result[futureDay])
    }

    @Test
    fun `결과 맵은 그 달의 모든 날짜를 키로 갖는다`() = runTest {
        val yearMonth = YearMonth.of(2026, 9)
        val result = useCaseWith(emptyMap())(yearMonth).first()

        assertEquals(yearMonth.lengthOfMonth(), result.size)
        assertTrue(result.containsKey(yearMonth.atDay(1)))
        assertTrue(result.containsKey(yearMonth.atEndOfMonth()))
    }

    @Test
    fun `과거 달을 조회하면 today가 달 밖이라도 모든 날짜가 채워진다`() = runTest {
        val pastMonth = YearMonth.of(2026, 8)
        val result = useCaseWith(emptyMap())(pastMonth).first()

        assertEquals(pastMonth.lengthOfMonth(), result.size)
        // today(9월 7일)의 시점에서 8월은 전부 지난 달이므로 편집 가능한 날이 없어 전부 Rest다.
        assertTrue(result.values.all { it == DayStatus.Rest })
    }

    @Test
    fun `미래 달을 조회하면 today가 달 밖이라도 모든 날짜가 Upcoming이다`() = runTest {
        val futureMonth = YearMonth.of(2026, 10)
        val recorded = mapOf(futureMonth.atDay(5) to setOf(BodyPart.SHOULDER))
        val result = useCaseWith(recorded)(futureMonth).first()

        assertEquals(futureMonth.lengthOfMonth(), result.size)
        assertTrue(result.values.all { it == DayStatus.Upcoming })
    }

    private class FakeWorkoutLogRepository(private val recorded: Map<LocalDate, Set<BodyPart>>) :
        WorkoutLogRepository {
        override fun observeLog(date: LocalDate): Flow<WorkoutLog> = error("사용하지 않음")

        override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
            flowOf(recorded.filterKeys { it in from..to })

        override suspend fun getEntry(id: Long): WorkoutEntry? = error("사용하지 않음")

        override suspend fun addEntry(date: LocalDate, exercise: Exercise, sets: List<WorkoutSet>): Long =
            error("사용하지 않음")

        override suspend fun updateEntry(entryId: Long, exercise: Exercise, sets: List<WorkoutSet>) {
            error("사용하지 않음")
        }

        override suspend fun deleteEntry(id: Long) {
            error("사용하지 않음")
        }
    }
}
