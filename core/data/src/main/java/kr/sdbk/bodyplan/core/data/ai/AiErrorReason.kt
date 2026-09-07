package kr.sdbk.bodyplan.core.data.ai

import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * 제공자가 보낸 오류 본문을 읽는다.
 *
 * 클로드·GPT·제미나이가 모양은 조금씩 달라도 `error.message`에 사유를 담는 것은 같아
 * 제공자별로 나누지 않는다. 읽어 내지 못하면 `null`을 내고 화면은 기본 문구로 떨어진다.
 *
 * 본문 전체를 그대로 쓰지 않는 것은 무엇이 실릴지 알 수 없기 때문이다.
 */
internal class AiErrorReason
@Inject
constructor(private val json: Json) {
    fun of(body: String): String? = runCatching {
        val message = json.parseToJsonElement(body)
            .jsonObject["error"]?.jsonObject
            ?.get("message") as? JsonPrimitive
        // JsonNull도 JsonPrimitive라 isString으로 걸러야 한다. 안 그러면 문자열 "null"이 화면에 뜬다.
        message?.takeIf { it.isString }?.content
            ?.takeIf { it.isNotBlank() }
            ?.take(MAX_LENGTH)
    }.getOrNull()

    /**
     * 400인데 사실은 키가 거절된 경우인지.
     *
     * 제미나이는 잘못된 키에 401이 아니라 400을 준다. 코드만 보면 요청이 잘못된 것과
     * 구분되지 않아 본문을 읽는다.
     */
    fun isInvalidKey(body: String): Boolean = runCatching {
        val error = json.parseToJsonElement(body).jsonObject["error"]?.jsonObject ?: return false
        val byDetails = error["details"]?.jsonArray?.any { detail ->
            (detail.jsonObject["reason"] as? JsonPrimitive)?.takeIf { it.isString }?.content == API_KEY_INVALID
        } ?: false
        if (byDetails) return true

        val message = (error["message"] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty().lowercase()
        message.contains("api key") && (message.contains("not valid") || message.contains("invalid"))
    }.getOrDefault(false)

    /**
     * 화면에 내보내기 전에 비밀이 섞였는지 지운다.
     *
     * 제미나이는 키를 URL 쿼리로 보내므로, 중간에 있는 무언가가 요청 URL을 되돌려주면
     * 오류 사유에 키가 실려 온다. 앞부분만 남기는 길이 제한은 이것을 막지 못한다.
     */
    fun withoutSecrets(reason: String, token: String): String {
        val withoutToken = if (token.isNotBlank()) reason.replace(token, MASK) else reason
        return withoutToken
            .replace(QUERY_KEY_PATTERN, "key=$MASK")
            .replace(API_KEY_PATTERN, MASK)
    }

    private companion object {
        /** 화면 한 자리에 들어갈 만큼만. 긴 사유는 앞부분에 이미 원인이 있다. */
        const val MAX_LENGTH = 300
        const val API_KEY_INVALID = "API_KEY_INVALID"
        const val MASK = "***"
        val QUERY_KEY_PATTERN = Regex("""key=[^&\s"]+""")
        val API_KEY_PATTERN = Regex("""(sk-|AIza)[A-Za-z0-9_-]{8,}""")
    }
}
