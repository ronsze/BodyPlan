package kr.sdbk.bodyplan.core.data.repository

import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kr.sdbk.bodyplan.core.data.ai.AiErrorReason
import kr.sdbk.bodyplan.core.data.ai.AnalysisContentParser
import kr.sdbk.bodyplan.core.data.ai.AnalysisImageLoader
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.AiRequestFailedException
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.core.network.AiClient
import kr.sdbk.bodyplan.core.network.AiHttpException
import kr.sdbk.bodyplan.core.network.AiImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

internal class AiAnalysisRepositoryImplTest {
    private val credential = AiCredential(provider = AiProvider.CLAUDE, token = "sk-1234")

    private fun repository(client: AiClient) = AiAnalysisRepositoryImpl(
        claude = client,
        gpt = client,
        gemini = client,
        onDevice = client,
        imageLoader = AnalysisImageLoader(),
        contentParser = AnalysisContentParser(Json),
        errorReason = AiErrorReason(Json),
    )

    @Test
    fun `401은 인증 실패가 된다`() = runTest {
        val client = FakeAiClient(verifyFailure = AiHttpException(401, ""))
        val repository = repository(client)

        val failure = runCatching { repository.verifyCredential(credential) }.exceptionOrNull()

        assertTrue(failure is AiUnauthorizedException)
    }

    @Test
    fun `403은 인증 실패가 된다`() = runTest {
        val client = FakeAiClient(verifyFailure = AiHttpException(403, ""))
        val repository = repository(client)

        val failure = runCatching { repository.verifyCredential(credential) }.exceptionOrNull()

        assertTrue(failure is AiUnauthorizedException)
    }

    @Test
    fun `400은 인증 실패가 아니라 사유가 담긴 실패가 된다`() = runTest {
        val body = """{"error": {"message": "요청 형식이 잘못됐습니다"}}"""
        val client = FakeAiClient(verifyFailure = AiHttpException(400, body))
        val repository = repository(client)

        val failure = runCatching { repository.verifyCredential(credential) }.exceptionOrNull()

        assertTrue(failure is AiRequestFailedException)
        assertEquals("요청 형식이 잘못됐습니다", (failure as AiRequestFailedException).reason)
    }

    @Test
    fun `사유를 읽어낼 수 없으면 응답 코드라도 남긴다`() = runTest {
        val client = FakeAiClient(verifyFailure = AiHttpException(400, "이건 JSON이 아닙니다"))
        val repository = repository(client)

        val failure = runCatching { repository.verifyCredential(credential) }.exceptionOrNull()

        assertTrue(failure is AiRequestFailedException)
        assertEquals("HTTP 400", (failure as AiRequestFailedException).reason)
    }

    @Test
    fun `300자가 넘는 사유는 잘린 채로 담긴다`() = runTest {
        val longMessage = "a".repeat(400)
        val body = """{"error": {"message": "$longMessage"}}"""
        val client = FakeAiClient(verifyFailure = AiHttpException(400, body))
        val repository = repository(client)

        val failure = runCatching { repository.verifyCredential(credential) }.exceptionOrNull()

        assertEquals(300, (failure as AiRequestFailedException).reason?.length)
    }

    @Test
    fun `400이어도 details의 reason이 API_KEY_INVALID면 인증 실패가 된다`() = runTest {
        val body = """
            {"error": {"message": "요청이 거절됐습니다", "details": [
                {"reason": "API_KEY_INVALID"}
            ]}}
        """.trimIndent()
        val client = FakeAiClient(verifyFailure = AiHttpException(400, body))
        val repository = repository(client)

        val failure = runCatching { repository.verifyCredential(credential) }.exceptionOrNull()

        assertTrue(failure is AiUnauthorizedException)
    }

    @Test
    fun `400이어도 message에 api key와 invalid가 같이 있으면 인증 실패가 된다`() = runTest {
        val body = """{"error": {"message": "API key not valid. Please pass a valid API key."}}"""
        val client = FakeAiClient(verifyFailure = AiHttpException(400, body))
        val repository = repository(client)

        val failure = runCatching { repository.verifyCredential(credential) }.exceptionOrNull()

        assertTrue(failure is AiUnauthorizedException)
    }

    @Test
    fun `사유에 토큰이 섞여 있으면 지워진 채로 담긴다`() = runTest {
        val credentialWithToken = credential.copy(token = "sk-my-secret-token")
        val body = """{"error": {"message": "token=sk-my-secret-token 이 거절됐습니다"}}"""
        val client = FakeAiClient(verifyFailure = AiHttpException(400, body))
        val repository = repository(client)

        val failure = runCatching { repository.verifyCredential(credentialWithToken) }.exceptionOrNull()

        val reason = (failure as AiRequestFailedException).reason
        assertTrue(reason != null && !reason.contains("sk-my-secret-token"))
    }

    @Test
    fun `사유에 URL 쿼리로 실린 키가 있으면 지워진 채로 담긴다`() = runTest {
        val body = """{"error": {"message": "호출 주소 https://example.com?key=abcdef123456 확인"}}"""
        val client = FakeAiClient(verifyFailure = AiHttpException(400, body))
        val repository = repository(client)

        val failure = runCatching { repository.verifyCredential(credential) }.exceptionOrNull()

        val reason = (failure as AiRequestFailedException).reason
        assertTrue(reason != null && !reason.contains("abcdef123456"))
    }

    @Test
    fun `error message가 JSON null이면 문자열 null이 아니라 응답 코드가 담긴다`() = runTest {
        val client = FakeAiClient(verifyFailure = AiHttpException(400, """{"error": {"message": null}}"""))
        val repository = repository(client)

        val failure = runCatching { repository.verifyCredential(credential) }.exceptionOrNull()

        assertEquals("HTTP 400", (failure as AiRequestFailedException).reason)
    }

    @Test
    fun `네트워크 단절은 끊긴 종류를 사유로 남긴다`() = runTest {
        val client = FakeAiClient(verifyFailure = IOException("disconnected"))
        val repository = repository(client)

        val failure = runCatching { repository.verifyCredential(credential) }.exceptionOrNull()

        assertTrue(failure is AiRequestFailedException)
        assertEquals("IOException: disconnected", (failure as AiRequestFailedException).reason)
    }

    @Test
    fun `네트워크 예외 메시지에 키가 섞여도 사유에 실리지 않는다`() = runTest {
        val client = FakeAiClient(
            verifyFailure = IOException("failed to connect to host?key=${credential.token}"),
        )
        val repository = repository(client)

        val failure = runCatching { repository.verifyCredential(credential) }.exceptionOrNull()

        val reason = (failure as AiRequestFailedException).reason.orEmpty()
        assertFalse(reason.contains(credential.token))
    }

    private class FakeAiClient(private val verifyFailure: Throwable? = null) : AiClient {
        override suspend fun verify(token: String) {
            verifyFailure?.let { throw it }
        }

        override suspend fun complete(
            token: String,
            systemPrompt: String,
            userPrompt: String,
            images: List<AiImage>,
        ): String {
            fail("사용하지 않음")
            return ""
        }
    }
}
