package kr.sdbk.bodyplan.feature.my.impl.fake

import kr.sdbk.bodyplan.core.domain.repository.BackupRepository

internal class FakeBackupRepository : BackupRepository {
    var exportFailure: Throwable? = null
    var importFailure: Throwable? = null

    val exportedTo = mutableListOf<String>()
    val importedFrom = mutableListOf<String>()

    override suspend fun exportTo(destinationUri: String) {
        exportedTo += destinationUri
        exportFailure?.let { throw it }
    }

    override suspend fun importFrom(sourceUri: String) {
        importedFrom += sourceUri
        importFailure?.let { throw it }
    }
}
