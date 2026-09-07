package kr.sdbk.bodyplan.core.domain.model

/**
 * 식단 기록 한 건. 사진 한 장과 선택 입력인 [memo]를 갖는다.
 *
 * [imagePath]는 화면이 바로 그릴 수 있는 절대 경로다. 저장소에는 파일명만 남고,
 * 앱 내부 저장소의 위치는 읽는 시점에 붙인다 — 재설치나 백업 복원으로 경로가 바뀌기 때문이다.
 */
data class DietEntry(val id: Long, val imagePath: String, val memo: String?)
