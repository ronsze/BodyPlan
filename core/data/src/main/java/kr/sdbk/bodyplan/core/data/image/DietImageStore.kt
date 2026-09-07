package kr.sdbk.bodyplan.core.data.image

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 식단 사진을 앱 내부 저장소에 보관한다.
 *
 * 인터페이스로 두는 것은 Repository 테스트가 파일 시스템을 타지 않게 하기 위해서다.
 */
internal interface DietImageStore {
    /** [sourceUri]의 내용을 내부 저장소로 복사하고 파일명을 낸다. 실패하면 던진다. */
    fun save(sourceUri: String): String

    fun delete(fileName: String)

    fun pathOf(fileName: String): String
}

@Singleton
internal class DietImageStoreImpl
@Inject
constructor(@ApplicationContext private val context: Context) :
    DietImageStore {
    // cacheDir는 시스템이 지울 수 있어 사진이 사라진다.
    private val directory: File
        get() = File(context.filesDir, DIRECTORY_NAME).apply { mkdirs() }

    override fun save(sourceUri: String): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        val target = File(directory, fileName)
        try {
            val source = context.contentResolver.openInputStream(android.net.Uri.parse(sourceUri))
                ?: error("사진을 열 수 없습니다: $sourceUri")
            source.use { input -> target.outputStream().use(input::copyTo) }
        } catch (throwable: Throwable) {
            // 반쪽 파일을 남기지 않는다.
            target.delete()
            throw throwable
        }
        return fileName
    }

    override fun delete(fileName: String) {
        // 파일이 이미 없을 수 있다. 삭제 실패로 기록 삭제를 막지 않는다.
        runCatching { File(directory, fileName).delete() }
    }

    override fun pathOf(fileName: String): String = File(directory, fileName).absolutePath
}

private const val DIRECTORY_NAME = "diet_images"
