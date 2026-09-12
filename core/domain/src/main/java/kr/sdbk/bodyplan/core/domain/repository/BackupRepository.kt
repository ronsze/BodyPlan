package kr.sdbk.bodyplan.core.domain.repository

import kr.sdbk.bodyplan.core.domain.model.InvalidBackupFileException
import kr.sdbk.bodyplan.core.domain.model.UnsupportedBackupVersionException

/**
 * 기록 전체를 파일 하나로 내보내고 되돌린다. 실패는 삼키지 않고 호출부로 던진다.
 *
 * 파일의 위치는 사용자가 고른 content URI다 — 앱은 저장소 권한 없이 그 URI로만 읽고 쓴다.
 */
interface BackupRepository {
    /** [destinationUri]에 백업 파일을 쓴다. */
    suspend fun exportTo(destinationUri: String)

    /**
     * [sourceUri]의 백업 파일로 기록 전체를 바꾼다. 지금 기록은 남지 않는다.
     * 백업 파일이 아니면 [InvalidBackupFileException], 스키마 버전이 다르면 [UnsupportedBackupVersionException].
     */
    suspend fun importFrom(sourceUri: String)
}
