package kr.sdbk.bodyplan.core.data.image

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.util.UUID

/**
 * 사진을 앱 내부 저장소에 보관한다.
 *
 * 식단과 인바디가 하는 일이 같고 디렉터리만 달라 한 클래스를 한정자로 나눠 쓴다.
 * 인터페이스로 두는 것은 Repository 테스트가 파일 시스템을 타지 않게 하기 위해서다.
 */
internal interface LocalImageStore {
    /** [sourceUri]의 내용을 내부 저장소로 복사하고 파일명을 낸다. 실패하면 던진다. */
    fun save(sourceUri: String): String

    fun delete(fileName: String)

    fun pathOf(fileName: String): String

    /** 앱 밖의 갤러리로 사진을 복사한다. 실패하면 던진다. */
    fun exportToGallery(fileName: String)
}

/**
 * [directoryName]은 기능마다 다르다. **한번 정한 이름은 바꾸지 않는다** —
 * 바꾸면 이미 저장된 사진을 찾지 못한다.
 */
internal class LocalImageStoreImpl(private val context: Context, private val directoryName: String) : LocalImageStore {
    // cacheDir는 시스템이 지울 수 있어 사진이 사라진다.
    private val directory: File
        get() = File(context.filesDir, directoryName).apply { mkdirs() }

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

    override fun exportToGallery(fileName: String) {
        val source = File(directory, fileName)
        if (!source.exists()) error("사진이 없습니다: $fileName")

        val displayName = "BodyPlan_${System.currentTimeMillis()}.jpg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportWithMediaStore(source, displayName)
        } else {
            exportWithPublicDirectory(source, displayName)
        }
    }

    /** 안드로이드 10부터는 MediaStore가 권한 없이 쓴다. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportWithMediaStore(source: File, displayName: String) {
        val resolver = context.contentResolver
        val pending = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
            // 다 쓰기 전에는 갤러리에 보이지 않게 한다.
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, pending)
            ?: error("갤러리에 자리를 만들지 못했습니다")

        try {
            resolver.openOutputStream(uri)?.use { output -> source.inputStream().use { it.copyTo(output) } }
                ?: error("갤러리에 쓸 수 없습니다")
        } catch (throwable: Throwable) {
            resolver.delete(uri, null, null)
            throw throwable
        }

        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
    }

    /** 안드로이드 9는 공용 디렉터리에 직접 쓴다. 호출부가 쓰기 권한을 먼저 받아 둔다. */
    private fun exportWithPublicDirectory(source: File, displayName: String) {
        val album = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            ALBUM_NAME,
        ).apply { mkdirs() }
        val target = File(album, displayName)
        try {
            source.inputStream().use { input -> target.outputStream().use(input::copyTo) }
        } catch (throwable: Throwable) {
            target.delete()
            throw throwable
        }
        // 갤러리가 새 파일을 알아채게 한다.
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(MIME_TYPE), null)
    }
}

/** 식단 사진 디렉터리. 이 이름을 바꾸면 저장된 사진이 전부 사라진다. */
internal const val DIET_IMAGE_DIRECTORY = "diet_images"
internal const val INBODY_IMAGE_DIRECTORY = "inbody_images"
private const val ALBUM_NAME = "BodyPlan"
private const val MIME_TYPE = "image/jpeg"
