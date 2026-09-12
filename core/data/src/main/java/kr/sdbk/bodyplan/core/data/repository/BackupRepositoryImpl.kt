package kr.sdbk.bodyplan.core.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kr.sdbk.bodyplan.core.data.backup.BackupArchive
import kr.sdbk.bodyplan.core.data.image.DIET_IMAGE_DIRECTORY
import kr.sdbk.bodyplan.core.data.image.DietImages
import kr.sdbk.bodyplan.core.data.image.INBODY_IMAGE_DIRECTORY
import kr.sdbk.bodyplan.core.data.image.InbodyImages
import kr.sdbk.bodyplan.core.data.image.LocalImageStore
import kr.sdbk.bodyplan.core.domain.model.InvalidBackupFileException
import kr.sdbk.bodyplan.core.domain.repository.BackupRepository
import kr.sdbk.bodyplan.core.local.snapshot.BodyPlanSnapshot
import kr.sdbk.bodyplan.core.local.snapshot.SnapshotStore

/**
 * DB 스냅샷과 사진 두 디렉터리를 zip 하나로 묶는다.
 *
 * 복원은 DB 트랜잭션을 먼저 하고 사진을 그 뒤에 바꾼다 — 트랜잭션이 실패하면 사진도 그대로라
 * 기록과 사진이 서로 다른 시점이 되지 않는다.
 */
internal class BackupRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val snapshotStore: SnapshotStore,
    private val json: Json,
    @DietImages private val dietImageStore: LocalImageStore,
    @InbodyImages private val inbodyImageStore: LocalImageStore,
    private val clock: Clock,
) : BackupRepository {
    override suspend fun exportTo(destinationUri: String) = withContext(Dispatchers.IO) {
        val snapshot = snapshotStore.export(exportedAtMillis = clock.millis())
        val dietImages = snapshot.dietEntries.map { entry ->
            BackupArchive.ImageEntry(DIET_IMAGE_DIRECTORY, File(dietImageStore.pathOf(entry.imageFileName)))
        }
        val inbodyImages = snapshot.analysisResults.mapNotNull { result ->
            result.imageFileName?.let {
                BackupArchive.ImageEntry(INBODY_IMAGE_DIRECTORY, File(inbodyImageStore.pathOf(it)))
            }
        }
        // 기록은 있는데 파일이 사라진 사진은 건너뛴다. 사진 하나 때문에 백업 전체를 막지 않는다.
        val images = (dietImages + inbodyImages).filter { it.file.exists() }

        val output = context.contentResolver.openOutputStream(Uri.parse(destinationUri))
            ?: error("백업 파일을 열 수 없습니다: $destinationUri")
        output.use { BackupArchive.write(it, json.encodeToString(snapshot), images) }
    }

    override suspend fun importFrom(sourceUri: String) = withContext(Dispatchers.IO) {
        val extractDir = File(context.cacheDir, EXTRACT_DIRECTORY).apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            val input = context.contentResolver.openInputStream(Uri.parse(sourceUri))
                ?: error("백업 파일을 열 수 없습니다: $sourceUri")
            val contents = input.use { BackupArchive.read(it, extractDir) }
            val snapshot = try {
                json.decodeFromString<BodyPlanSnapshot>(contents.snapshotJson)
            } catch (exception: SerializationException) {
                throw InvalidBackupFileException(exception)
            } catch (exception: IllegalArgumentException) {
                throw InvalidBackupFileException(exception)
            }
            snapshotStore.import(snapshot)
            dietImageStore.replaceAll(contents.imageDirs[DIET_IMAGE_DIRECTORY])
            inbodyImageStore.replaceAll(contents.imageDirs[INBODY_IMAGE_DIRECTORY])
        } finally {
            extractDir.deleteRecursively()
        }
    }
}

private const val EXTRACT_DIRECTORY = "backup_import"
