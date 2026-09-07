package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutLog
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AnalyzeWorkoutUseCaseTest {
    private val credential = AiCredential(provider = AiProvider.CLAUDE, token = "token")
    private val content = AnalysisContent(
        summary = "요약",
        sections = listOf(AnalysisSection(title = "부위별 볼륨", body = "가슴")),
    )
    private val today = LocalDate.of(2026, 9, 7)

    private fun useCaseWith(
        workoutLogRepository: FakeWorkoutLogRepository = FakeWorkoutLogRepository(),
        analysisResultRepository: FakeAnalysisResultRepository = FakeAnalysisResultRepository(),
        aiAnalysisRepository: FakeAiAnalysisRepository = FakeAiAnalysisRepository(),
        credential: AiCredential? = this.credential,
        today: LocalDate = this.today,
    ): Triple<AnalyzeWorkoutUseCase, FakeAiAnalysisRepository, FakeAnalysisResultRepository> {
        val useCase = AnalyzeWorkoutUseCase(
            workoutLogRepository = workoutLogRepository,
            userProfileRepository = FakeUserProfileRepository(),
            aiCredentialRepository = FakeAiCredentialRepository(credential),
            aiAnalysisRepository = aiAnalysisRepository,
            analysisResultRepository = analysisResultRepository,
            clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC),
        )
        return Triple(useCase, aiAnalysisRepository, analysisResultRepository)
    }

    private fun entry(
        bodyPart: BodyPart = BodyPart.CHEST,
        exerciseName: String = "벤치프레스",
        sets: List<WorkoutSet> = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(60))),
    ): WorkoutEntry = WorkoutEntry(
        id = 1L,
        exerciseId = 1L,
        exerciseName = exerciseName,
        bodyPart = bodyPart,
        intensityType = sets.first().intensity.let {
            when (it) {
                is Intensity.Weight -> IntensityType.WEIGHT
                is Intensity.Angle -> IntensityType.ANGLE
            }
        },
        sets = sets,
    )

    @Test
    fun `날짜별 분석은 그 날의 기록을 모아 AI를 부르고 결과를 저장한다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(date to listOf(entry())))
        val (useCase, aiAnalysisRepository, analysisResultRepository) = useCaseWith(
            workoutLogRepository = workoutLogRepository,
            today = date,
        )

        val result = useCase(
            kind = AnalysisKind.WORKOUT_DAILY,
            scopeKey = AnalysisScopeKey.daily(date),
            periodLabel = "9월 7일",
            from = date,
            to = date,
        )

        assertEquals(content, result)
        assertEquals(1, aiAnalysisRepository.analyzeWorkoutCallCount)
        assertEquals(listOf(date), aiAnalysisRepository.lastRequest?.entries?.map { it.date })
        assertEquals(
            content,
            analysisResultRepository.saved[AnalysisKind.WORKOUT_DAILY to AnalysisScopeKey.daily(date)],
        )
    }

    @Test
    fun `주간 분석도 저장된 날짜별 분석에 기대지 않고 기록 원문을 보낸다`() = runTest {
        val monday = LocalDate.of(2026, 9, 7)
        val sunday = monday.plusDays(6)
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(monday to listOf(entry())))
        val (useCase, aiAnalysisRepository, _) = useCaseWith(
            workoutLogRepository = workoutLogRepository,
            today = sunday,
        )

        useCase(
            kind = AnalysisKind.WORKOUT_WEEKLY,
            scopeKey = AnalysisScopeKey.weekly(monday),
            periodLabel = "이번 주",
            from = monday,
            to = sunday,
        )

        // 한 주(7일) 전부를 관찰했다 — 저장된 날짜별 분석이 아니라 기록 원문을 직접 읽었다는 뜻.
        assertEquals(7, workoutLogRepository.observedDates.size)
        assertEquals(1, aiAnalysisRepository.analyzeWorkoutCallCount)
        assertEquals(listOf(monday), aiAnalysisRepository.lastRequest?.entries?.map { it.date })
    }

    @Test
    fun `월간 분석도 기록 원문을 보낸다`() = runTest {
        val from = LocalDate.of(2026, 9, 1)
        val to = LocalDate.of(2026, 9, 30)
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(from to listOf(entry())))
        val (useCase, aiAnalysisRepository, _) = useCaseWith(
            workoutLogRepository = workoutLogRepository,
            today = to,
        )

        useCase(
            kind = AnalysisKind.WORKOUT_MONTHLY,
            scopeKey = AnalysisScopeKey.monthly(from),
            periodLabel = "9월",
            from = from,
            to = to,
        )

        assertEquals(30, workoutLogRepository.observedDates.size)
        assertEquals(1, aiAnalysisRepository.analyzeWorkoutCallCount)
    }

    @Test
    fun `요청에 담기는 workoutDayCount와 restDayCount는 앱이 센 정확한 값이다`() = runTest {
        val day1 = LocalDate.of(2026, 9, 1)
        val day2 = day1.plusDays(1)
        val day3 = day1.plusDays(2)
        // day1엔 기록 있음, day2엔 기록 없음(휴식), day3은 오늘 이후(아직 오지 않은 날)
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(day1 to listOf(entry())))
        val (useCase, aiAnalysisRepository, _) = useCaseWith(
            workoutLogRepository = workoutLogRepository,
            today = day2,
        )

        useCase(
            kind = AnalysisKind.WORKOUT_WEEKLY,
            scopeKey = AnalysisScopeKey.weekly(day1),
            periodLabel = "이번 주",
            from = day1,
            to = day3,
        )

        val request = aiAnalysisRepository.lastRequest!!
        assertEquals(1, request.workoutDayCount)
        // day3은 아직 오지 않은 날이라 쉰 날로 세지 않는다.
        assertEquals(1, request.restDayCount)
    }

    @Test
    fun `아직 오지 않은 날은 쉰 날로 세지 않는다`() = runTest {
        val day1 = LocalDate.of(2026, 9, 1)
        val day2 = day1.plusDays(1)
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(day1 to listOf(entry())))
        val (useCase, aiAnalysisRepository, _) = useCaseWith(
            workoutLogRepository = workoutLogRepository,
            today = day1,
        )

        useCase(
            kind = AnalysisKind.WORKOUT_WEEKLY,
            scopeKey = AnalysisScopeKey.weekly(day1),
            periodLabel = "이번 주",
            from = day1,
            to = day2,
        )

        assertEquals(0, aiAnalysisRepository.lastRequest?.restDayCount)
    }

    @Test
    fun `무게 종목과 각도 종목이 볼륨과 횟수에 각각 정확히 들어간다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val weightEntry = entry(
            bodyPart = BodyPart.CHEST,
            exerciseName = "벤치프레스",
            sets = listOf(
                WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(60)),
                WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(70)),
            ),
        )
        val angleEntry = entry(
            bodyPart = BodyPart.LEG,
            exerciseName = "레그레이즈",
            sets = listOf(
                WorkoutSet(repeatCount = 15, intensity = Intensity.Angle(45)),
            ),
        )
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(date to listOf(weightEntry, angleEntry)))
        val (useCase, aiAnalysisRepository, _) = useCaseWith(
            workoutLogRepository = workoutLogRepository,
            today = date,
        )

        useCase(
            kind = AnalysisKind.WORKOUT_DAILY,
            scopeKey = AnalysisScopeKey.daily(date),
            periodLabel = "9월 7일",
            from = date,
            to = date,
        )

        val request = aiAnalysisRepository.lastRequest!!
        // 60*10 + 70*8 = 600 + 560 = 1160. 각도 종목은 kg 볼륨에 섞이지 않는다.
        assertEquals(1160, request.totalWeightVolume)
        // 각도 종목의 횟수만 들어간다.
        assertEquals(15, request.totalBodyweightReps)
    }

    @Test
    fun `기록이 없는 기간을 분석하면 AI를 부르지 않고 NoRecordToAnalyzeException을 던진다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val (useCase, aiAnalysisRepository, _) = useCaseWith(
            workoutLogRepository = FakeWorkoutLogRepository(emptyMap()),
            today = date,
        )

        try {
            useCase(
                kind = AnalysisKind.WORKOUT_DAILY,
                scopeKey = AnalysisScopeKey.daily(date),
                periodLabel = "9월 7일",
                from = date,
                to = date,
            )
            fail("NoRecordToAnalyzeException이 나야 한다")
        } catch (e: NoRecordToAnalyzeException) {
            // 기대한 결과
        }

        assertEquals(0, aiAnalysisRepository.analyzeWorkoutCallCount)
    }

    @Test
    fun `토큰이 없으면 AI를 부르지 않고 AiCredentialMissingException을 던진다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(date to listOf(entry())))
        val (useCase, aiAnalysisRepository, analysisResultRepository) = useCaseWith(
            workoutLogRepository = workoutLogRepository,
            credential = null,
            today = date,
        )

        try {
            useCase(
                kind = AnalysisKind.WORKOUT_DAILY,
                scopeKey = AnalysisScopeKey.daily(date),
                periodLabel = "9월 7일",
                from = date,
                to = date,
            )
            fail("AiCredentialMissingException이 나야 한다")
        } catch (e: AiCredentialMissingException) {
            // 기대한 결과
        }

        assertEquals(0, aiAnalysisRepository.analyzeWorkoutCallCount)
        assertTrue(analysisResultRepository.saved.isEmpty())
    }

    @Test
    fun `호출이 실패하면 결과를 저장하지 않고 예외를 그대로 던진다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(date to listOf(entry())))
        val aiAnalysisRepository = FakeAiAnalysisRepository()
        aiAnalysisRepository.analyzeWorkoutFailure = IllegalStateException("boom")
        val (useCase, _, analysisResultRepository) = useCaseWith(
            workoutLogRepository = workoutLogRepository,
            aiAnalysisRepository = aiAnalysisRepository,
            today = date,
        )

        try {
            useCase(
                kind = AnalysisKind.WORKOUT_DAILY,
                scopeKey = AnalysisScopeKey.daily(date),
                periodLabel = "9월 7일",
                from = date,
                to = date,
            )
            fail("예외가 그대로 나야 한다")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }

        assertTrue(analysisResultRepository.saved.isEmpty())
    }

    private inner class FakeWorkoutLogRepository(
        private val logsByDate: Map<LocalDate, List<WorkoutEntry>> = emptyMap(),
    ) : WorkoutLogRepository {
        val observedDates: MutableList<LocalDate> = mutableListOf()

        override fun observeLog(date: LocalDate): Flow<WorkoutLog> {
            observedDates.add(date)
            return flowOf(WorkoutLog(date = date, entries = logsByDate[date].orEmpty()))
        }

        override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
            error("사용하지 않음")

        override suspend fun getEntry(id: Long): WorkoutEntry? = error("사용하지 않음")

        override suspend fun addEntry(
            date: LocalDate,
            exercise: kr.sdbk.bodyplan.core.domain.model.Exercise,
            sets: List<WorkoutSet>,
        ): Long = error("사용하지 않음")

        override suspend fun updateEntry(
            entryId: Long,
            exercise: kr.sdbk.bodyplan.core.domain.model.Exercise,
            sets: List<WorkoutSet>,
        ) {
            error("사용하지 않음")
        }

        override suspend fun deleteEntry(id: Long) {
            error("사용하지 않음")
        }
    }

    private inner class FakeUserProfileRepository : UserProfileRepository {
        override fun observeProfile(): Flow<UserProfile> = error("사용하지 않음")

        override suspend fun getProfile(): UserProfile = UserProfile()

        override suspend fun saveProfile(profile: UserProfile) = error("사용하지 않음")
    }

    private inner class FakeAiCredentialRepository(private val credential: AiCredential?) : AiCredentialRepository {
        override fun observeCredential(): Flow<AiCredential?> = error("사용하지 않음")

        override suspend fun getCredential(): AiCredential? = credential

        override suspend fun save(credential: AiCredential) = error("사용하지 않음")

        override suspend fun clear() = error("사용하지 않음")
    }

    private inner class FakeAiAnalysisRepository : AiAnalysisRepository {
        var analyzeWorkoutCallCount: Int = 0
            private set
        var lastRequest: WorkoutAnalysisRequest? = null
            private set
        var analyzeWorkoutFailure: Throwable? = null

        override suspend fun verifyCredential(credential: AiCredential) = error("사용하지 않음")

        override suspend fun analyzeDiet(
            request: kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest,
        ): AnalysisContent = error("사용하지 않음")

        override suspend fun summarizeDiet(
            request: kr.sdbk.bodyplan.core.domain.model.DietSummaryRequest,
        ): AnalysisContent = error("사용하지 않음")

        override suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent {
            analyzeWorkoutCallCount++
            lastRequest = request
            analyzeWorkoutFailure?.let { throw it }
            return content
        }

        override suspend fun analyzeInbody(request: InbodyAnalysisRequest): AnalysisContent = error("사용하지 않음")
    }

    private inner class FakeAnalysisResultRepository : AnalysisResultRepository {
        val saved: MutableMap<Pair<AnalysisKind, String>, AnalysisContent> = mutableMapOf()

        override fun observeLatest(
            kind: AnalysisKind,
            scopeKey: String,
        ): Flow<kr.sdbk.bodyplan.core.domain.model.AnalysisResult?> = error("사용하지 않음")

        override suspend fun getLatestOf(
            kind: AnalysisKind,
            scopeKeys: List<String>,
        ): Map<String, kr.sdbk.bodyplan.core.domain.model.AnalysisResult> = error("사용하지 않음")

        override fun observeHistory(
            kind: AnalysisKind,
        ): Flow<List<kr.sdbk.bodyplan.core.domain.model.AnalysisResult>> = error("사용하지 않음")

        override suspend fun save(
            kind: AnalysisKind,
            scopeKey: String,
            content: AnalysisContent,
            imageFileName: String?,
        ) {
            saved[kind to scopeKey] = content
        }
    }
}
