package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.ProgressDirection
import kr.sdbk.bodyplan.core.domain.model.ProgressHeadline
import kr.sdbk.bodyplan.core.domain.model.ProgressMetricKey
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutLog
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.domain.repository.WeightLogRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class GetProgressSummaryUseCaseTest {
    private val today = LocalDate.of(2026, 9, 10)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    // 최근: 9/4 ~ 9/10, 그 앞: 8/28 ~ 9/3
    private val recentDay = today.minusDays(3)
    private val previousDay = today.minusDays(10)

    private fun useCaseWith(
        weightRecords: List<WeightRecord> = emptyList(),
        entriesByDate: Map<LocalDate, List<WorkoutEntry>> = emptyMap(),
        profile: UserProfile = UserProfile(),
        inbodyHistory: List<AnalysisResult> = emptyList(),
    ): GetProgressSummaryUseCase = GetProgressSummaryUseCase(
        weightLogRepository = FakeWeightLogRepository(weightRecords),
        workoutLogRepository = FakeWorkoutLogRepository(entriesByDate),
        userProfileRepository = FakeUserProfileRepository(profile),
        analysisResultRepository = FakeAnalysisResultRepository(inbodyHistory),
        getWeightTrend = GetWeightTrendUseCase(),
        summarizeWorkoutVolume = SummarizeWorkoutVolumeUseCase(),
        summarizeBodyPartVolumeTrend = SummarizeBodyPartVolumeTrendUseCase(SummarizeBodyPartVolumeUseCase()),
        clock = clock,
    )

    private fun weightEntry(volume: Int): WorkoutEntry = WorkoutEntry(
        id = 1L,
        exerciseId = 1L,
        exerciseName = "종목",
        bodyPart = BodyPart.CHEST,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = 1, intensity = Intensity.Weight(volume))),
    )

    private fun angleEntry(): WorkoutEntry = WorkoutEntry(
        id = 2L,
        exerciseId = 2L,
        exerciseName = "종목",
        bodyPart = BodyPart.SHOULDER,
        intensityType = IntensityType.ANGLE,
        sets = listOf(WorkoutSet(repeatCount = 1, intensity = Intensity.Angle(30))),
    )

    private fun inbodyResult(skeletalMuscleKg: Double? = null, bodyFatKg: Double? = null): AnalysisResult =
        AnalysisResult(
            id = 1L,
            kind = AnalysisKind.INBODY,
            scopeKey = "",
            content = AnalysisContent(summary = "", sections = emptyList()),
            createdAtMillis = 0L,
            measurement = InbodyMeasurement(skeletalMuscleKg = skeletalMuscleKg, bodyFatKg = bodyFatKg),
        )

    @Test
    fun `최근 7일과 그 앞 7일에 체중 기록이 모두 있으면 두 구간 평균의 차이다`() = runTest {
        val records = listOf(WeightRecord(recentDay, 70.0), WeightRecord(previousDay, 68.0))

        val metric = useCaseWith(weightRecords = records)().first().recentMetrics
            .first { it.key == ProgressMetricKey.WEIGHT }

        assertEquals(2.0, metric.changeValue!!, 0.0001)
    }

    @Test
    fun `한쪽 구간에 체중 기록이 없으면 WEIGHT는 changeValue null이고 direction UNKNOWN이다`() = runTest {
        val records = listOf(WeightRecord(recentDay, 70.0))

        val metric = useCaseWith(weightRecords = records)().first().recentMetrics
            .first { it.key == ProgressMetricKey.WEIGHT }

        assertEquals(null, metric.changeValue)
        assertEquals(ProgressDirection.UNKNOWN, metric.direction)
    }

    @Test
    fun `무게 종목 볼륨 차이가 WORKOUT_VOLUME에 기록된 날 수 차이가 WORKOUT_DAYS에 담긴다`() = runTest {
        val entriesByDate = mapOf(
            recentDay to listOf(weightEntry(100)),
            previousDay to listOf(weightEntry(40)),
        )

        val summary = useCaseWith(entriesByDate = entriesByDate)().first()

        val volume = summary.recentMetrics.first { it.key == ProgressMetricKey.WORKOUT_VOLUME }
        val days = summary.recentMetrics.first { it.key == ProgressMetricKey.WORKOUT_DAYS }
        assertEquals(60.0, volume.changeValue!!, 0.0001)
        assertEquals(0.0, days.changeValue!!, 0.0001)
    }

    @Test
    fun `두 구간 모두 운동 기록이 없으면 WORKOUT_VOLUME과 WORKOUT_DAYS 모두 changeValue null이고 UNKNOWN이다`() = runTest {
        val summary = useCaseWith(entriesByDate = emptyMap())().first()

        val volume = summary.recentMetrics.first { it.key == ProgressMetricKey.WORKOUT_VOLUME }
        val days = summary.recentMetrics.first { it.key == ProgressMetricKey.WORKOUT_DAYS }
        assertEquals(null, volume.changeValue)
        assertEquals(ProgressDirection.UNKNOWN, volume.direction)
        assertEquals(null, days.changeValue)
        assertEquals(ProgressDirection.UNKNOWN, days.direction)
    }

    @Test
    fun `Goal DIET면 체중 감소가 IMPROVING이다`() = runTest {
        val records = listOf(WeightRecord(recentDay, 68.0), WeightRecord(previousDay, 70.0))
        val profile = UserProfile(goals = setOf(Goal.DIET))

        val metric = useCaseWith(weightRecords = records, profile = profile)().first().recentMetrics
            .first { it.key == ProgressMetricKey.WEIGHT }

        assertEquals(ProgressDirection.IMPROVING, metric.direction)
    }

    @Test
    fun `Goal MUSCLE_GAIN이면 체중 증가가 IMPROVING이다`() = runTest {
        val records = listOf(WeightRecord(recentDay, 70.0), WeightRecord(previousDay, 68.0))
        val profile = UserProfile(goals = setOf(Goal.MUSCLE_GAIN))

        val metric = useCaseWith(weightRecords = records, profile = profile)().first().recentMetrics
            .first { it.key == ProgressMetricKey.WEIGHT }

        assertEquals(ProgressDirection.IMPROVING, metric.direction)
    }

    @Test
    fun `Goal TARGET_WEIGHT면 증가해도 목표에 가까워지면 IMPROVING이다`() = runTest {
        // 목표 75kg. 이전 평균 70 -> 최근 평균 72로 늘었지만 목표와의 거리는 5에서 3으로 줄었다.
        val records = listOf(WeightRecord(recentDay, 72.0), WeightRecord(previousDay, 70.0))
        val profile = UserProfile(goals = setOf(Goal.TARGET_WEIGHT), targetWeightKg = 75)

        val metric = useCaseWith(weightRecords = records, profile = profile)().first().recentMetrics
            .first { it.key == ProgressMetricKey.WEIGHT }

        assertEquals(ProgressDirection.IMPROVING, metric.direction)
    }

    @Test
    fun `목표가 비어 있으면 체중 값이 있어도 direction은 STEADY다`() = runTest {
        val records = listOf(WeightRecord(recentDay, 70.0), WeightRecord(previousDay, 68.0))

        val metric = useCaseWith(weightRecords = records, profile = UserProfile())().first().recentMetrics
            .first { it.key == ProgressMetricKey.WEIGHT }

        assertEquals(ProgressDirection.STEADY, metric.direction)
    }

    @Test
    fun `골격근량 증가가 IMPROVING 체지방량 감소가 IMPROVING이다`() = runTest {
        // observeHistory는 최신이 앞이므로 첫 번째가 최신값이다.
        val history = listOf(
            inbodyResult(skeletalMuscleKg = 30.0, bodyFatKg = 15.0),
            inbodyResult(skeletalMuscleKg = 28.0, bodyFatKg = 17.0),
        )

        val summary = useCaseWith(inbodyHistory = history)().first()

        val muscle = summary.bodyCompositionMetrics.first { it.key == ProgressMetricKey.SKELETAL_MUSCLE }
        val fat = summary.bodyCompositionMetrics.first { it.key == ProgressMetricKey.BODY_FAT }
        assertEquals(2.0, muscle.changeValue!!, 0.0001)
        assertEquals(ProgressDirection.IMPROVING, muscle.direction)
        assertEquals(-2.0, fat.changeValue!!, 0.0001)
        assertEquals(ProgressDirection.IMPROVING, fat.direction)
    }

    @Test
    fun `측정값이 있는 인바디 결과가 1건 이하면 체성분 두 축이 UNKNOWN이다`() = runTest {
        val history = listOf(inbodyResult(skeletalMuscleKg = 30.0, bodyFatKg = 15.0))

        val summary = useCaseWith(inbodyHistory = history)().first()

        assertEquals(ProgressDirection.UNKNOWN, summary.bodyCompositionMetrics[0].direction)
        assertEquals(ProgressDirection.UNKNOWN, summary.bodyCompositionMetrics[1].direction)
    }

    @Test
    fun `판정된 지표 중 개선이 많으면 헤드라인이 IMPROVING이다`() = runTest {
        // 체중(개선, DIET) + 골격근(개선) + 체지방(개선) vs 볼륨·일수(UNKNOWN, 기록 없음) = 개선 3, 악화 0
        val records = listOf(WeightRecord(recentDay, 68.0), WeightRecord(previousDay, 70.0))
        val profile = UserProfile(goals = setOf(Goal.DIET))
        val history = listOf(
            inbodyResult(skeletalMuscleKg = 30.0, bodyFatKg = 15.0),
            inbodyResult(skeletalMuscleKg = 28.0, bodyFatKg = 17.0),
        )

        val summary = useCaseWith(weightRecords = records, profile = profile, inbodyHistory = history)().first()

        assertEquals(ProgressHeadline.IMPROVING, summary.headline)
    }

    @Test
    fun `판정된 지표 중 악화가 많으면 헤드라인이 WORSENING이다`() = runTest {
        val records = listOf(WeightRecord(recentDay, 70.0), WeightRecord(previousDay, 68.0))
        val profile = UserProfile(goals = setOf(Goal.DIET))
        val history = listOf(
            inbodyResult(skeletalMuscleKg = 28.0, bodyFatKg = 17.0),
            inbodyResult(skeletalMuscleKg = 30.0, bodyFatKg = 15.0),
        )

        val summary = useCaseWith(weightRecords = records, profile = profile, inbodyHistory = history)().first()

        assertEquals(ProgressHeadline.WORSENING, summary.headline)
    }

    @Test
    fun `판정된 지표의 개선과 악화가 같으면 헤드라인이 STEADY다`() = runTest {
        // 체중(개선, DIET) vs 골격근(악화) = 1대1, 체지방·볼륨·일수는 UNKNOWN
        val records = listOf(WeightRecord(recentDay, 68.0), WeightRecord(previousDay, 70.0))
        val profile = UserProfile(goals = setOf(Goal.DIET))
        val history = listOf(
            inbodyResult(skeletalMuscleKg = 28.0),
            inbodyResult(skeletalMuscleKg = 30.0),
        )

        val summary = useCaseWith(weightRecords = records, profile = profile, inbodyHistory = history)().first()

        assertEquals(ProgressHeadline.STEADY, summary.headline)
    }

    @Test
    fun `모든 지표가 UNKNOWN이면 헤드라인이 NOT_ENOUGH_DATA다`() = runTest {
        val summary = useCaseWith()().first()

        assertEquals(ProgressHeadline.NOT_ENOUGH_DATA, summary.headline)
    }

    @Test
    fun `recentMetrics는 늘 3개 bodyCompositionMetrics는 늘 2개를 담는다`() = runTest {
        val summary = useCaseWith()().first()

        assertEquals(3, summary.recentMetrics.size)
        assertEquals(2, summary.bodyCompositionMetrics.size)
        assertEquals(
            listOf(ProgressMetricKey.WEIGHT, ProgressMetricKey.WORKOUT_VOLUME, ProgressMetricKey.WORKOUT_DAYS),
            summary.recentMetrics.map { it.key },
        )
        assertEquals(
            listOf(ProgressMetricKey.SKELETAL_MUSCLE, ProgressMetricKey.BODY_FAT),
            summary.bodyCompositionMetrics.map { it.key },
        )
    }

    @Test
    fun `체중 변화가 0점1 미만이면 STEADY다`() = runTest {
        val records = listOf(WeightRecord(recentDay, 70.0), WeightRecord(previousDay, 70.05))
        val profile = UserProfile(goals = setOf(Goal.DIET))

        val metric = useCaseWith(weightRecords = records, profile = profile)().first().recentMetrics
            .first { it.key == ProgressMetricKey.WEIGHT }

        assertEquals(ProgressDirection.STEADY, metric.direction)
    }

    @Test
    fun `운동한 날 수 차이가 0이면 기록이 있어도 STEADY이지 UNKNOWN이 아니다`() = runTest {
        val entriesByDate = mapOf(
            recentDay to listOf(angleEntry()),
            previousDay to listOf(angleEntry()),
        )

        val metric = useCaseWith(entriesByDate = entriesByDate)().first().recentMetrics
            .first { it.key == ProgressMetricKey.WORKOUT_DAYS }

        assertEquals(0.0, metric.changeValue!!, 0.0001)
        assertEquals(ProgressDirection.STEADY, metric.direction)
    }

    @Test
    fun `14일 구간 기록이 최근 7일과 그 앞 7일로 갈려 bodyPartVolumeTrends에 담긴다`() = runTest {
        val entriesByDate = mapOf(
            recentDay to listOf(weightEntry(100)),
            previousDay to listOf(weightEntry(40)),
        )

        val trends = useCaseWith(entriesByDate = entriesByDate)().first().bodyPartVolumeTrends

        assertEquals(listOf(BodyPart.CHEST), trends.map { it.bodyPart })
        assertEquals(100, trends[0].recentVolumeKg)
        assertEquals(60, trends[0].changeKg)
        assertEquals(ProgressDirection.IMPROVING, trends[0].direction)
    }

    @Test
    fun `두 구간 모두 무게 기록이 없으면 bodyPartVolumeTrends가 빈 목록이다`() = runTest {
        val summary = useCaseWith()().first()

        assertEquals(emptyList<Any>(), summary.bodyPartVolumeTrends)
    }

    private class FakeWeightLogRepository(private val records: List<WeightRecord>) : WeightLogRepository {
        override fun observeRecordsInRange(from: LocalDate, to: LocalDate): Flow<List<WeightRecord>> =
            flowOf(records.filter { !it.date.isBefore(from) && !it.date.isAfter(to) })

        override suspend fun save(date: LocalDate, weightKg: Double) = error("사용하지 않음")
    }

    private class FakeWorkoutLogRepository(private val entriesByDate: Map<LocalDate, List<WorkoutEntry>>) :
        WorkoutLogRepository {
        override fun observeLog(date: LocalDate): Flow<WorkoutLog> = error("사용하지 않음")

        override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
            error("사용하지 않음")

        override fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>> =
            flowOf(entriesByDate.filterKeys { !it.isBefore(from) && !it.isAfter(to) })

        override suspend fun getEntry(id: Long): WorkoutEntry? = error("사용하지 않음")

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

    private class FakeUserProfileRepository(private val profile: UserProfile) : UserProfileRepository {
        override fun observeProfile(): Flow<UserProfile> = flowOf(profile)

        override suspend fun getProfile(): UserProfile = error("사용하지 않음")

        override suspend fun saveProfile(profile: UserProfile) = error("사용하지 않음")
    }

    private class FakeAnalysisResultRepository(private val history: List<AnalysisResult>) : AnalysisResultRepository {
        override fun observeLatest(kind: AnalysisKind, scopeKey: String): Flow<AnalysisResult?> = error("사용하지 않음")

        override suspend fun getLatestOf(kind: AnalysisKind, scopeKeys: List<String>): Map<String, AnalysisResult> =
            error("사용하지 않음")

        override fun observeHistory(kind: AnalysisKind): Flow<List<AnalysisResult>> = flowOf(history)

        override suspend fun save(
            kind: AnalysisKind,
            scopeKey: String,
            content: AnalysisContent,
            imageFileName: String?,
            measurement: InbodyMeasurement?,
        ) = error("사용하지 않음")
    }
}
