package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyRecord
import kr.sdbk.bodyplan.core.domain.model.InbodyRecordWithResult
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisRunner
import kr.sdbk.bodyplan.core.domain.repository.InbodyRecordRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RequestInbodyAnalysisUseCaseTest {
    private val credential = AiCredential(provider = AiProvider.CLAUDE, token = "token")

    @Test
    fun `AI 연결이 없으면 요청하지 않고 연결 없음 예외를 던진다`() = runTest {
        val runner = FakeAnalysisRunner()
        val useCase = RequestInbodyAnalysisUseCase(
            aiCredentialRepository = FakeAiCredentialRepository(null),
            inbodyRecordRepository = FakeInbodyRecordRepository(),
            analysisRunner = runner,
        )

        try {
            useCase(1L)
            fail("AiCredentialMissingException이 나야 한다")
        } catch (e: AiCredentialMissingException) {
            // 기대한 예외
        }

        assertTrue(runner.started.isEmpty())
    }

    @Test
    fun `분석을 요청하면 그 기록의 실패 사유를 지우고 기록 번호를 열쇠로 작업을 시작한다`() = runTest {
        val recordRepository = FakeInbodyRecordRepository()
        val runner = FakeAnalysisRunner()
        val useCase = RequestInbodyAnalysisUseCase(
            aiCredentialRepository = FakeAiCredentialRepository(credential),
            inbodyRecordRepository = recordRepository,
            analysisRunner = runner,
        )

        useCase(7L)

        assertEquals(listOf(7L to null), recordRepository.failureReasonCalls)
        val request = runner.started.single()
        assertEquals(AnalysisKind.INBODY, request.kind)
        assertEquals("7", request.scopeKey)
    }

    private class FakeAiCredentialRepository(private val credential: AiCredential?) : AiCredentialRepository {
        override fun observeCredential(): Flow<AiCredential?> = error("사용하지 않음")

        override suspend fun getCredential(): AiCredential? = credential

        override suspend fun save(credential: AiCredential) = error("사용하지 않음")

        override suspend fun clear() = error("사용하지 않음")
    }

    private class FakeInbodyRecordRepository : InbodyRecordRepository {
        val failureReasonCalls: MutableList<Pair<Long, String?>> = mutableListOf()

        override fun observeRecords(): Flow<List<InbodyRecordWithResult>> = error("사용하지 않음")

        override suspend fun getRecords(): List<InbodyRecordWithResult> = error("사용하지 않음")

        override suspend fun getRecord(id: Long): InbodyRecord? =
            InbodyRecord(id, "/path/$id.jpg", LocalDate.of(2026, 9, 1), 0L, failureReason = "이전 실패")

        override suspend fun insert(imageFileName: String, measuredOn: LocalDate): Long = error("사용하지 않음")

        override suspend fun setFailureReason(id: Long, reason: String?) {
            failureReasonCalls.add(id to reason)
        }
    }

    private class FakeAnalysisRunner : AnalysisRunner {
        val started: MutableList<AnalysisRunRequest> = mutableListOf()

        override suspend fun start(request: AnalysisRunRequest) {
            started.add(request)
        }

        override fun observe(kind: AnalysisKind, scopeKey: String): Flow<AnalysisRun?> = error("사용하지 않음")

        override fun observeAll(kind: AnalysisKind): Flow<List<AnalysisRun>> = error("사용하지 않음")
    }
}
