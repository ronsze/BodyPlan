package kr.sdbk.bodyplan.core.data.repository.fake

import java.io.File
import kr.sdbk.bodyplan.core.data.image.LocalImageStore

/** 파일 시스템을 타지 않는 [LocalImageStore]. 무엇이 저장되고 지워졌는지만 기록한다. */
internal class FakeDietImageStore : LocalImageStore {
    var saveFailure: Throwable? = null
    var exportFailure: Throwable? = null

    private var nextIndex = 1

    val savedUris: MutableList<String> = mutableListOf()
    val deletedFileNames: MutableList<String> = mutableListOf()
    val exportedFileNames: MutableList<String> = mutableListOf()
    val replacedFrom: MutableList<File?> = mutableListOf()

    override fun save(sourceUri: String): String {
        saveFailure?.let { throw it }
        savedUris += sourceUri
        return "image-${nextIndex++}.jpg"
    }

    override fun delete(fileName: String) {
        deletedFileNames += fileName
    }

    override fun pathOf(fileName: String): String = "/files/diet_images/$fileName"

    override fun exportToGallery(fileName: String) {
        exportFailure?.let { throw it }
        exportedFileNames += fileName
    }

    override fun replaceAll(sourceDirectory: File?) {
        replacedFrom += sourceDirectory
    }
}
