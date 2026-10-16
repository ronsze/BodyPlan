package kr.sdbk.bodyplan.core.data.repository

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kr.sdbk.bodyplan.core.data.mapper.encodeSections
import kr.sdbk.bodyplan.core.data.repository.fake.FakeAnalysisResultDao
import kr.sdbk.bodyplan.core.data.repository.fake.FakeDietImageStore
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class AnalysisResultRepositoryImplTest {
    private val json = Json
    private val dao = FakeAnalysisResultDao()
    private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(2_000L), ZoneOffset.UTC)

    private val repository = AnalysisResultRepositoryImpl(dao, FakeDietImageStore(), json, clock)

    private fun content(summary: String) = AnalysisContent(summary, listOf(AnalysisSection("제목", "본문")))

    @Test
    fun `replace는 열쇠와 내용과 측정값과 현재 시각을 담아 저장한다`() = runTest {
        val content = content("요약")
        val measurement = InbodyMeasurement(70.0, 30.0, 15.0, 175.0)

        repository.replace(AnalysisKind.INBODY, "3", content, measurement)

        val row = dao.stored.single()
        assertEquals("INBODY", row.kind)
        assertEquals("3", row.scopeKey)
        assertEquals("요약", row.summary)
        assertEquals(encodeSections(json, content.sections), row.sections)
        assertEquals(70.0, row.weightKg)
        assertEquals(30.0, row.skeletalMuscleKg)
        assertEquals(15.0, row.bodyFatKg)
        assertEquals(175.0, row.heightCm)
        assertNull(row.imageFileName)
        assertEquals(2_000L, row.createdAtMillis)
    }

    @Test
    fun `replace를 두 번 부르면 같은 열쇠의 결과는 하나만 남는다`() = runTest {
        repository.replace(AnalysisKind.INBODY, "3", content("첫째"), null)
        repository.replace(AnalysisKind.INBODY, "3", content("둘째"), null)

        assertEquals("둘째", dao.stored.single().summary)
    }

    @Test
    fun `replace는 다른 열쇠의 결과를 건드리지 않는다`() = runTest {
        repository.replace(AnalysisKind.INBODY, "3", content("셋"), null)
        repository.replace(AnalysisKind.INBODY, "4", content("넷"), null)

        assertEquals(setOf("3", "4"), dao.stored.map { it.scopeKey }.toSet())
    }

    @Test
    fun `measurement가 null이면 값 네 개가 모두 null이다`() = runTest {
        repository.replace(AnalysisKind.INBODY, "3", content("요약"), null)

        val row = dao.stored.single()
        assertNull(row.weightKg)
        assertNull(row.skeletalMuscleKg)
        assertNull(row.bodyFatKg)
        assertNull(row.heightCm)
    }
}
