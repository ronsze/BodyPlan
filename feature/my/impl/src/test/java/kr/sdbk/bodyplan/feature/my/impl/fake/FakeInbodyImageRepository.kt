package kr.sdbk.bodyplan.feature.my.impl.fake

import kr.sdbk.bodyplan.core.domain.repository.InbodyImageRepository

internal class FakeInbodyImageRepository : InbodyImageRepository {
    val saved: MutableList<String> = mutableListOf()
    val deleted: MutableList<String> = mutableListOf()

    var saveFailure: Throwable? = null

    override suspend fun save(sourceUri: String): String {
        saveFailure?.let { throw it }
        val fileName = "saved-$sourceUri"
        saved += sourceUri
        return fileName
    }

    override suspend fun delete(fileName: String) {
        deleted += fileName
    }

    override fun pathOf(fileName: String): String = "/path/$fileName"
}
