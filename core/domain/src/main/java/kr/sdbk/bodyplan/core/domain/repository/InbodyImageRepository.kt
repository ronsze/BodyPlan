package kr.sdbk.bodyplan.core.domain.repository

/**
 * 인바디 사진을 앱 안에 보관한다.
 *
 * 고른 사진을 그대로 두지 않고 복사하는 것은, 갤러리에서 지워도 이력의 사진이 남아야 하고
 * 고른 URI의 접근 권한이 앱을 다시 켜면 사라지기 때문이다.
 */
interface InbodyImageRepository {
    /** [sourceUri]를 앱 안으로 복사하고 파일명을 낸다. 실패하면 던진다. */
    suspend fun save(sourceUri: String): String

    suspend fun delete(fileName: String)

    /** 저장된 파일명을 읽을 수 있는 경로로 푼다. */
    fun pathOf(fileName: String): String
}
