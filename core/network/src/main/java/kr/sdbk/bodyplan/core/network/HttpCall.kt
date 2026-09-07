package kr.sdbk.bodyplan.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

/**
 * JSON 한 번 주고받는 호출. 세 클라이언트가 같은 방식으로 부르므로 여기 모은다.
 *
 * 2xx가 아니면 [AiHttpException]을 던진다. 네트워크가 끊기면 OkHttp의 예외가 그대로 올라간다.
 */
internal suspend fun OkHttpClient.postJson(url: String, body: String, headers: Map<String, String>): String =
    withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers.forEach { (name, value) -> addHeader(name, value) } }
            .build()

        newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw AiHttpException(response.code, responseBody)
            responseBody
        }
    }
