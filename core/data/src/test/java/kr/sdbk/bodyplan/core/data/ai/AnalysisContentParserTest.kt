package kr.sdbk.bodyplan.core.data.ai

import kotlinx.serialization.json.Json
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import org.junit.Assert.assertEquals
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
}
