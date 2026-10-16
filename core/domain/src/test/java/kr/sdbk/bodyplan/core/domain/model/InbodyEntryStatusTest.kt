package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.usecase.inbodyEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class InbodyEntryStatusTest {
    private val day = LocalDate.of(2026, 9, 1)
    private val weight = InbodyMeasurement(weightKg = 70.0)
    private val running = AnalysisRun(AnalysisKind.INBODY, "1", AnalysisRunState.RUNNING)

    @Test
    fun `진행 중이면 분석 중이고 그 다음 실패 사유, 결과, 분석 전 순서로 정해진다`() {
        val runningEntry = inbodyEntry(1, day, weight, failureReason = "실패", run = running)
        val failedEntry = inbodyEntry(1, day, weight, failureReason = "실패")
        val analyzedEntry = inbodyEntry(1, day, weight)
        val notAnalyzedEntry = inbodyEntry(1, day)

        assertEquals(
            listOf(
                InbodyEntryStatus.RUNNING,
                InbodyEntryStatus.FAILED,
                InbodyEntryStatus.ANALYZED,
                InbodyEntryStatus.NOT_ANALYZED,
            ),
            listOf(runningEntry, failedEntry, analyzedEntry, notAnalyzedEntry).map { it.status },
        )
    }

    @Test
    fun `실패 사유가 있고 이전 결과도 있으면 분석 실패 상태이며 이전 결과를 함께 가진다`() {
        val entry = inbodyEntry(1, day, weight, failureReason = "실패")

        assertEquals(InbodyEntryStatus.FAILED, entry.status)
        assertEquals(weight, entry.result?.measurement)
        assertEquals(1L, entry.result?.id)
    }
}
