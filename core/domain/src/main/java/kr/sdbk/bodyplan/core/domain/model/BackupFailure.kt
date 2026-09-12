package kr.sdbk.bodyplan.core.domain.model

/** 고른 파일이 이 앱의 백업이 아니거나 열 수 없게 깨진 경우. */
class InvalidBackupFileException(cause: Throwable? = null) : Exception("백업 파일이 아니거나 손상됐습니다", cause)

/**
 * 백업이 지금 앱과 다른 스키마 버전에서 나온 경우. 행의 컬럼이 맞지 않아 들일 수 없다.
 *
 * [backupVersion]은 파일에 적힌 버전이다. 화면에는 내지 않고 원인 추적용으로만 남긴다.
 */
class UnsupportedBackupVersionException(val backupVersion: Int) : Exception("이 앱 버전에서 지원하지 않는 백업 파일입니다")
