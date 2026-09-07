package kr.sdbk.bodyplan.core.data.repository

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.data.image.DietImages
import kr.sdbk.bodyplan.core.data.image.LocalImageStore
import kr.sdbk.bodyplan.core.data.mapper.toDomain
import kr.sdbk.bodyplan.core.data.mapper.toImagePathsByDate
import kr.sdbk.bodyplan.core.domain.model.DietEntry
import kr.sdbk.bodyplan.core.domain.model.DietLog
import kr.sdbk.bodyplan.core.domain.repository.DietLogRepository
import kr.sdbk.bodyplan.core.local.dao.DietEntryDao
import kr.sdbk.bodyplan.core.local.entity.DietEntryEntity

internal class DietLogRepositoryImpl
@Inject
constructor(
    private val dietEntryDao: DietEntryDao,
    @DietImages private val imageStore: LocalImageStore,
    private val clock: Clock,
) : DietLogRepository {
    override fun observeLog(date: LocalDate): Flow<DietLog> = dietEntryDao.observeByDate(date.toEpochDay())
        .map { rows -> DietLog(date = date, entries = rows.map { it.toDomain(imageStore) }) }

    override fun observeFirstImageInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, String>> =
        dietEntryDao.observeFirstImageInRange(from.toEpochDay(), to.toEpochDay())
            .map { rows -> rows.toImagePathsByDate(imageStore) }

    override suspend fun getEntry(id: Long): DietEntry? = dietEntryDao.getById(id)?.toDomain(imageStore)

    override suspend fun addEntry(date: LocalDate, sourceImageUri: String, memo: String?): Long {
        // 사진을 먼저 옮긴다. 실패하면 행을 남기지 않고 그대로 던진다.
        val fileName = imageStore.save(sourceImageUri)
        return runCatching {
            dietEntryDao.insert(
                DietEntryEntity(
                    dateEpochDay = date.toEpochDay(),
                    imageFileName = fileName,
                    memo = memo,
                    createdAtMillis = clock.millis(),
                ),
            )
        }.onFailure { imageStore.delete(fileName) }.getOrThrow()
    }

    override suspend fun updateEntry(entryId: Long, sourceImageUri: String?, memo: String?) {
        val stored = requireNotNull(dietEntryDao.getById(entryId)) { "수정할 기록이 없습니다: $entryId" }
        if (sourceImageUri == null) {
            dietEntryDao.update(stored.copy(memo = memo))
            return
        }

        val fileName = imageStore.save(sourceImageUri)
        runCatching {
            dietEntryDao.update(stored.copy(imageFileName = fileName, memo = memo))
        }.onFailure { imageStore.delete(fileName) }.getOrThrow()
        // 행 갱신이 끝난 뒤에 지운다. 먼저 지우면 갱신이 실패했을 때 사진을 잃는다.
        imageStore.delete(stored.imageFileName)
    }

    override suspend fun exportEntryImage(id: Long) {
        val stored = requireNotNull(dietEntryDao.getById(id)) { "내보낼 기록이 없습니다: $id" }
        imageStore.exportToGallery(stored.imageFileName)
    }

    override suspend fun deleteEntry(id: Long) {
        val stored = dietEntryDao.getById(id) ?: return
        dietEntryDao.deleteById(id)
        imageStore.delete(stored.imageFileName)
    }
}
