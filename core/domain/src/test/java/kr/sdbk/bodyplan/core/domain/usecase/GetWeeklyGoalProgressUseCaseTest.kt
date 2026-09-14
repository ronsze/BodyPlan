package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.ExerciseBest
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutLog
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetWeeklyGoalProgressUseCaseTest {
    // 2026-09-17은 목요일이다. 이번 주(월요일 시작)는 9/14 ~ 9/20이고, 오늘까지는 9/14 ~ 9/17이다.
    private val today = LocalDate.of(2026, 9, 17)
    private val clock: Clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val thisWeekStart: LocalDate = LocalDate.of(2026, 9, 14)

    private fun useCaseWith(
        profile: UserProfile = UserProfile(),
        recordedDates: Set<LocalDate> = emptySet(),
        workoutFailure: Throwable? = null,
        profileFailure: Throwable? = null,
    ): GetWeeklyGoalProgressUseCase = GetWeeklyGoalProgressUseCase(
        userProfileRepository = FakeUserProfileRepository(profile, profileFailure),
        workoutLogRepository = FakeWorkoutLogRepository(recordedDates, workoutFailure),
        clock = clock,
    )

    @Test
    fun `목표가 없으면 null이다`() = runTest {
        val progress = useCaseWith(profile = UserProfile(weeklyWorkoutGoal = null))().first()

        assertNull(progress)
    }

    @Test
    fun `이번 주 기록이 있는 날짜 수가 doneDays다`() = runTest {
        val recorded = setOf(
            thisWeekStart,
            thisWeekStart.plusDays(1),
            thisWeekStart.plusDays(1), // 같은 날은 중복으로 세지 않는다(Set이라 자연히 하나).
        )

        val progress = useCaseWith(
            profile = UserProfile(weeklyWorkoutGoal = 4),
            recordedDates = recorded,
        )().first()

        assertEquals(4, progress!!.goalDays)
        assertEquals(2, progress.doneDays)
    }

    @Test
    fun `지난 주부터 목표를 채운 주가 연속이면 이번 주 달성 시 스트릭에 1을 더한다`() = runTest {
        val goal = 3
        val lastWeekStart = thisWeekStart.minusWeeks(1)
        val twoWeeksAgoStart = thisWeekStart.minusWeeks(2)
        val recorded = setOf(
            // 이번 주 3일 달성
            thisWeekStart, thisWeekStart.plusDays(1), thisWeekStart.plusDays(2),
            // 지난 주 3일 달성
            lastWeekStart, lastWeekStart.plusDays(1), lastWeekStart.plusDays(2),
            // 2주 전 3일 달성
            twoWeeksAgoStart, twoWeeksAgoStart.plusDays(1), twoWeeksAgoStart.plusDays(2),
        )

        val progress = useCaseWith(
            profile = UserProfile(weeklyWorkoutGoal = goal),
            recordedDates = recorded,
        )().first()

        assertEquals(3, progress!!.streakWeeks)
    }

    @Test
    fun `이번 주가 미달이면 스트릭에 더하지 않되 지난 연속은 깨지 않는다`() = runTest {
        val goal = 3
        val lastWeekStart = thisWeekStart.minusWeeks(1)
        val recorded = setOf(
            // 이번 주는 1일만 — 미달
            thisWeekStart,
            // 지난 주는 3일 — 달성
            lastWeekStart,
            lastWeekStart.plusDays(1),
            lastWeekStart.plusDays(2),
        )

        val progress = useCaseWith(
            profile = UserProfile(weeklyWorkoutGoal = goal),
            recordedDates = recorded,
        )().first()

        assertEquals(1, progress!!.doneDays)
        assertEquals(1, progress.streakWeeks)
    }

    @Test
    fun `스트릭은 52주를 넘지 않는다`() = runTest {
        val goal = 1
        val recorded = (0..60).map { thisWeekStart.minusWeeks(it.toLong()) }.toSet()

        val progress = useCaseWith(
            profile = UserProfile(weeklyWorkoutGoal = goal),
            recordedDates = recorded,
        )().first()

        assertEquals(52, progress!!.streakWeeks)
    }

    @Test
    fun `프로필 조회가 실패하면 Flow가 실패한다`() = runTest {
        val useCase = useCaseWith(profileFailure = IllegalStateException("boom"))

        var thrown: Throwable? = null
        try {
            useCase().first()
        } catch (t: Throwable) {
            thrown = t
        }

        assertEquals("boom", thrown?.message)
    }

    @Test
    fun `운동 기록 조회가 실패하면 Flow가 실패한다`() = runTest {
        val useCase = useCaseWith(
            profile = UserProfile(weeklyWorkoutGoal = 3),
            workoutFailure = IllegalStateException("boom"),
        )

        var thrown: Throwable? = null
        try {
            useCase().first()
        } catch (t: Throwable) {
            thrown = t
        }

        assertEquals("boom", thrown?.message)
    }

    private class FakeUserProfileRepository(private val profile: UserProfile, private val failure: Throwable?) :
        UserProfileRepository {
        override fun observeProfile(): Flow<UserProfile> = flow {
            failure?.let { throw it }
            emit(profile)
        }

        override suspend fun getProfile(): UserProfile = error("사용하지 않음")

        override suspend fun saveProfile(profile: UserProfile) = error("사용하지 않음")
    }

    private class FakeWorkoutLogRepository(
        private val recordedDates: Set<LocalDate>,
        private val failure: Throwable?,
    ) : WorkoutLogRepository {
        override fun observeLog(date: LocalDate): Flow<WorkoutLog> = error("사용하지 않음")

        override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
            flow {
                failure?.let { throw it }
                emit(
                    recordedDates.filter { !it.isBefore(from) && !it.isAfter(to) }
                        .associateWith { setOf(BodyPart.CHEST) },
                )
            }

        override fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>> =
            error("사용하지 않음")

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
