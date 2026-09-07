package kr.sdbk.bodyplan.core.network

import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient

internal class GptClient
@Inject
constructor(private val okHttpClient: OkHttpClient, private val json: Json) :
    AiClient {
    override suspend fun verify(token: String) {
        okHttpClient.postJson(
            url = URL,
            body = json.encodeToString(JsonObject.serializer(), verifyBody()),
            headers = mapOf("Authorization" to "Bearer $token"),
        )
    }

    private fun verifyBody() = buildJsonObject {
        put("model", AiModels.GPT)
        put("max_completion_tokens", 1)
        put(
            "messages",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", "hi")
                    },
                )
            },
        )
    }
}

private const val URL = "https://api.openai.com/v1/chat/completions"
