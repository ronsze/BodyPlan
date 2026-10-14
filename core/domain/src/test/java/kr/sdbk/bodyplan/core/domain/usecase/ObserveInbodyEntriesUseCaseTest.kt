package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunRequest
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunState
import kr.sdbk.bodyplan.core.domain.model.InbodyEntry
import kr.sdbk.bodyplan.core.domain.model.InbodyEntryStatus
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.InbodyRecord
import kr.sdbk.bodyplan.core.domain.model.InbodyRecordWithResult
import kr.sdbk.bodyplan.core.domain.repository.AnalysisRunner
import kr.sdbk.bodyplan.core.domain.repository.InbodyRecordRepository
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveInbodyEntriesUseCaseTest {
    private val sep1 = LocalDate.of(2026, 9, 1)
    private val running = AnalysisRun(AnalysisKind.INBODY, "1", AnalysisRunState.RUNNING)

    @Test
    fun `기록마다 작업 상태가 따로이고 한 기록이 분석 중이어도 다른 기록은 영향을 받지 않는다`() = runTest {
        val records = listOf(
            inbodyRecordWithResult(1, sep1, null),
            inbodyRecordWithResult(2, sep1, null),
        )
        val useCase = ObserveInbodyEntriesUseCase(
            FakeInbodyRecordRepository(flowOf(records)),
            FakeAnalysisRunner(MutableStateFlow(listOf(running))),
        )

        val entries = useCase().first().associateBy { it.record.id }

        assertEquals(InbodyEntryStatus.RUNNING, entries.getValue(1).status)
        assertEquals(InbodyEntryStatus.NOT_ANALYZED, entries.getValue(2).status)
    }

    @Test
    fun `목록은 측정 순서 내림차순이다`() = runTest {
        val records = listOf(
            inbodyRecordWithResult(1, LocalDate.of(2026, 9, 8), null),
            inbodyRecordWithResult(2, sep1, null),
            inbodyRecordWithResult(3, LocalDate.of(2026, 9, 20), InbodyMeasurement(weightKg = 70.0)),
        )
        val useCase = ObserveInbodyEntriesUseCase(
            FakeInbodyRecordRepository(flowOf(records)),
            FakeAnalysisRunner(MutableStateFlow(emptyList())),
        )

        val ids = useCase().first().map { it.record.id }

        assertEquals(listOf(3L, 1L, 2L), ids)
    }

    @Test
    fun `작업 상태가 바뀌면 목록이 다시 나온다`() = runTest {
        val runs = MutableStateFlow(listOf(running))
        val useCase = ObserveInbodyEntriesUseCase(
            FakeInbodyRecordRepository(flowOf(listOf(inbodyRecordWithResult(1, sep1, null)))),
            FakeAnalysisRunner(runs),
        )
        val flow: Flow<List<InbodyEntry>> = useCase()
        val emissions = mutableListOf<List<InbodyEntry>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.toList(emissions) }

        runs.value = emptyList()

        assertEquals(
            listOf(InbodyEntryStatus.RUNNING, InbodyEntryStatus.NOT_ANALYZED),
            emissions.map { it.single().status },
        )
    }

    private class FakeInbodyRecordRepository(private val records: Flow<List<InbodyRecordWithResult>>) :
        InbodyRecordRepository {
        override fun observeRecords(): Flow<List<InbodyRecordWithResult>> = records

        override suspend fun getRecords(): List<InbodyRecordWithResult> = error("사용하지 않음")

        override suspend fun getRecord(id: Long): InbodyRecord? = error("사용하지 않음")

        override suspend fun insert(imageFileName: String, measuredOn: LocalDate): Long = error("사용하지 않음")

        override suspend fun setFailureReason(id: Long, reason: String?) = error("사용하지 않음")
    }

    private class FakeAnalysisRunner(private val runs: Flow<List<AnalysisRun>>) : AnalysisRunner {
        override suspend fun start(request: AnalysisRunRequest) = error("사용하지 않음")

        override fun observe(kind: AnalysisKind, scopeKey: String): Flow<AnalysisRun?> = error("사용하지 않음")

        override fun observeAll(kind: AnalysisKind): Flow<List<AnalysisRun>> = runs
    }
}
