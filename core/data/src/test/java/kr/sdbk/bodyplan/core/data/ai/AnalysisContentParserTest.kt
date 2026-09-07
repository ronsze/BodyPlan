package kr.sdbk.bodyplan.core.data.ai

import kotlinx.serialization.json.Json
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisContentParserTest {
    private val parser = AnalysisContentParser(Json)

    @Test
    fun `빈 답은 안내 문구와 빈 묶음을 낸다`() {
        val result = parser.parse("")

        assertEquals("답을 받지 못했습니다", result.summary)
        assertTrue(result.sections.isEmpty())
    }

    @Test
    fun `공백만 있는 답도 빈 답으로 본다`() {
        val result = parser.parse("   \n  ")

        assertEquals("답을 받지 못했습니다", result.summary)
        assertTrue(result.sections.isEmpty())
    }

    @Test
    fun `순수 JSON을 요약과 묶음 목록으로 가른다`() {
        val raw = """
            {
                "summary": "오늘은 단백질이 부족했어요",
                "sections": [
                    {"title": "먹은 음식", "body": "밥, 김치찌개"},
                    {"title": "칼로리", "body": "약 650kcal"},
                    {"title": "영양 성분", "body": "단백질 20g"},
                    {"title": "개선 방향", "body": "단백질을 더 드세요"}
                ]
            }
        """.trimIndent()

        val result = parser.parse(raw)

        assertEquals("오늘은 단백질이 부족했어요", result.summary)
        assertEquals(
            listOf(
                AnalysisSection("먹은 음식", "밥, 김치찌개"),
                AnalysisSection("칼로리", "약 650kcal"),
                AnalysisSection("영양 성분", "단백질 20g"),
                AnalysisSection("개선 방향", "단백질을 더 드세요"),
            ),
            result.sections,
        )
    }

    @Test
    fun `코드 울타리로 감싼 JSON도 가른다`() {
        val raw = """
            ```json
            {"summary": "요약", "sections": [{"title": "제목", "body": "내용"}]}
            ```
        """.trimIndent()

        val result = parser.parse(raw)

        assertEquals("요약", result.summary)
        assertEquals(listOf(AnalysisSection("제목", "내용")), result.sections)
    }

    @Test
    fun `앞뒤에 말이 붙은 JSON도 가른다`() {
        val raw = """
            물론이죠, 분석 결과입니다:
            {"summary": "요약", "sections": []}
            도움이 되었으면 좋겠어요.
        """.trimIndent()

        val result = parser.parse(raw)

        assertEquals("요약", result.summary)
        assertTrue(result.sections.isEmpty())
    }

    @Test
    fun `JSON으로 읽을 수 없으면 받은 글 전체를 요약으로 담는다`() {
        val raw = "이건 JSON이 아니라 그냥 평범한 글입니다."

        val result = parser.parse(raw)

        assertEquals(raw, result.summary)
        assertTrue(result.sections.isEmpty())
    }

    @Test
    fun `summary가 비어 있으면 받은 글 전체를 요약으로 대신한다`() {
        val raw = """{"summary": "", "sections": []}"""

        val result = parser.parse(raw)

        assertEquals(raw, result.summary)
    }

    @Test
    fun `제목과 본문이 모두 빈 묶음은 걸러낸다`() {
        val raw = """
            {"summary": "요약", "sections": [
                {"title": "", "body": ""},
                {"title": "제목만", "body": ""}
            ]}
        """.trimIndent()

        val result = parser.parse(raw)

        assertEquals(listOf(AnalysisSection("제목만", "")), result.sections)
    }

    @Test
    fun `정상 JSON에서 네 측정값을 읽는다`() {
        val raw = """
            {"summary": "요약", "sections": [], "measurement": {
                "weightKg": 65.4, "skeletalMuscleKg": 30.1, "bodyFatKg": 12.3, "heightCm": 172.5
            }}
        """.trimIndent()

        val result = parser.parseMeasurement(raw)

        assertEquals(
            InbodyMeasurement(weightKg = 65.4, skeletalMuscleKg = 30.1, bodyFatKg = 12.3, heightCm = 172.5),
            result,
        )
    }

    @Test
    fun `measurement가 없으면 값이 전부 null인 InbodyMeasurement를 낸다`() {
        val raw = """{"summary": "요약", "sections": []}"""

        val result = parser.parseMeasurement(raw)

        assertEquals(InbodyMeasurement(), result)
    }

    @Test
    fun `JSON이 아니면 값이 전부 null인 InbodyMeasurement를 내고 예외를 던지지 않는다`() {
        val raw = "이건 JSON이 아니라 그냥 평범한 글입니다."

        val result = parser.parseMeasurement(raw)

        assertEquals(InbodyMeasurement(), result)
    }

    @Test
    fun `항목이 null이면 그 항목만 null이고 나머지는 읽힌다`() {
        val raw = """
            {"summary": "요약", "sections": [], "measurement": {
                "weightKg": 65.4, "skeletalMuscleKg": null, "bodyFatKg": 12.3, "heightCm": null
            }}
        """.trimIndent()

        val result = parser.parseMeasurement(raw)

        assertEquals(65.4, result.weightKg)
        assertNull(result.skeletalMuscleKg)
        assertEquals(12.3, result.bodyFatKg)
        assertNull(result.heightCm)
    }

    @Test
    fun `0이나 음수는 읽어 내지 못한 것으로 보고 null이 된다`() {
        val raw = """
            {"summary": "요약", "sections": [], "measurement": {
                "weightKg": 0, "skeletalMuscleKg": -1.5, "bodyFatKg": 12.3, "heightCm": 172.5
            }}
        """.trimIndent()

        val result = parser.parseMeasurement(raw)

        assertNull(result.weightKg)
        assertNull(result.skeletalMuscleKg)
        assertEquals(12.3, result.bodyFatKg)
        assertEquals(172.5, result.heightCm)
    }

    @Test
    fun `값이 문자열로 와도 예외 없이 숫자로 읽히거나 null이 된다`() {
        val raw = """
            {"summary": "요약", "sections": [], "measurement": {
                "weightKg": "65.4", "skeletalMuscleKg": "모름", "bodyFatKg": 12.3, "heightCm": "172.5"
            }}
        """.trimIndent()

        val result = parser.parseMeasurement(raw)

        // 값이 문자열이면 읽히거나(숫자로 파싱) null이거나, 어느 쪽이든 허용된다 — 예외만 없으면 된다.
        assertTrue(result.weightKg == null || result.weightKg == 65.4)
        assertTrue(result.skeletalMuscleKg == null)
        assertEquals(12.3, result.bodyFatKg)
        assertTrue(result.heightCm == null || result.heightCm == 172.5)
    }
}
