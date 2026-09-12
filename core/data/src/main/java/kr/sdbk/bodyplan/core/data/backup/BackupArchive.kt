package kr.sdbk.bodyplan.core.data.backup

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kr.sdbk.bodyplan.core.domain.model.InvalidBackupFileException

/**
 * 백업 파일의 zip 구조.
 *
 * ```
 * snapshot.json
 * images/<디렉터리>/<파일명>
 * ```
 *
 * JSON의 내용은 모른다 — 문자열로만 넣고 꺼낸다. 형식 해석은 저장소가 한다.
 */
internal object BackupArchive {
    /** zip에 넣을 사진 하나. [directoryName]은 앱 내부 저장소의 디렉터리 이름 그대로다. */
    data class ImageEntry(val directoryName: String, val file: File)

    /** 읽은 결과. [imageDirs]는 디렉터리 이름 → 풀어 놓은 디렉터리. zip에 없던 디렉터리는 키가 없다. */
    data class Contents(val snapshotJson: String, val imageDirs: Map<String, File>)

    fun write(output: OutputStream, snapshotJson: String, images: List<ImageEntry>) {
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(SNAPSHOT_ENTRY))
            zip.write(snapshotJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            images.forEach { image ->
                zip.putNextEntry(ZipEntry("$IMAGES_PREFIX${image.directoryName}/${image.file.name}"))
                image.file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * [extractDir] 아래에 사진을 풀고 JSON을 낸다. `snapshot.json`이 없거나 zip이 아니면
     * [InvalidBackupFileException]. 상위 디렉터리로 빠져나가는 항목은 버린다.
     */
    fun read(input: InputStream, extractDir: File): Contents {
        var snapshotJson: String? = null
        val imageDirs = mutableMapOf<String, File>()
        try {
            ZipInputStream(input.buffered()).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    when {
                        entry.isDirectory -> Unit
                        entry.name == SNAPSHOT_ENTRY -> snapshotJson = zip.readBytes().toString(Charsets.UTF_8)
                        entry.name.startsWith(IMAGES_PREFIX) -> extractImage(entry.name, zip, extractDir, imageDirs)
                    }
                    zip.closeEntry()
                }
            }
        } catch (exception: ZipException) {
            throw InvalidBackupFileException(exception)
        }
        return Contents(
            snapshotJson = snapshotJson ?: throw InvalidBackupFileException(),
            imageDirs = imageDirs,
        )
    }

    private fun extractImage(
        entryName: String,
        zip: ZipInputStream,
        extractDir: File,
        imageDirs: MutableMap<String, File>,
    ) {
        val parts = entryName.removePrefix(IMAGES_PREFIX).split('/')
        // `images/<디렉터리>/<파일명>` 두 단계만 받는다. 더 깊거나 `..`가 섞이면 백업이 만든 항목이 아니다.
        if (parts.size != 2 || parts.any { it.isEmpty() || it == "." || it == ".." }) return
        val (directoryName, fileName) = parts
        val directory = imageDirs.getOrPut(directoryName) { File(extractDir, directoryName).apply { mkdirs() } }
        File(directory, fileName).outputStream().use { zip.copyTo(it) }
    }

    private const val SNAPSHOT_ENTRY = "snapshot.json"
    private const val IMAGES_PREFIX = "images/"
}
