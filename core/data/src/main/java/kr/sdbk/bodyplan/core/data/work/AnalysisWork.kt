package kr.sdbk.bodyplan.core.data.work

import kr.sdbk.bodyplan.core.domain.model.AnalysisKind

/**
 * 워커와 저장소가 함께 쓰는 이름들.
 *
 * `Data`에 담을 수 있는 것이 원시 타입뿐이라 종류는 이름으로, 날짜는 epochDay로 넣는다.
 */
internal object AnalysisWork {
    const val KEY_KIND = "kind"
    const val KEY_SCOPE = "scopeKey"
    const val KEY_PERIOD_LABEL = "periodLabel"
    const val KEY_FROM = "fromEpochDay"
    const val KEY_TO = "toEpochDay"
    const val KEY_SOURCE_URI = "sourceUri"
    const val KEY_STAGE = "stage"
    const val KEY_REASON = "reason"

    /** 날짜가 없는 요청(인바디)에서 쓰는 값. epochDay 0은 실제 날짜라 쓸 수 없다. */
    const val NO_DATE = Long.MIN_VALUE

    /**
     * 같은 대상을 두 번 넣지 못하게 하는 열쇠.
     *
     * 대상이 다르면 이름도 달라 동시에 돈다.
     */
    fun nameOf(kind: AnalysisKind, scopeKey: String): String = "analysis:${kind.name}:$scopeKey"
}
