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

internal class ClaudeClient
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
            .jsonObject["content"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            .orEmpty()
    }

    private suspend fun post(token: String, body: JsonObject): String = okHttpClient.postJson(
        url = URL,
        body = json.encodeToString(JsonObject.serializer(), body),
        headers = mapOf(
            "x-api-key" to token,
            "anthropic-version" to ANTHROPIC_VERSION,
        ),
    )

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

    private fun completeBody(systemPrompt: String, userPrompt: String, images: List<AiImage>) = buildJsonObject {
        put("model", AiModels.CLAUDE)
        put("max_tokens", AiModels.MAX_OUTPUT_TOKENS)
        put("system", systemPrompt)
        put(
            "messages",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("role", "user")
                        put(
                            "content",
                            buildJsonArray {
                                images.forEach { image ->
                                    add(
                                        buildJsonObject {
                                            put("type", "image")
                                            put(
                                                "source",
                                                buildJsonObject {
                                                    put("type", "base64")
                                                    put("media_type", image.mediaType)
                                                    put("data", image.base64)
                                                },
                                            )
                                        },
                                    )
                                }
                                add(
                                    buildJsonObject {
                                        put("type", "text")
                                        put("text", userPrompt)
                                    },
                                )
                            },
                        )
                    },
                )
            },
        )
    }
}

private const val URL = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION = "2023-06-01"
