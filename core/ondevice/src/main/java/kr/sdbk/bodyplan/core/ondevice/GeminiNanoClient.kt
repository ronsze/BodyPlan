package kr.sdbk.bodyplan.core.ondevice

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.SystemInstruction
import com.google.mlkit.genai.prompt.content
import com.google.mlkit.genai.prompt.generateContentRequest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.sdbk.bodyplan.core.network.AiClient
import kr.sdbk.bodyplan.core.network.AiImage

/**
 * 기기 안 Gemini Nano를 다른 제공자와 같은 창구로 부른다. [token]은 받지만 쓰지 않는다.
 *
 * 사진은 base64로 받아 다시 비트맵으로 푼다. 창구 계약을 온디바이스 하나 때문에 바꾸지 않기 위해서다.
 */
internal class GeminiNanoClient
@Inject
constructor(private val geminiNano: GeminiNano) : AiClient {
    /** 저장된 연결이 있어도 모델이 내려가 있을 수 있어 부를 때마다 준비 상태를 본다. */
    override suspend fun verify(token: String) {
        if (geminiNano.getStatus() != OnDeviceFeatureStatus.AVAILABLE) {
            throw OnDeviceAiException(GenAiException.ErrorCode.NOT_AVAILABLE)
        }
    }

    override suspend fun complete(
        token: String,
        systemPrompt: String,
        userPrompt: String,
        images: List<AiImage>,
    ): String {
        verify(token)
        val model = geminiNano.model
        val bitmaps = withContext(Dispatchers.IO) { images.mapNotNull { it.toBitmap() } }
        // 시스템 지시를 못 받는 모델 버전이 있다. 그때는 지시를 본문 앞에 붙여 같은 뜻을 전한다.
        val useSystemInstruction = model.isSystemPromptAvailable()
        val body = content {
            bitmaps.forEach { image(it) }
            text(if (useSystemInstruction) userPrompt else "$systemPrompt\n\n$userPrompt")
        }
        val request = if (useSystemInstruction) {
            generateContentRequest(SystemInstruction(systemPrompt), body) { maxOutputTokens = MAX_OUTPUT_TOKENS }
        } else {
            generateContentRequest(body) { maxOutputTokens = MAX_OUTPUT_TOKENS }
        }
        val response = try {
            model.generateContent(request)
        } catch (exception: GenAiException) {
            throw exception.toOnDeviceFailure()
        }
        return response.candidates.firstOrNull()?.text.orEmpty()
    }

    private fun AiImage.toBitmap(): Bitmap? {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}

/** 문서가 4K 토큰 넘는 출력을 피하라고 한다. 외부 제공자의 8192와 다른 이유다. */
private const val MAX_OUTPUT_TOKENS = 4096
