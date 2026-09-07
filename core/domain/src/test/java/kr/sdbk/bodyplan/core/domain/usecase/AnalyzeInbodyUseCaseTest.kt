package kr.sdbk.bodyplan.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysis
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.InbodyImageRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AnalyzeInbodyUseCaseTest {
    private val credential = AiCredential(provider = AiProvider.CLAUDE, token = "token")
    private val content = AnalysisContent(
        summary = "요약",
        sections = listOf(AnalysisSection(title = "체성분", body = "근육량")),
    )
    private val sourceUri = "content://picked/image.jpg"

    private fun useCaseWith(
        inbodyImageRepository: FakeInbodyImageRepository = FakeInbodyImageRepository(),
        analysisResultRepository: FakeAnalysisResultRepository = FakeAnalysisResultRepository(),
        aiAnalysisRepository: FakeAiAnalysisRepository = FakeAiAnalysisRepository(),
        userProfileRepository: FakeUserProfileRepository = FakeUserProfileRepository(),
        credential: AiCredential? = this.credential,
    ): AnalyzeInbodyUseCase = AnalyzeInbodyUseCase(
        userProfileRepository = userProfileRepository,
        aiCredentialRepository = FakeAiCredentialRepository(credential),
        aiAnalysisRepository = aiAnalysisRepository,
        analysisResultRepository = analysisResultRepository,
        inbodyImageRepository = inbodyImageRepository,
    )

    @Test
    fun `사진을 고르고 분석하면 사진이 먼저 복사되고 그 경로로 AI가 불리고 결과가 파일명과 함께 저장된다`() = runTest {
        val inbodyImageRepository = FakeInbodyImageRepository()
        val aiAnalysisRepository = FakeAiAnalysisRepository()
        val analysisResultRepository = FakeAnalysisResultRepository()
        val useCase = useCaseWith(
            inbodyImageRepository = inbodyImageRepository,
            aiAnalysisRepository = aiAnalysisRepository,
            analysisResultRepository = analysisResultRepository,
        )

        val result = useCase(sourceUri)

        assertEquals(content, result)
        assertEquals(listOf(sourceUri), inbodyImageRepository.saved)
        assertEquals(
            inbodyImageRepository.pathOf("saved-$sourceUri"),
            aiAnalysisRepository.lastRequest?.imagePath,
        )
        assertEquals(1, aiAnalysisRepository.analyzeInbodyCallCount)
        val savedEntry = analysisResultRepository.saved.entries.single()
        assertEquals(AnalysisKind.INBODY to "", savedEntry.key)
        assertEquals(content, savedEntry.value.content)
        assertEquals("saved-$sourceUri", savedEntry.value.imageFileName)
    }

    @Test
    fun `저장되는 결과의 kind는 INBODY이고 scopeKey는 빈 문자열이다`() = runTest {
        val analysisResultRepository = FakeAnalysisResultRepository()
        val useCase = useCaseWith(analysisResultRepository = analysisResultRepository)

        useCase(sourceUri)

        val key = analysisResultRepository.saved.keys.single()
        assertEquals(AnalysisKind.INBODY, key.first)
        assertEquals("", key.second)
    }

    @Test
    fun `분석이 실패하면 복사한 파일을 지우고 결과를 저장하지 않는다`() = runTest {
        val inbodyImageRepository = FakeInbodyImageRepository()
        val aiAnalysisRepository = FakeAiAnalysisRepository()
        aiAnalysisRepository.analyzeInbodyFailure = IllegalStateException("boom")
        val analysisResultRepository = FakeAnalysisResultRepository()
        val useCase = useCaseWith(
            inbodyImageRepository = inbodyImageRepository,
            aiAnalysisRepository = aiAnalysisRepository,
            analysisResultRepository = analysisResultRepository,
        )

        try {
            useCase(sourceUri)
            fail("예외가 그대로 나야 한다")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }

        assertEquals(inbodyImageRepository.saved, inbodyImageRepository.deleted)
        assertTrue(analysisResultRepository.saved.isEmpty())
    }

    @Test
    fun `토큰이 없으면 사진을 복사하지도 AI를 부르지도 않고 AiCredentialMissingException을 던진다`() = runTest {
        val inbodyImageRepository = FakeInbodyImageRepository()
        val aiAnalysisRepository = FakeAiAnalysisRepository()
        val analysisResultRepository = FakeAnalysisResultRepository()
        val useCase = useCaseWith(
            inbodyImageRepository = inbodyImageRepository,
            aiAnalysisRepository = aiAnalysisRepository,
            analysisResultRepository = analysisResultRepository,
            credential = null,
        )

        try {
            useCase(sourceUri)
            fail("AiCredentialMissingException이 나야 한다")
        } catch (e: AiCredentialMissingException) {
            // 기대한 결과
        }

        assertTrue(inbodyImageRepository.saved.isEmpty())
        assertEquals(0, aiAnalysisRepository.analyzeInbodyCallCount)
        assertTrue(analysisResultRepository.saved.isEmpty())
    }

    @Test
    fun `읽힌 체중과 키가 프로필에 반영된다`() = runTest {
        val userProfileRepository = FakeUserProfileRepository(UserProfile(heightCm = 170, weightKg = 60))
        val aiAnalysisRepository = FakeAiAnalysisRepository(
            measurement = InbodyMeasurement(weightKg = 65.4, heightCm = 172.0),
        )
        val useCase = useCaseWith(
            aiAnalysisRepository = aiAnalysisRepository,
            userProfileRepository = userProfileRepository,
        )

        useCase(sourceUri)

        assertEquals(65, userProfileRepository.savedProfile?.weightKg)
        assertEquals(172, userProfileRepository.savedProfile?.heightCm)
    }

    @Test
    fun `키를 못 읽으면 기존 키가 그대로 남고 체중도 같다`() = runTest {
        val userProfileRepository = FakeUserProfileRepository(UserProfile(heightCm = 170, weightKg = 60))
        val aiAnalysisRepository = FakeAiAnalysisRepository(
            measurement = InbodyMeasurement(weightKg = 65.0, heightCm = null),
        )
        val useCase = useCaseWith(
            aiAnalysisRepository = aiAnalysisRepository,
            userProfileRepository = userProfileRepository,
        )

        useCase(sourceUri)

        assertEquals(170, userProfileRepository.savedProfile?.heightCm)
        assertEquals(65, userProfileRepository.savedProfile?.weightKg)
    }

    @Test
    fun `측정값이 전부 비어 있으면 프로필을 저장하지 않는다`() = runTest {
        val userProfileRepository = FakeUserProfileRepository(UserProfile(heightCm = 170, weightKg = 60))
        val aiAnalysisRepository = FakeAiAnalysisRepository(measurement = InbodyMeasurement())
        val useCase = useCaseWith(
            aiAnalysisRepository = aiAnalysisRepository,
            userProfileRepository = userProfileRepository,
        )

        useCase(sourceUri)

        assertEquals(0, userProfileRepository.saveCount)
    }

    @Test
    fun `저장되는 결과에 측정값이 함께 담긴다`() = runTest {
        val measurement = InbodyMeasurement(weightKg = 65.0, heightCm = 172.0)
        val aiAnalysisRepository = FakeAiAnalysisRepository(measurement = measurement)
        val analysisResultRepository = FakeAnalysisResultRepository()
        val useCase = useCaseWith(
            aiAnalysisRepository = aiAnalysisRepository,
            analysisResultRepository = analysisResultRepository,
        )

        useCase(sourceUri)

        val savedEntry = analysisResultRepository.saved.entries.single()
        assertEquals(measurement, savedEntry.value.measurement)
    }

    private inner class FakeUserProfileRepository(private val initial: UserProfile = UserProfile()) :
        UserProfileRepository {
        var savedProfile: UserProfile? = null
            private set
        var saveCount: Int = 0
            private set

        override fun observeProfile(): Flow<UserProfile> = error("사용하지 않음")

        override suspend fun getProfile(): UserProfile = initial

        override suspend fun saveProfile(profile: UserProfile) {
            saveCount++
            savedProfile = profile
        }
    }

    private inner class FakeAiCredentialRepository(private val credential: AiCredential?) : AiCredentialRepository {
        override fun observeCredential(): Flow<AiCredential?> = error("사용하지 않음")

        override suspend fun getCredential(): AiCredential? = credential

        override suspend fun save(credential: AiCredential) = error("사용하지 않음")

        override suspend fun clear() = error("사용하지 않음")
    }

    private inner class FakeInbodyImageRepository : InbodyImageRepository {
        val saved: MutableList<String> = mutableListOf()
        val deleted: MutableList<String> = mutableListOf()

        override suspend fun save(sourceUri: String): String {
            saved.add(sourceUri)
            return "saved-$sourceUri"
        }

        override suspend fun delete(fileName: String) {
            deleted.add(fileName.removePrefix("saved-"))
        }

        override fun pathOf(fileName: String): String = "/path/$fileName"
    }

    private inner class FakeAiAnalysisRepository(private val measurement: InbodyMeasurement = InbodyMeasurement()) :
        AiAnalysisRepository {
        var analyzeInbodyCallCount: Int = 0
            private set
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
            analyzeInbodyCallCount++
            lastRequest = request
            analyzeInbodyFailure?.let { throw it }
            return InbodyAnalysis(content = content, measurement = measurement)
        }
    }

    private data class SavedAnalysis(
        val content: AnalysisContent,
        val imageFileName: String?,
        val measurement: InbodyMeasurement?,
    )

    private inner class FakeAnalysisResultRepository : AnalysisResultRepository {
        val saved: MutableMap<Pair<AnalysisKind, String>, SavedAnalysis> = mutableMapOf()

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
        ) {
            saved[kind to scopeKey] = SavedAnalysis(content, imageFileName, measurement)
        }
    }
}
