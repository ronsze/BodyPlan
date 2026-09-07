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

internal class GptClient
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
            .jsonObject["choices"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content
            .orEmpty()
    }

    private suspend fun post(token: String, body: JsonObject): String = okHttpClient.postJson(
        url = URL,
        body = json.encodeToString(JsonObject.serializer(), body),
        headers = mapOf("Authorization" to "Bearer $token"),
    )

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

    private fun completeBody(systemPrompt: String, userPrompt: String, images: List<AiImage>) = buildJsonObject {
        put("model", AiModels.GPT)
        put("max_completion_tokens", AiModels.MAX_OUTPUT_TOKENS)
        put(
            "messages",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    },
                )
                add(
                    buildJsonObject {
                        put("role", "user")
                        put(
                            "content",
                            buildJsonArray {
                                images.forEach { image ->
                                    add(
                                        buildJsonObject {
                                            put("type", "image_url")
                                            put(
                                                "image_url",
                                                buildJsonObject {
                                                    put(
                                                        "url",
                                                        "data:${image.mediaType};base64,${image.base64}",
                                                    )
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

private const val URL = "https://api.openai.com/v1/chat/completions"
