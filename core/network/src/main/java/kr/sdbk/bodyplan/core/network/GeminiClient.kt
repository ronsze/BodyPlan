package kr.sdbk.bodyplan.core.network

import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient

internal class GeminiClient
@Inject
constructor(private val okHttpClient: OkHttpClient, private val json: Json) :
    AiClient {
    override suspend fun verify(token: String) {
        post(token, verifyBody())
    }

    override suspend fun complete(
        token: String,
        systemPrompt: String,
        userPrompt: String,
        images: List<AiImage>,
    ): String {
        val response = post(token, completeBody(systemPrompt, userPrompt, images))
        return json.parseToJsonElement(response)
            .jsonObject["candidates"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            .orEmpty()
    }

    // 제미나이는 키를 헤더가 아니라 쿼리로 받는다.
    private suspend fun post(token: String, body: JsonObject): String = okHttpClient.postJson(
        url = "$BASE_URL/${AiModels.GEMINI}:generateContent?key=$token",
        body = json.encodeToString(JsonObject.serializer(), body),
        headers = emptyMap(),
    )

    private fun verifyBody() = buildJsonObject {
        put(
            "contents",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put(
                            "parts",
                            buildJsonArray { add(buildJsonObject { put("text", "hi") }) },
                        )
                    },
                )
            },
        )
        put("generationConfig", buildJsonObject { put("maxOutputTokens", 1) })
    }

    private fun completeBody(systemPrompt: String, userPrompt: String, images: List<AiImage>) = buildJsonObject {
        put(
            "systemInstruction",
            buildJsonObject {
                put(
                    "parts",
                    buildJsonArray { add(buildJsonObject { put("text", systemPrompt) }) },
                )
            },
        )
        put(
            "contents",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put(
                            "parts",
                            buildJsonArray {
                                images.forEach { image ->
                                    add(
                                        buildJsonObject {
                                            put(
                                                "inline_data",
                                                buildJsonObject {
                                                    put("mime_type", image.mediaType)
                                                    put("data", image.base64)
                                                },
                                            )
                                        },
                                    )
                                }
                                add(buildJsonObject { put("text", userPrompt) })
                            },
                        )
                    },
                )
            },
        )
        put(
            "generationConfig",
            buildJsonObject { put("maxOutputTokens", AiModels.MAX_OUTPUT_TOKENS) },
        )
    }
}

private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
