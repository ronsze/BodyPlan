package kr.sdbk.bodyplan.core.data.ai

import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement

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

        val parsed = rootObject(text) ?: return AnalysisContent(summary = text, sections = emptyList())

        return runCatching {
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

    /**
     * 인바디가 읽어 낸 숫자를 꺼낸다.
     *
     * 사진에서 읽은 추정값이라 항목마다 없을 수 있다. 없는 것을 지어내지 말라고 지시문이
     * 이르지만, 그래도 이상한 값이 오면 여기서 거른다.
     */
    fun parseMeasurement(raw: String): InbodyMeasurement = runCatching {
        val measurement = rootObject(raw.trim())?.get("measurement")?.jsonObject
            ?: return InbodyMeasurement()

        InbodyMeasurement(
            weightKg = measurement.number("weightKg", WEIGHT_RANGE),
            skeletalMuscleKg = measurement.number("skeletalMuscleKg", MASS_RANGE),
            bodyFatKg = measurement.number("bodyFatKg", MASS_RANGE),
            heightCm = measurement.number("heightCm", HEIGHT_RANGE),
        )
    }.getOrDefault(InbodyMeasurement())

    /** 코드 울타리를 걷고 바깥 중괄호 하나를 꺼낸다. 답을 두 번 읽으므로 규칙이 하나여야 한다. */
    private fun rootObject(text: String): JsonObject? {
        val body = text.substringAfter(FENCE, text).substringBeforeLast(FENCE, text)
        val objectText = body.substringAfter('{', "").substringBeforeLast('}', "")
        if (objectText.isEmpty()) return null
        return runCatching { json.parseToJsonElement("{$objectText}").jsonObject }.getOrNull()
    }

    /**
     * 숫자를 꺼내되 상식 밖의 값은 버린다.
     *
     * AI가 키를 미터로 내거나 체지방률을 체지방량으로 잘못 실을 수 있고, 그 값이 그대로
     * 그래프에 찍히고 프로필까지 덮어쓴다. 따옴표에 싸여 오는 숫자는 받아 준다 —
     * JSON 숫자를 문자열로 감싸는 것은 흔하고, 범위 검사가 이미 걸러 준다.
     */
    private fun JsonObject.number(key: String, range: ClosedFloatingPointRange<Double>): Double? =
        (this[key] as? JsonPrimitive)?.contentOrNull
            ?.toDoubleOrNull()
            ?.takeIf { it in range }

    private companion object {
        const val FENCE = "```"
        const val EMPTY_ANSWER = "답을 받지 못했습니다"

        // 사람 몸이 가질 수 있는 범위. 밖에 있으면 잘못 읽은 것이다.
        val HEIGHT_RANGE = 100.0..250.0
        val WEIGHT_RANGE = 20.0..300.0
        val MASS_RANGE = 0.1..150.0
    }
}
