package kr.sdbk.bodyplan.core.data.mapper

import kotlinx.serialization.json.Json
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.local.entity.AnalysisResultEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 묶음이 한 컬럼에 JSON으로 담기므로, 직렬화기가 실제로 생성됐는지까지 여기서 걸린다.
 * `:core:data`에 직렬화 플러그인이 빠지면 컴파일은 통과하고 이 테스트만 깨진다.
 */
class AnalysisResultMapperTest {
    private val json = Json

    @Test
    fun `묶음을 담았다 꺼내면 제목과 본문이 그대로다`() {
        val sections = listOf(
            AnalysisSection("먹은 음식", "닭가슴살 샐러드"),
            AnalysisSection("칼로리", "약 1,450kcal"),
        )

        val restored = entityWith(encodeSections(json, sections)).toDomain(json)

        assertEquals(sections, restored?.content?.sections)
    }

    @Test
    fun `저장된 글이 깨졌으면 묶음만 비우고 요약은 남긴다`() {
        val restored = entityWith("깨진 글").toDomain(json)

        assertEquals(emptyList<AnalysisSection>(), restored?.content?.sections)
        assertEquals("요약", restored?.content?.summary)
    }

    @Test
    fun `모르는 종류는 없는 결과로 본다`() {
        assertNull(entityWith("[]").copy(kind = "SOMETHING_ELSE").toDomain(json))
    }

    private fun entityWith(sections: String) = AnalysisResultEntity(
        id = 1L,
        kind = AnalysisKind.DIET_DAILY.name,
        scopeKey = "20700",
        summary = "요약",
        sections = sections,
        createdAtMillis = 1_757_300_000_000L,
    )
}
