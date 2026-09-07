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
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.DietEntry
import kr.sdbk.bodyplan.core.domain.model.DietLog
import kr.sdbk.bodyplan.core.domain.model.DietSummaryRequest
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.DietLogRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AnalyzeDietUseCaseTest {
    private val credential = AiCredential(provider = AiProvider.CLAUDE, token = "token")
    private val content = AnalysisContent(
        summary = "요약",
        sections = listOf(AnalysisSection(title = "먹은 음식", body = "밥")),
    )

    private fun useCaseWith(
        dietLogRepository: FakeDietLogRepository = FakeDietLogRepository(),
        analysisResultRepository: FakeAnalysisResultRepository = FakeAnalysisResultRepository(),
        aiAnalysisRepository: FakeAiAnalysisRepository = FakeAiAnalysisRepository(),
        credential: AiCredential? = this.credential,
    ): Triple<AnalyzeDietUseCase, FakeAiAnalysisRepository, FakeAnalysisResultRepository> {
        val useCase = AnalyzeDietUseCase(
            dietLogRepository = dietLogRepository,
            userProfileRepository = FakeUserProfileRepository(),
            aiCredentialRepository = FakeAiCredentialRepository(credential),
            aiAnalysisRepository = aiAnalysisRepository,
            analysisResultRepository = analysisResultRepository,
        )
        return Triple(useCase, aiAnalysisRepository, analysisResultRepository)
    }

    @Test
    fun `날짜별 분석은 그 날의 기록을 모아 AI를 부르고 결과를 저장한다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val dietLogRepository = FakeDietLogRepository(
            mapOf(date to listOf(DietEntry(id = 1L, imagePath = "/a.jpg", memo = "메모"))),
        )
        val (useCase, aiAnalysisRepository, analysisResultRepository) = useCaseWith(
            dietLogRepository = dietLogRepository,
        )

        val result = useCase(
            kind = AnalysisKind.DIET_DAILY,
            scopeKey = AnalysisScopeKey.daily(date),
            periodLabel = "9월 7일",
            from = date,
            to = date,
        )

        assertEquals(content, result)
        assertEquals(1, aiAnalysisRepository.analyzeDietCallCount)
        assertEquals(listOf(date), aiAnalysisRepository.lastAnalyzeDietRequest?.entries?.map { it.date })
        assertEquals(
            content,
            analysisResultRepository.saved[AnalysisKind.DIET_DAILY to AnalysisScopeKey.daily(date)],
        )
    }

    @Test
    fun `기록이 없는 날을 분석하면 AI를 부르지 않고 NoRecordToAnalyzeException을 던진다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val (useCase, aiAnalysisRepository, _) = useCaseWith(dietLogRepository = FakeDietLogRepository(emptyMap()))

        try {
            useCase(
                kind = AnalysisKind.DIET_DAILY,
                scopeKey = AnalysisScopeKey.daily(date),
                periodLabel = "9월 7일",
                from = date,
                to = date,
            )
            fail("NoRecordToAnalyzeException이 나야 한다")
        } catch (e: NoRecordToAnalyzeException) {
            // 기대한 결과
        }

        assertEquals(0, aiAnalysisRepository.analyzeDietCallCount)
    }

    @Test
    fun `주간 종합은 저장된 날짜별 분석을 모아 AI를 부르고 사진을 다시 보내지 않는다`() = runTest {
        val monday = LocalDate.of(2026, 9, 7)
        val sunday = monday.plusDays(6)
        val dailyContent = content.copy(summary = "하루 요약")
        val analysisResultRepository = FakeAnalysisResultRepository(
            preloaded = mapOf(
                AnalysisScopeKey.daily(monday) to
                    AnalysisResult(1L, AnalysisKind.DIET_DAILY, AnalysisScopeKey.daily(monday), dailyContent, 0L),
            ),
        )
        val dietLogRepository = FakeDietLogRepository()
        val (useCase, aiAnalysisRepository, _) = useCaseWith(
            dietLogRepository = dietLogRepository,
            analysisResultRepository = analysisResultRepository,
        )

        val result = useCase(
            kind = AnalysisKind.DIET_WEEKLY,
            scopeKey = AnalysisScopeKey.weekly(monday),
            periodLabel = "이번 주",
            from = monday,
            to = sunday,
        )

        assertEquals(content, result)
        assertEquals(1, aiAnalysisRepository.summarizeDietCallCount)
        assertEquals(listOf(monday), aiAnalysisRepository.lastSummarizeDietRequest?.dailyResults?.map { it.date })
        assertEquals(0, dietLogRepository.observeLogCallCount)
    }

    @Test
    fun `기간에 날짜별 분석이 하나도 없으면 AI를 부르지 않고 NoDailyAnalysisException을 던진다`() = runTest {
        val monday = LocalDate.of(2026, 9, 7)
        val sunday = monday.plusDays(6)
        val (useCase, aiAnalysisRepository, _) = useCaseWith(
            analysisResultRepository = FakeAnalysisResultRepository(preloaded = emptyMap()),
        )

        try {
            useCase(
                kind = AnalysisKind.DIET_WEEKLY,
                scopeKey = AnalysisScopeKey.weekly(monday),
                periodLabel = "이번 주",
                from = monday,
                to = sunday,
            )
            fail("NoDailyAnalysisException이 나야 한다")
        } catch (e: NoDailyAnalysisException) {
            // 기대한 결과
        }

        assertEquals(0, aiAnalysisRepository.summarizeDietCallCount)
    }

    @Test
    fun `토큰이 없으면 AI를 부르지 않고 AiCredentialMissingException을 던진다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val dietLogRepository = FakeDietLogRepository(
            mapOf(date to listOf(DietEntry(id = 1L, imagePath = "/a.jpg", memo = null))),
        )
        val (useCase, aiAnalysisRepository, analysisResultRepository) = useCaseWith(
            dietLogRepository = dietLogRepository,
            credential = null,
        )

        try {
            useCase(
                kind = AnalysisKind.DIET_DAILY,
                scopeKey = AnalysisScopeKey.daily(date),
                periodLabel = "9월 7일",
                from = date,
                to = date,
            )
            fail("AiCredentialMissingException이 나야 한다")
        } catch (e: AiCredentialMissingException) {
            // 기대한 결과
        }

        assertEquals(0, aiAnalysisRepository.analyzeDietCallCount)
        assertTrue(analysisResultRepository.saved.isEmpty())
    }

    @Test
    fun `호출이 실패하면 결과를 저장하지 않고 예외를 그대로 던진다`() = runTest {
        val date = LocalDate.of(2026, 9, 7)
        val dietLogRepository = FakeDietLogRepository(
            mapOf(date to listOf(DietEntry(id = 1L, imagePath = "/a.jpg", memo = null))),
        )
        val aiAnalysisRepository = FakeAiAnalysisRepository()
        aiAnalysisRepository.analyzeDietFailure = IllegalStateException("boom")
        val (useCase, _, analysisResultRepository) = useCaseWith(
            dietLogRepository = dietLogRepository,
            aiAnalysisRepository = aiAnalysisRepository,
        )

        try {
            useCase(
                kind = AnalysisKind.DIET_DAILY,
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

    private inner class FakeDietLogRepository(private val logsByDate: Map<LocalDate, List<DietEntry>> = emptyMap()) :
        DietLogRepository {
        var observeLogCallCount: Int = 0
            private set

        override fun observeLog(date: LocalDate): Flow<DietLog> {
            observeLogCallCount++
            return flowOf(DietLog(date = date, entries = logsByDate[date].orEmpty()))
        }

        override fun observeFirstImageInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, String>> =
            error("사용하지 않음")

        override suspend fun getEntry(id: Long): DietEntry? = error("사용하지 않음")

        override suspend fun addEntry(date: LocalDate, sourceImageUri: String, memo: String?): Long = error("사용하지 않음")

        override suspend fun updateEntry(entryId: Long, sourceImageUri: String?, memo: String?) {
            error("사용하지 않음")
        }

        override suspend fun exportEntryImage(id: Long) {
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
        var analyzeDietCallCount: Int = 0
            private set
        var summarizeDietCallCount: Int = 0
            private set
        var lastAnalyzeDietRequest: DietAnalysisRequest? = null
            private set
        var lastSummarizeDietRequest: DietSummaryRequest? = null
            private set
        var analyzeDietFailure: Throwable? = null
        var summarizeDietFailure: Throwable? = null

        override suspend fun verifyCredential(credential: AiCredential) = error("사용하지 않음")

        override suspend fun analyzeDiet(request: DietAnalysisRequest): AnalysisContent {
            analyzeDietCallCount++
            lastAnalyzeDietRequest = request
            analyzeDietFailure?.let { throw it }
            return content
        }

        override suspend fun summarizeDiet(request: DietSummaryRequest): AnalysisContent {
            summarizeDietCallCount++
            lastSummarizeDietRequest = request
            summarizeDietFailure?.let { throw it }
            return content
        }

        override suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent = error("사용하지 않음")
    }

    private inner class FakeAnalysisResultRepository(private val preloaded: Map<String, AnalysisResult> = emptyMap()) :
        AnalysisResultRepository {
        val saved: MutableMap<Pair<AnalysisKind, String>, AnalysisContent> = mutableMapOf()

        override fun observeLatest(kind: AnalysisKind, scopeKey: String): Flow<AnalysisResult?> = error("사용하지 않음")

        override suspend fun getLatestOf(kind: AnalysisKind, scopeKeys: List<String>): Map<String, AnalysisResult> =
            preloaded.filterKeys { it in scopeKeys }

        override suspend fun save(kind: AnalysisKind, scopeKey: String, content: AnalysisContent) {
            saved[kind to scopeKey] = content
        }
    }
}
