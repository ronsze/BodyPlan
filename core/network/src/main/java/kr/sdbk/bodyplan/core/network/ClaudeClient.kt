package kr.sdbk.bodyplan.core.network

import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient

internal class ClaudeClient
@Inject
constructor(private val okHttpClient: OkHttpClient, private val json: Json) :
    AiClient {
    override suspend fun verify(token: String) {
        okHttpClient.postJson(
            url = URL,
            body = json.encodeToString(JsonObject.serializer(), verifyBody()),
            headers = mapOf(
                "x-api-key" to token,
                "anthropic-version" to ANTHROPIC_VERSION,
            ),
        )
    }

    // 응답 내용은 쓰지 않는다. 부를 수 있는지만 보면 되므로 출력을 최소로 둔다.
    private fun verifyBody() = buildJsonObject {
        put("model", AiModels.CLAUDE)
        put("max_tokens", 1)
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

private const val URL = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION = "2023-06-01"
