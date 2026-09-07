package kr.sdbk.bodyplan.core.data.ai

import android.util.Base64
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.sdbk.bodyplan.core.network.AiImage

/**
 * 사진 파일을 요청에 실을 수 있는 모양으로 바꾼다.
 *
 * 기간이 길면 사진이 수십 장이 된다. 전부 보내면 요청이 커져 느리고 비싸므로 고르게 솎아
 * 최대 [MAX_IMAGES]장만 보낸다. 나머지는 메모로만 전달된다.
 *
 * 파일 읽기와 인코딩이 무거워 IO로 옮긴다 — 호출부가 메인 스레드에서 부른다.
 */
internal class AnalysisImageLoader
@Inject
constructor() {
    suspend fun load(paths: List<String>): List<AiImage> = withContext(Dispatchers.IO) {
        sample(paths).mapNotNull { path ->
            val file = File(path)
            if (!file.exists()) return@mapNotNull null
            runCatching {
                AiImage(
                    mediaType = MEDIA_TYPE,
                    base64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP),
                )
            }.getOrNull()
        }
    }

    private fun sample(paths: List<String>): List<String> {
        if (paths.size <= MAX_IMAGES) return paths
        val step = paths.size.toDouble() / MAX_IMAGES
        return (0 until MAX_IMAGES).map { paths[(it * step).toInt()] }
    }

    private companion object {
        const val MAX_IMAGES = 12
        const val MEDIA_TYPE = "image/jpeg"
    }
}
