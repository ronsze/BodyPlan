package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.domain.model.AnalysisStage
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysis
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.InbodyRecord
import kr.sdbk.bodyplan.core.domain.model.InbodyRecordWithResult
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.InbodyRecordRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AnalyzeInbodyUseCaseTest {
    private val credential = AiCredential(provider = AiProvider.CLAUDE, token = "token")
    private val content = AnalysisContent(
        summary = "요약",
        sections = listOf(AnalysisSection(title = "체성분", body = "근육량")),
    )
    private val oldContent = AnalysisContent(summary = "이전", sections = emptyList())
    private val sep1 = LocalDate.of(2026, 9, 1)
    private val sep8 = LocalDate.of(2026, 9, 8)
    private val initialProfile = UserProfile(heightCm = 170, weightKg = 60)

    private fun oldResult(recordId: Long, measurement: InbodyMeasurement? = InbodyMeasurement(weightKg = 65.0)) =
        AnalysisResult(
            id = 100 + recordId,
            kind = AnalysisKind.INBODY,
            scopeKey = recordId.toString(),
            content = oldContent,
            createdAtMillis = 0L,
            measurement = measurement,
        )

    private inner class Env(
        val resultRepository: FakeAnalysisResultRepository,
        val recordRepository: FakeInbodyRecordRepository,
        val aiRepository: FakeAiAnalysisRepository,
        val profileRepository: FakeUserProfileRepository,
        val useCase: AnalyzeInbodyUseCase,
    )

    private fun env(
        records: List<InbodyRecord>,
        existing: List<AnalysisResult> = emptyList(),
        measurements: Map<String, InbodyMeasurement> = emptyMap(),
        defaultMeasurement: InbodyMeasurement = InbodyMeasurement(),
    ): Env {
        val resultRepository = FakeAnalysisResultRepository(existing.toMutableList())
        val recordRepository = FakeInbodyRecordRepository(records, resultRepository)
        val aiRepository = FakeAiAnalysisRepository(measurements, defaultMeasurement)
        val profileRepository = FakeUserProfileRepository(initialProfile)
        return Env(
            resultRepository,
            recordRepository,
            aiRepository,
            profileRepository,
            AnalyzeInbodyUseCase(
                userProfileRepository = profileRepository,
                aiCredentialRepository = FakeAiCredentialRepository(credential),
                aiAnalysisRepository = aiRepository,
                analysisResultRepository = resultRepository,
                inbodyRecordRepository = recordRepository,
            ),
        )
    }

    @Test
    fun `성공하면 그 기록의 결과를 교체한다`() = runTest {
        val measurement = InbodyMeasurement(weightKg = 72.4)
        val e = env(
            records = listOf(inbodyRecord(1, sep1)),
            existing = listOf(oldResult(1)),
            defaultMeasurement = measurement,
        )

        val returned = e.useCase(1L)

        assertEquals(content, returned)
        val results = e.resultRepository.results.filter { it.scopeKey == "1" }
        assertEquals(1, results.size)
        assertEquals(content, results.single().content)
        assertEquals(measurement, results.single().measurement)
    }

    @Test
    fun `측정일이 가장 늦은 기록이면 읽힌 키와 체중을 프로필에 반영한다`() = runTest {
        val e = env(
            records = listOf(inbodyRecord(1, sep1), inbodyRecord(2, sep8)),
            existing = listOf(oldResult(1)),
            defaultMeasurement = InbodyMeasurement(weightKg = 72.4, heightCm = 175.4),
        )

        e.useCase(2L)

        assertEquals(175, e.profileRepository.profile.heightCm)
        assertEquals(72, e.profileRepository.profile.weightKg)
    }

    @Test
    fun `측정일이 가장 늦지 않은 기록을 분석하면 프로필을 바꾸지 않는다`() = runTest {
        val e = env(
            records = listOf(inbodyRecord(1, sep1), inbodyRecord(2, sep8)),
            existing = listOf(oldResult(2)),
            defaultMeasurement = InbodyMeasurement(weightKg = 72.4, heightCm = 175.4),
        )

        e.useCase(1L)

        assertEquals(initialProfile, e.profileRepository.profile)
        assertEquals(0, e.profileRepository.saveCount)
    }

    @Test
    fun `읽히지 않은 값은 프로필을 그대로 둔다`() = runTest {
        val e = env(
            records = listOf(inbodyRecord(1, sep1)),
            defaultMeasurement = InbodyMeasurement(weightKg = 72.4),
        )

        e.useCase(1L)

        assertEquals(170, e.profileRepository.profile.heightCm)
        assertEquals(72, e.profileRepository.profile.weightKg)
    }

    @Test
    fun `읽힌 값이 하나도 없으면 프로필을 바꾸지 않는다`() = runTest {
        val e = env(records = listOf(inbodyRecord(1, sep1)), defaultMeasurement = InbodyMeasurement())

        e.useCase(1L)

        assertEquals(initialProfile, e.profileRepository.profile)
        assertEquals(0, e.profileRepository.saveCount)
    }

    @Test
    fun `두 기록을 차례로 분석해도 최종 프로필은 가장 최근 측정 기록의 값이다`() = runTest {
        val e = env(
            records = listOf(inbodyRecord(1, sep1), inbodyRecord(2, sep8)),
            measurements = mapOf(
                "/path/2.jpg" to InbodyMeasurement(weightKg = 70.0),
                "/path/1.jpg" to InbodyMeasurement(weightKg = 68.0),
            ),
        )

        e.useCase(2L)
        e.useCase(1L)

        assertEquals(70, e.profileRepository.profile.weightKg)
    }

    @Test
    fun `분석이 실패하면 이전 결과가 그대로 남는다`() = runTest {
        val previous = oldResult(1)
        val e = env(records = listOf(inbodyRecord(1, sep1)), existing = listOf(previous))
        e.aiRepository.analyzeInbodyFailure = IllegalStateException("boom")

        try {
            e.useCase(1L)
            fail("예외가 그대로 나야 한다")
        } catch (ex: IllegalStateException) {
            assertEquals("boom", ex.message)
        }

        assertEquals(0, e.resultRepository.replaceCallCount)
        assertEquals(listOf(previous), e.resultRepository.results)
    }

    @Test
    fun `결과 교체 저장이 실패하면 이전 결과가 그대로 남는다`() = runTest {
        val previous = oldResult(1)
        val e = env(
            records = listOf(inbodyRecord(1, sep1)),
            existing = listOf(previous),
            defaultMeasurement = InbodyMeasurement(weightKg = 72.4),
        )
        e.resultRepository.replaceFailure = IllegalStateException("db")

        try {
            e.useCase(1L)
        } catch (ex: Exception) {
            // 저장 실패는 호출자에게 전달될 수 있다
        }

        assertEquals(1, e.resultRepository.replaceCallCount)
        assertEquals(listOf(previous), e.resultRepository.results)
    }

    @Test
    fun `AI 호출에는 기록의 사진 경로를 쓰고 단계가 차례로 보고된다`() = runTest {
        val e = env(records = listOf(inbodyRecord(1, sep1)))
        val stages = mutableListOf<AnalysisStage>()

        e.useCase(1L) { stages.add(it) }

        assertEquals("/path/1.jpg", e.aiRepository.lastRequest?.imagePath)
        assertEquals(
            listOf(
                AnalysisStage.COLLECTING,
                AnalysisStage.PREPARING_IMAGES,
                AnalysisStage.CALLING,
                AnalysisStage.PARSING,
            ),
            stages,
        )
    }

    private class FakeUserProfileRepository(initial: UserProfile) : UserProfileRepository {
        var profile: UserProfile = initial
            private set
        var saveCount: Int = 0
            private set

        override fun observeProfile(): Flow<UserProfile> = error("사용하지 않음")

        override suspend fun getProfile(): UserProfile = profile

        override suspend fun saveProfile(profile: UserProfile) {
            saveCount++
            this.profile = profile
        }
    }

    private class FakeAiCredentialRepository(private val credential: AiCredential?) : AiCredentialRepository {
        override fun observeCredential(): Flow<AiCredential?> = error("사용하지 않음")

        override suspend fun getCredential(): AiCredential? = credential

        override suspend fun save(credential: AiCredential) = error("사용하지 않음")

        override suspend fun clear() = error("사용하지 않음")
    }

    private class FakeInbodyRecordRepository(
        private val records: List<InbodyRecord>,
        private val resultRepository: FakeAnalysisResultRepository,
    ) : InbodyRecordRepository {
        override fun observeRecords(): Flow<List<InbodyRecordWithResult>> = error("사용하지 않음")

        override suspend fun getRecords(): List<InbodyRecordWithResult> = records.map { record ->
            InbodyRecordWithResult(record, resultRepository.results.firstOrNull { it.scopeKey == record.id.toString() })
        }

        override suspend fun getRecord(id: Long): InbodyRecord? = records.firstOrNull { it.id == id }

        override suspend fun insert(imageFileName: String, measuredOn: LocalDate): Long = error("사용하지 않음")

        override suspend fun setFailureReason(id: Long, reason: String?) = error("사용하지 않음")
    }

    private class FakeAnalysisResultRepository(val results: MutableList<AnalysisResult>) : AnalysisResultRepository {
        var replaceCallCount: Int = 0
            private set
        var replaceFailure: Throwable? = null
        private var nextId = 1000L

        override fun observeLatest(kind: AnalysisKind, scopeKey: String): Flow<AnalysisResult?> = error("사용하지 않음")

        override suspend fun getLatestOf(kind: AnalysisKind, scopeKeys: List<String>): Map<String, AnalysisResult> =
            error("사용하지 않음")

        override fun observeHistory(kind: AnalysisKind): Flow<List<AnalysisResult>> = error("사용하지 않음")

        override suspend fun save(
            kind: AnalysisKind,
            scopeKey: String,
            content: AnalysisContent,
            imageFileName: String?,
            measurement: InbodyMeasurement?,
        ) = error("사용하지 않음")

        override suspend fun replace(
            kind: AnalysisKind,
            scopeKey: String,
            content: AnalysisContent,
            measurement: InbodyMeasurement?,
        ) {
            replaceCallCount++
            replaceFailure?.let { throw it }
            results.removeAll { it.kind == kind && it.scopeKey == scopeKey }
            results.add(AnalysisResult(nextId++, kind, scopeKey, content, 0L, null, measurement))
        }
    }

    private inner class FakeAiAnalysisRepository(
        private val measurements: Map<String, InbodyMeasurement>,
        private val defaultMeasurement: InbodyMeasurement,
    ) : AiAnalysisRepository {
        var lastRequest: InbodyAnalysisRequest? = null
            private set
        var analyzeInbodyFailure: Throwable? = null

        override suspend fun verifyCredential(credential: AiCredential) = error("사용하지 않음")

        override suspend fun analyzeDiet(
            request: kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest,
        ): AnalysisContent = error("사용하지 않음")

        override suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent = error("사용하지 않음")

        override suspend fun summarize(
            request: kr.sdbk.bodyplan.core.domain.model.AnalysisSummaryRequest,
        ): AnalysisContent = error("사용하지 않음")

        override suspend fun analyzeInbody(request: InbodyAnalysisRequest): InbodyAnalysis {
            lastRequest = request
            analyzeInbodyFailure?.let { throw it }
            return InbodyAnalysis(
                content = content,
                measurement = measurements[request.imagePath] ?: defaultMeasurement,
            )
        }
    }
}
