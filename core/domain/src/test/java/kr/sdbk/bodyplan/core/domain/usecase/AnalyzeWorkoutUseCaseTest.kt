package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.domain.model.AnalysisSummaryRequest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysis
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
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

    private fun useCaseWith(
        workoutLogRepository: FakeWorkoutLogRepository = FakeWorkoutLogRepository(),
        analysisResultRepository: FakeAnalysisResultRepository = FakeAnalysisResultRepository(),
        aiAnalysisRepository: FakeAiAnalysisRepository = FakeAiAnalysisRepository(),
        credential: AiCredential? = this.credential,
    ): Triple<AnalyzeWorkoutUseCase, FakeAiAnalysisRepository, FakeAnalysisResultRepository> {
        val useCase = AnalyzeWorkoutUseCase(
            workoutLogRepository = workoutLogRepository,
            userProfileRepository = FakeUserProfileRepository(),
            aiCredentialRepository = FakeAiCredentialRepository(credential),
            aiAnalysisRepository = aiAnalysisRepository,
            analysisResultRepository = analysisResultRepository,
            children = AnalysisChildren(analysisResultRepository),
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
                is Intensity.Duration -> IntensityType.DURATION
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
    fun `날짜별 분석의 workoutDayCount는 1이고 restDayCount는 0이다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(date to listOf(entry())))
        val (useCase, aiAnalysisRepository, _) = useCaseWith(workoutLogRepository = workoutLogRepository)

        useCase(
            kind = AnalysisKind.WORKOUT_DAILY,
            scopeKey = AnalysisScopeKey.daily(date),
            periodLabel = "9월 7일",
            from = date,
            to = date,
        )

        val request = aiAnalysisRepository.lastRequest!!
        assertEquals(1, request.workoutDayCount)
        assertEquals(0, request.restDayCount)
    }

    @Test
    fun `기록이 없는 날을 분석하면 AI를 부르지 않고 NoRecordToAnalyzeException을 던진다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val (useCase, aiAnalysisRepository, _) = useCaseWith(
            workoutLogRepository = FakeWorkoutLogRepository(emptyMap()),
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
            sets = listOf(WorkoutSet(repeatCount = 15, intensity = Intensity.Angle(45))),
        )
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(date to listOf(weightEntry, angleEntry)))
        val (useCase, aiAnalysisRepository, _) = useCaseWith(workoutLogRepository = workoutLogRepository)

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
    fun `유산소만 있는 기록은 총 시간만 채우고 볼륨과 횟수는 0이다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val cardioEntry = entry(
            bodyPart = BodyPart.CARDIO,
            exerciseName = "러닝",
            sets = listOf(
                WorkoutSet(repeatCount = 1, intensity = Intensity.Duration(30)),
                WorkoutSet(repeatCount = 1, intensity = Intensity.Duration(20)),
            ),
        )
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(date to listOf(cardioEntry)))
        val (useCase, aiAnalysisRepository, _) = useCaseWith(workoutLogRepository = workoutLogRepository)

        useCase(
            kind = AnalysisKind.WORKOUT_DAILY,
            scopeKey = AnalysisScopeKey.daily(date),
            periodLabel = "9월 7일",
            from = date,
            to = date,
        )

        val request = aiAnalysisRepository.lastRequest!!
        assertEquals(50, request.totalCardioMinutes)
        assertEquals(0, request.totalWeightVolume)
        assertEquals(0, request.totalBodyweightReps)
    }

    @Test
    fun `무게 각도 유산소가 섞인 기록에서 셋이 각각 제 축으로만 집계된다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val weightEntry = entry(
            bodyPart = BodyPart.CHEST,
            exerciseName = "벤치프레스",
            sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(60))),
        )
        val angleEntry = entry(
            bodyPart = BodyPart.LEG,
            exerciseName = "레그레이즈",
            sets = listOf(WorkoutSet(repeatCount = 15, intensity = Intensity.Angle(45))),
        )
        val cardioEntry = entry(
            bodyPart = BodyPart.CARDIO,
            exerciseName = "러닝",
            sets = listOf(WorkoutSet(repeatCount = 1, intensity = Intensity.Duration(30))),
        )
        val workoutLogRepository = FakeWorkoutLogRepository(
            mapOf(date to listOf(weightEntry, angleEntry, cardioEntry)),
        )
        val (useCase, aiAnalysisRepository, _) = useCaseWith(workoutLogRepository = workoutLogRepository)

        useCase(
            kind = AnalysisKind.WORKOUT_DAILY,
            scopeKey = AnalysisScopeKey.daily(date),
            periodLabel = "9월 7일",
            from = date,
            to = date,
        )

        val request = aiAnalysisRepository.lastRequest!!
        assertEquals(600, request.totalWeightVolume)
        assertEquals(15, request.totalBodyweightReps)
        assertEquals(30, request.totalCardioMinutes)
    }

    @Test
    fun `유산소 세트의 반복 횟수가 몇이든 시간에는 곱해지지 않는다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val cardioEntry = entry(
            bodyPart = BodyPart.CARDIO,
            exerciseName = "러닝",
            sets = listOf(WorkoutSet(repeatCount = 7, intensity = Intensity.Duration(30))),
        )
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(date to listOf(cardioEntry)))
        val (useCase, aiAnalysisRepository, _) = useCaseWith(workoutLogRepository = workoutLogRepository)

        useCase(
            kind = AnalysisKind.WORKOUT_DAILY,
            scopeKey = AnalysisScopeKey.daily(date),
            periodLabel = "9월 7일",
            from = date,
            to = date,
        )

        // 30분짜리 세트가 repeatCount=7이어도 곱해지지 않고 30분 그대로다.
        assertEquals(30, aiAnalysisRepository.lastRequest?.totalCardioMinutes)
    }

    @Test
    fun `주간 분석은 저장된 날짜별 분석을 모아 AI를 부르고 기록 원문을 다시 보내지 않는다`() = runTest {
        val monday = LocalDate.of(2026, 9, 7)
        val sunday = monday.plusDays(6)
        val dailyContent = content.copy(summary = "하루 요약")
        val analysisResultRepository = FakeAnalysisResultRepository(
            preloaded = mapOf(
                AnalysisScopeKey.daily(monday) to
                    AnalysisResult(1L, AnalysisKind.WORKOUT_DAILY, AnalysisScopeKey.daily(monday), dailyContent, 0L),
            ),
        )
        val workoutLogRepository = FakeWorkoutLogRepository()
        val (useCase, aiAnalysisRepository, _) = useCaseWith(
            workoutLogRepository = workoutLogRepository,
            analysisResultRepository = analysisResultRepository,
        )

        val result = useCase(
            kind = AnalysisKind.WORKOUT_WEEKLY,
            scopeKey = AnalysisScopeKey.weekly(monday),
            periodLabel = "이번 주",
            from = monday,
            to = sunday,
        )

        assertEquals(content, result)
        assertEquals(1, aiAnalysisRepository.summarizeCallCount)
        assertEquals(AnalysisKind.WORKOUT_WEEKLY, aiAnalysisRepository.lastSummaryRequest?.kind)
        assertEquals(
            listOf(dailyContent),
            aiAnalysisRepository.lastSummaryRequest?.children?.map { it.content },
        )
        assertEquals(0, workoutLogRepository.observedDates.size)
    }

    @Test
    fun `주간 분석에 날짜별 분석이 하나도 없으면 NoChildAnalysisException을 던지고 안내 문구에 담긴다`() = runTest {
        val monday = LocalDate.of(2026, 9, 7)
        val sunday = monday.plusDays(6)
        val (useCase, aiAnalysisRepository, _) = useCaseWith(
            analysisResultRepository = FakeAnalysisResultRepository(preloaded = emptyMap()),
        )

        try {
            useCase(
                kind = AnalysisKind.WORKOUT_WEEKLY,
                scopeKey = AnalysisScopeKey.weekly(monday),
                periodLabel = "이번 주",
                from = monday,
                to = sunday,
            )
            fail("NoChildAnalysisException이 나야 한다")
        } catch (e: NoChildAnalysisException) {
            assertEquals("날짜별", e.childLabel)
            assertTrue(e.message!!.contains("날짜별"))
        }

        assertEquals(0, aiAnalysisRepository.summarizeCallCount)
    }

    @Test
    fun `월 분석은 온전한 주의 주 분석과 잘린 주의 날짜 분석을 섞어 모은다`() = runTest {
        val anyDateInMonth = LocalDate.of(2026, 9, 15)
        val leadingDayContent = content.copy(summary = "9월 1일 요약")
        val weekContent = content.copy(summary = "주간 요약")
        val trailingDayContent = content.copy(summary = "9월 30일 요약")
        val analysisResultRepository = FakeAnalysisResultRepository(
            preloaded = mapOf(
                AnalysisScopeKey.daily(LocalDate.of(2026, 9, 1)) to AnalysisResult(
                    1L,
                    AnalysisKind.WORKOUT_DAILY,
                    AnalysisScopeKey.daily(LocalDate.of(2026, 9, 1)),
                    leadingDayContent,
                    0L,
                ),
                AnalysisScopeKey.weekly(LocalDate.of(2026, 9, 7)) to AnalysisResult(
                    2L,
                    AnalysisKind.WORKOUT_WEEKLY,
                    AnalysisScopeKey.weekly(LocalDate.of(2026, 9, 7)),
                    weekContent,
                    0L,
                ),
                AnalysisScopeKey.daily(LocalDate.of(2026, 9, 30)) to AnalysisResult(
                    3L,
                    AnalysisKind.WORKOUT_DAILY,
                    AnalysisScopeKey.daily(LocalDate.of(2026, 9, 30)),
                    trailingDayContent,
                    0L,
                ),
            ),
        )
        val (useCase, aiAnalysisRepository, _) = useCaseWith(analysisResultRepository = analysisResultRepository)

        useCase(
            kind = AnalysisKind.WORKOUT_MONTHLY,
            scopeKey = AnalysisScopeKey.monthly(anyDateInMonth),
            periodLabel = "9월",
            from = LocalDate.of(2026, 9, 1),
            to = LocalDate.of(2026, 9, 30),
        )

        val request = aiAnalysisRepository.lastSummaryRequest!!
        assertEquals(AnalysisKind.WORKOUT_MONTHLY, request.kind)
        assertEquals(
            setOf(leadingDayContent, weekContent, trailingDayContent),
            request.children.map { it.content }.toSet(),
        )
        assertEquals(3, request.children.size)
    }

    @Test
    fun `월 분석에 주간 날짜별 분석이 하나도 없으면 NoChildAnalysisException을 던진다`() = runTest {
        val anyDateInMonth = LocalDate.of(2026, 9, 15)
        val (useCase, aiAnalysisRepository, _) = useCaseWith(
            analysisResultRepository = FakeAnalysisResultRepository(preloaded = emptyMap()),
        )

        try {
            useCase(
                kind = AnalysisKind.WORKOUT_MONTHLY,
                scopeKey = AnalysisScopeKey.monthly(anyDateInMonth),
                periodLabel = "9월",
                from = LocalDate.of(2026, 9, 1),
                to = LocalDate.of(2026, 9, 30),
            )
            fail("NoChildAnalysisException이 나야 한다")
        } catch (e: NoChildAnalysisException) {
            assertEquals("주간", e.childLabel)
        }

        assertEquals(0, aiAnalysisRepository.summarizeCallCount)
    }

    @Test
    fun `토큰이 없으면 AI를 부르지 않고 AiCredentialMissingException을 던진다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val workoutLogRepository = FakeWorkoutLogRepository(mapOf(date to listOf(entry())))
        val (useCase, aiAnalysisRepository, analysisResultRepository) = useCaseWith(
            workoutLogRepository = workoutLogRepository,
            credential = null,
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

        override fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>> =
            error("사용하지 않음")

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
        var summarizeCallCount: Int = 0
            private set
        var lastRequest: WorkoutAnalysisRequest? = null
            private set
        var lastSummaryRequest: AnalysisSummaryRequest? = null
            private set
        var analyzeWorkoutFailure: Throwable? = null

        override suspend fun verifyCredential(credential: AiCredential) = error("사용하지 않음")

        override suspend fun analyzeDiet(request: DietAnalysisRequest): AnalysisContent = error("사용하지 않음")

        override suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent {
            analyzeWorkoutCallCount++
            lastRequest = request
            analyzeWorkoutFailure?.let { throw it }
            return content
        }

        override suspend fun summarize(request: AnalysisSummaryRequest): AnalysisContent {
            summarizeCallCount++
            lastSummaryRequest = request
            return content
        }

        override suspend fun analyzeInbody(request: InbodyAnalysisRequest): InbodyAnalysis = error("사용하지 않음")
    }

    private inner class FakeAnalysisResultRepository(private val preloaded: Map<String, AnalysisResult> = emptyMap()) :
        AnalysisResultRepository {
        val saved: MutableMap<Pair<AnalysisKind, String>, AnalysisContent> = mutableMapOf()

        override fun observeLatest(kind: AnalysisKind, scopeKey: String): Flow<AnalysisResult?> = error("사용하지 않음")

        override suspend fun getLatestOf(kind: AnalysisKind, scopeKeys: List<String>): Map<String, AnalysisResult> =
            preloaded.filterKeys { it in scopeKeys }

        override fun observeHistory(kind: AnalysisKind): Flow<List<AnalysisResult>> = error("사용하지 않음")

        override suspend fun save(
            kind: AnalysisKind,
            scopeKey: String,
            content: AnalysisContent,
            imageFileName: String?,
            measurement: InbodyMeasurement?,
        ) {
            saved[kind to scopeKey] = content
        }
    }
}
