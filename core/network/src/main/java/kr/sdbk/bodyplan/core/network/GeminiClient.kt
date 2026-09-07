package kr.sdbk.bodyplan.core.network

import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient

internal class GeminiClient
@Inject
constructor(private val okHttpClient: OkHttpClient, private val json: Json) :
    AiClient {
    // 제미나이는 키를 헤더가 아니라 쿼리로 받는다.
    override suspend fun verify(token: String) {
        okHttpClient.postJson(
            url = "$BASE_URL/${AiModels.GEMINI}:generateContent?key=$token",
            body = json.encodeToString(JsonObject.serializer(), verifyBody()),
            headers = emptyMap(),
        )
    }

    private fun verifyBody() = buildJsonObject {
        put(
            "contents",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put(
                            "parts",
                            buildJsonArray {
                                add(buildJsonObject { put("text", "hi") })
                            },
                        )
                    },
                )
            },
        )
        put(
            "generationConfig",
            buildJsonObject { put("maxOutputTokens", 1) },
        )
    }
}

private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
