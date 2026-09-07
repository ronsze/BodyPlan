package kr.sdbk.bodyplan.core.data.ai

import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection

/**
 * 답을 요약과 묶음 목록으로 가른다.
 *
 * JSON을 부탁했어도 제공자에 따라 코드 울타리를 두르거나 앞뒤에 말을 붙인다.
 * 읽어 낼 수 없으면 받은 글을 통째로 요약에 넣는다 — 버리는 것보다 낫다.
 *
 * 묶음 제목은 검사하지 않는다. 지시문과 다른 제목이 와도 그대로 그린다 — 제목이 어긋났다고
 * 답 전체를 버리면 사용자가 볼 것이 없어진다.
 */
internal class AnalysisContentParser
@Inject
constructor(private val json: Json) {
    fun parse(raw: String): AnalysisContent {
        val text = raw.trim()
        if (text.isEmpty()) return AnalysisContent(summary = EMPTY_ANSWER, sections = emptyList())

        val body = text.substringAfter(FENCE, text).substringBeforeLast(FENCE, text)
        val objectText = body.substringAfter('{', "").substringBeforeLast('}', "")
        if (objectText.isEmpty()) return AnalysisContent(summary = text, sections = emptyList())

        return runCatching {
            val parsed = json.parseToJsonElement("{$objectText}").jsonObject
            AnalysisContent(
                summary = parsed["summary"]?.jsonPrimitive?.content.orEmpty().ifBlank { text },
                sections = parsed["sections"]?.jsonArray.orEmpty().mapNotNull { element ->
                    val section = element.jsonObject
                    val title = section["title"]?.jsonPrimitive?.content.orEmpty()
                    val sectionBody = section["body"]?.jsonPrimitive?.content.orEmpty()
                    if (title.isBlank() && sectionBody.isBlank()) {
                        null
                    } else {
                        AnalysisSection(title = title, body = sectionBody)
                    }
                },
            )
        }.getOrElse { AnalysisContent(summary = text, sections = emptyList()) }
    }

    private companion object {
        const val FENCE = "```"
        const val EMPTY_ANSWER = "답을 받지 못했습니다"
    }
}
