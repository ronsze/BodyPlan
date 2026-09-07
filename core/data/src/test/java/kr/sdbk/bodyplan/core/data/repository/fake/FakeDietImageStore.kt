package kr.sdbk.bodyplan.core.data.repository.fake

import kr.sdbk.bodyplan.core.data.image.DietImageStore

/** 파일 시스템을 타지 않는 [DietImageStore]. 무엇이 저장되고 지워졌는지만 기록한다. */
internal class FakeDietImageStore : DietImageStore {
    var saveFailure: Throwable? = null

    private var nextIndex = 1

    val savedUris: MutableList<String> = mutableListOf()
    val deletedFileNames: MutableList<String> = mutableListOf()

    override fun save(sourceUri: String): String {
        saveFailure?.let { throw it }
        savedUris += sourceUri
        return "image-${nextIndex++}.jpg"
    }

    override fun delete(fileName: String) {
        deletedFileNames += fileName
    }

    override fun pathOf(fileName: String): String = "/files/diet_images/$fileName"
}
