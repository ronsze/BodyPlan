package kr.sdbk.bodyplan.core.data.ai

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiErrorReasonTest {
    private val errorReason = AiErrorReason(Json)

    @Test
    fun `error message를 사유로 꺼낸다`() {
        val body = """{"error": {"message": "invalid api key"}}"""

        assertEquals("invalid api key", errorReason.of(body))
    }

    @Test
    fun `빈 본문은 사유가 없다`() {
        assertNull(errorReason.of(""))
    }

    @Test
    fun `JSON이 아니면 사유가 없다`() {
        assertNull(errorReason.of("이건 JSON이 아닙니다"))
    }

    @Test
    fun `error 객체가 없으면 사유가 없다`() {
        assertNull(errorReason.of("""{"status": "fail"}"""))
    }

    @Test
    fun `error message가 없으면 사유가 없다`() {
        assertNull(errorReason.of("""{"error": {"code": "bad_request"}}"""))
    }

    @Test
    fun `message가 빈 문자열이면 사유가 없다`() {
        assertNull(errorReason.of("""{"error": {"message": "   "}}"""))
    }

    @Test
    fun `300자가 넘는 사유는 300자로 잘린다`() {
        val longMessage = "a".repeat(400)
        val body = """{"error": {"message": "$longMessage"}}"""

        val reason = errorReason.of(body)

        assertEquals(300, reason?.length)
        assertEquals("a".repeat(300), reason)
    }

    @Test
    fun `300자인 사유는 그대로 남는다`() {
        val message = "a".repeat(300)
        val body = """{"error": {"message": "$message"}}"""

        assertEquals(message, errorReason.of(body))
    }

    @Test
    fun `message가 JSON null이면 사유가 없다`() {
        assertNull(errorReason.of("""{"error": {"message": null}}"""))
    }

    @Test
    fun `details의 reason이 API_KEY_INVALID면 잘못된 키다`() {
        val body = """
            {"error": {"message": "요청이 거절됐습니다", "details": [
                {"reason": "API_KEY_INVALID"}
            ]}}
        """.trimIndent()

        assertTrue(errorReason.isInvalidKey(body))
    }

    @Test
    fun `message에 api key와 not valid가 같이 있으면 잘못된 키다`() {
        val body = """{"error": {"message": "API key not valid. Please pass a valid API key."}}"""

        assertTrue(errorReason.isInvalidKey(body))
    }

    @Test
    fun `message에 api key와 invalid가 같이 있으면 대소문자 상관없이 잘못된 키다`() {
        val body = """{"error": {"message": "Invalid API Key provided"}}"""

        assertTrue(errorReason.isInvalidKey(body))
    }

    @Test
    fun `api key만 있고 not valid나 invalid가 없으면 잘못된 키가 아니다`() {
        val body = """{"error": {"message": "api key is missing from the request"}}"""

        assertFalse(errorReason.isInvalidKey(body))
    }

    @Test
    fun `그 밖의 400 본문은 잘못된 키가 아니다`() {
        val body = """{"error": {"message": "요청 형식이 잘못됐습니다"}}"""

        assertFalse(errorReason.isInvalidKey(body))
    }

    @Test
    fun `JSON이 아니면 잘못된 키가 아니다`() {
        assertFalse(errorReason.isInvalidKey("이건 JSON이 아닙니다"))
    }

    @Test
    fun `error가 없으면 잘못된 키가 아니다`() {
        assertFalse(errorReason.isInvalidKey("""{"status": "fail"}"""))
    }

    @Test
    fun `사유에 토큰이 그대로 있으면 지워진다`() {
        val reason = errorReason.withoutSecrets("요청 실패: token=sk-my-secret-token 확인하세요", "sk-my-secret-token")

        assertFalse(reason.contains("sk-my-secret-token"))
    }

    @Test
    fun `key= 꼴은 지워진다`() {
        val reason = errorReason.withoutSecrets("호출 주소: https://example.com?key=abcdef123456&other=1", "")

        assertFalse(reason.contains("abcdef123456"))
        assertTrue(reason.contains("key=***"))
    }

    @Test
    fun `sk- 꼴 키는 지워진다`() {
        val reason = errorReason.withoutSecrets("키 sk-abcdefgh12345678 가 거절됐습니다", "")

        assertFalse(reason.contains("sk-abcdefgh12345678"))
    }

    @Test
    fun `AIza 꼴 키는 지워진다`() {
        val reason = errorReason.withoutSecrets("키 AIzaSyABCDEFGHIJKLMNOP 가 거절됐습니다", "")

        assertFalse(reason.contains("AIzaSyABCDEFGHIJKLMNOP"))
    }
}
