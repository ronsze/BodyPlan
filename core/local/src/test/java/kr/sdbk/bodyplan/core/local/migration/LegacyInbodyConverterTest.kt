package kr.sdbk.bodyplan.core.local.migration

import java.time.LocalDate
import java.time.ZoneId
import kr.sdbk.bodyplan.core.local.entity.AnalysisResultEntity
import kr.sdbk.bodyplan.core.local.entity.InbodyRecordEntity
import kr.sdbk.bodyplan.core.local.snapshot.BodyPlanSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyInbodyConverterTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val utc = ZoneId.of("UTC")

    private fun result(
        id: Long,
        createdAtMillis: Long,
        kind: String = "INBODY",
        scopeKey: String = "",
        imageFileName: String? = "photo_$id.jpg",
    ) = AnalysisResultEntity(
        id = id,
        kind = kind,
        scopeKey = scopeKey,
        summary = "요약$id",
        sections = "[]",
        imageFileName = imageFileName,
        weightKg = 70.5,
        skeletalMuscleKg = 32.1,
        bodyFatKg = 14.2,
        heightCm = 175.0,
        createdAtMillis = createdAtMillis,
    )

    @Test
    fun `옛 인바디 결과 셋은 기록 셋이 되고 번호는 생성 시각 순서로 1부터 매겨진다`() {
        val results = listOf(result(7, 3000), result(5, 1000), result(6, 2000))

        val converted = LegacyInbodyConverter.convert(results, seoul)

        assertEquals(listOf(1L, 2L, 3L), converted.records.map { it.id })
        assertEquals(listOf("photo_5.jpg", "photo_6.jpg", "photo_7.jpg"), converted.records.map { it.imageFileName })
    }

    @Test
    fun `같은 생성 시각이면 결과 번호가 작은 것이 먼저 번호를 받는다`() {
        val results = listOf(result(9, 1000), result(4, 1000))

        val converted = LegacyInbodyConverter.convert(results, seoul)

        assertEquals("photo_4.jpg", converted.records.first { it.id == 1L }.imageFileName)
    }

    @Test
    fun `측정일은 결과 생성 시각의 주어진 시간대 날짜이고 추가 시각은 생성 시각이다`() {
        val millis = java.time.Instant.parse("2026-09-29T15:30:00Z").toEpochMilli()
        val results = listOf(result(1, millis))

        val inSeoul = LegacyInbodyConverter.convert(results, seoul).records.single()
        val inUtc = LegacyInbodyConverter.convert(results, utc).records.single()

        assertEquals(LocalDate.of(2026, 9, 30).toEpochDay(), inSeoul.measuredOnEpochDay)
        assertEquals(LocalDate.of(2026, 9, 29).toEpochDay(), inUtc.measuredOnEpochDay)
        assertEquals(millis, inSeoul.createdAtMillis)
        assertEquals(millis, inUtc.createdAtMillis)
    }

    @Test
    fun `변환된 결과는 요약과 묶음과 읽힌 값과 시각이 같고 열쇠가 기록 번호이며 사진 파일 이름은 비워진다`() {
        val input = result(3, 5000)

        val rewritten = LegacyInbodyConverter.convert(listOf(input), seoul).rewritten.single()

        assertEquals(input.id, rewritten.id)
        assertEquals(input.kind, rewritten.kind)
        assertEquals(input.summary, rewritten.summary)
        assertEquals(input.sections, rewritten.sections)
        assertEquals(input.weightKg, rewritten.weightKg)
        assertEquals(input.skeletalMuscleKg, rewritten.skeletalMuscleKg)
        assertEquals(input.bodyFatKg, rewritten.bodyFatKg)
        assertEquals(input.heightCm, rewritten.heightCm)
        assertEquals(input.createdAtMillis, rewritten.createdAtMillis)
        assertEquals("1", rewritten.scopeKey)
        assertNull(rewritten.imageFileName)
    }

    @Test
    fun `이미 기록 번호가 열쇠인 결과는 다시 변환하지 않는다`() {
        val first = LegacyInbodyConverter.convert(listOf(result(1, 1000), result(2, 2000)), seoul)

        val again = LegacyInbodyConverter.convert(first.rewritten, seoul)

        assertTrue(again.records.isEmpty())
        assertTrue(again.rewritten.isEmpty())
    }

    @Test
    fun `인바디가 아닌 결과는 건드리지 않는다`() {
        val results = listOf(
            result(1, 1000, kind = "DIET_DAILY"),
            result(2, 2000, scopeKey = "3"),
        )

        val converted = LegacyInbodyConverter.convert(results, seoul)

        assertTrue(converted.records.isEmpty())
        assertTrue(converted.rewritten.isEmpty())
    }

    @Test
    fun `사진 파일 이름이 없는 옛 결과는 기록으로 만들지 않는다`() {
        val converted = LegacyInbodyConverter.convert(listOf(result(1, 1000, imageFileName = null)), seoul)

        assertTrue(converted.records.isEmpty())
        assertTrue(converted.rewritten.isEmpty())
    }

    @Test
    fun `결과가 없으면 빈 결과를 낸다`() {
        val converted = LegacyInbodyConverter.convert(emptyList(), seoul)

        assertTrue(converted.records.isEmpty())
        assertTrue(converted.rewritten.isEmpty())
    }

    private fun snapshot(
        dbVersion: Int,
        results: List<AnalysisResultEntity>,
        records: List<InbodyRecordEntity> = emptyList(),
    ) = BodyPlanSnapshot(
        formatVersion = 1,
        dbVersion = dbVersion,
        exportedAtMillis = 99_000L,
        exercises = emptyList(),
        workoutEntries = emptyList(),
        workoutSets = emptyList(),
        dietEntries = emptyList(),
        userProfiles = emptyList(),
        analysisResults = results,
        weightRecords = emptyList(),
        workoutMemos = emptyList(),
        routines = emptyList(),
        routineEntries = emptyList(),
        routineSets = emptyList(),
        inbodyRecords = records,
    )

    @Test
    fun `옛 스냅샷은 변환해 기록과 바뀐 결과를 채운다`() {
        val diet = result(1, 500, kind = "DIET_DAILY", scopeKey = "2026-09-01", imageFileName = null)
        val a = result(2, 1000)
        val b = result(3, 2000)
        val input = snapshot(13, listOf(diet, a, b))

        val out = LegacyInbodyConverter.convert(input, seoul)

        assertEquals(2, out.inbodyRecords.size)
        assertEquals(listOf(1L, 2L, 3L), out.analysisResults.map { it.id })
        assertEquals(diet, out.analysisResults[0])
        assertEquals("1", out.analysisResults[1].scopeKey)
        assertEquals("2", out.analysisResults[2].scopeKey)
        assertNull(out.analysisResults[1].imageFileName)
        assertNull(out.analysisResults[2].imageFileName)
        assertEquals(input.copy(analysisResults = out.analysisResults, inbodyRecords = out.inbodyRecords), out)
        assertEquals(13, out.dbVersion)
        assertEquals(99_000L, out.exportedAtMillis)
    }

    @Test
    fun `새 스냅샷은 그대로 들인다`() {
        val record =
            InbodyRecordEntity(id = 1, imageFileName = "p.jpg", measuredOnEpochDay = 20000, createdAtMillis = 1000)
        val input = snapshot(14, listOf(result(2, 1000, scopeKey = "1", imageFileName = null)), listOf(record))

        assertEquals(input, LegacyInbodyConverter.convert(input, seoul))
    }
}
