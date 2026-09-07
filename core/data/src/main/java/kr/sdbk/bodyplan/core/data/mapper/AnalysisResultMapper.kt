package kr.sdbk.bodyplan.core.data.mapper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.local.entity.AnalysisResultEntity

/** 묶음을 한 컬럼에 담기 위한 저장 모양. 도메인 모델을 직렬화 대상으로 만들지 않으려고 따로 둔다. */
@Serializable
private data class StoredSection(val title: String, val body: String)

/** 저장된 종류 이름이 낯설면 지금은 없는 값이므로 결과를 없는 것으로 본다. */
internal fun AnalysisResultEntity.toDomain(json: Json, pathOf: (String) -> String): AnalysisResult? {
    val parsedKind = AnalysisKind.entries.firstOrNull { it.name == kind } ?: return null
    return AnalysisResult(
        id = id,
        kind = parsedKind,
        scopeKey = scopeKey,
        content = AnalysisContent(summary = summary, sections = decodeSections(json, sections)),
        createdAtMillis = createdAtMillis,
        imagePath = imageFileName?.let(pathOf),
    )
}

internal fun encodeSections(json: Json, sections: List<AnalysisSection>): String =
    json.encodeToString(sections.map { StoredSection(title = it.title, body = it.body) })

// 저장된 글이 깨졌다고 결과를 통째로 버리지 않는다. 요약만이라도 보이는 편이 낫다.
private fun decodeSections(json: Json, raw: String): List<AnalysisSection> = runCatching {
    json.decodeFromString<List<StoredSection>>(raw)
        .map { AnalysisSection(title = it.title, body = it.body) }
}.getOrDefault(emptyList())
