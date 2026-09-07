package kr.sdbk.bodyplan.core.data.repository

import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.sdbk.bodyplan.core.data.image.InbodyImages
import kr.sdbk.bodyplan.core.data.image.LocalImageStore
import kr.sdbk.bodyplan.core.domain.repository.InbodyImageRepository

internal class InbodyImageRepositoryImpl
@Inject
constructor(@InbodyImages private val imageStore: LocalImageStore) :
    InbodyImageRepository {
    override suspend fun save(sourceUri: String): String = withContext(Dispatchers.IO) {
        imageStore.save(sourceUri)
    }

    override suspend fun delete(fileName: String) = withContext(Dispatchers.IO) {
        imageStore.delete(fileName)
    }

    override fun pathOf(fileName: String): String = imageStore.pathOf(fileName)
}
