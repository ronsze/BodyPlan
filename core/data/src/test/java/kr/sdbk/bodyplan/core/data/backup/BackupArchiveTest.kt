package kr.sdbk.bodyplan.core.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kr.sdbk.bodyplan.core.domain.model.InvalidBackupFileException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

internal class BackupArchiveTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `write한 것을 read하면 snapshot json이 그대로 나온다`() {
        val output = ByteArrayOutputStream()

        BackupArchive.write(output, snapshotJson = """{"a":1}""", images = emptyList())
        val contents = BackupArchive.read(output.toByteArray().inputStream(), temporaryFolder.newFolder("extract"))

        assertEquals("""{"a":1}""", contents.snapshotJson)
    }

    @Test
    fun `write한 사진을 read하면 imageDirs에 디렉터리 이름으로 풀려 파일 내용이 그대로 있다`() {
        val imageFile = temporaryFolder.newFile("photo.jpg")
        imageFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        val output = ByteArrayOutputStream()

        BackupArchive.write(
            output,
            snapshotJson = "{}",
            images = listOf(BackupArchive.ImageEntry(directoryName = "diet", file = imageFile)),
        )
        val extractDir = temporaryFolder.newFolder("extract")
        val contents = BackupArchive.read(output.toByteArray().inputStream(), extractDir)

        val dietDir = contents.imageDirs["diet"]
        assertEquals(File(extractDir, "diet"), dietDir)
        val extracted = File(dietDir, "photo.jpg")
        assertTrue(extracted.exists())
        assertEquals(listOf<Byte>(1, 2, 3, 4), extracted.readBytes().toList())
    }

    @Test
    fun `사진이 없던 디렉터리는 imageDirs에 키가 없다`() {
        val output = ByteArrayOutputStream()

        BackupArchive.write(output, snapshotJson = "{}", images = emptyList())
        val contents = BackupArchive.read(output.toByteArray().inputStream(), temporaryFolder.newFolder("extract"))

        assertTrue(contents.imageDirs.isEmpty())
    }

    @Test
    fun `snapshot json이 없는 zip은 InvalidBackupFileException을 던진다`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("images/diet/photo.jpg"))
            zip.write(byteArrayOf(1, 2, 3))
            zip.closeEntry()
        }

        assertThrows(InvalidBackupFileException::class.java) {
            BackupArchive.read(output.toByteArray().inputStream(), temporaryFolder.newFolder("extract"))
        }
    }

    @Test
    fun `zip이 아닌 바이트는 InvalidBackupFileException을 던진다`() {
        val garbage = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5))

        assertThrows(InvalidBackupFileException::class.java) {
            BackupArchive.read(garbage, temporaryFolder.newFolder("extract"))
        }
    }

    @Test
    fun `상위 디렉터리로 빠져나가는 항목은 버리고 extractDir 밖에 파일이 생기지 않는다`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("snapshot.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("images/../escape.jpg"))
            zip.write(byteArrayOf(9))
            zip.closeEntry()
        }
        val extractDir = temporaryFolder.newFolder("extract")
        val outsideFile = File(extractDir.parentFile, "escape.jpg")

        BackupArchive.read(output.toByteArray().inputStream(), extractDir)

        assertFalse(outsideFile.exists())
        assertTrue(extractDir.listFiles()?.isEmpty() != false)
    }

    @Test
    fun `images 아래 3단계 이상 깊은 항목은 버린다`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("snapshot.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("images/a/b/c"))
            zip.write(byteArrayOf(9))
            zip.closeEntry()
        }
        val extractDir = temporaryFolder.newFolder("extract")

        val contents = BackupArchive.read(output.toByteArray().inputStream(), extractDir)

        assertTrue(contents.imageDirs.isEmpty())
        assertTrue(extractDir.listFiles()?.isEmpty() != false)
    }
}
