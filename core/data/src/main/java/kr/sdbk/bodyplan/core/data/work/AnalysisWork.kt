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

    /** 한 종류의 작업을 한꺼번에 찾는 꼬리표. 목록 화면이 기록마다 따로 구독하지 않게 한다. */
    fun tagOf(kind: AnalysisKind): String = "analysis-kind:${kind.name}"

    /** 꼬리표만으로 어느 대상의 작업인지 알 수 있어야 해서 대상 열쇠도 꼬리표로 남긴다. */
    fun scopeTagOf(scopeKey: String): String = "$SCOPE_TAG_PREFIX$scopeKey"

    fun scopeKeyOf(tags: Set<String>): String? =
        tags.firstOrNull { it.startsWith(SCOPE_TAG_PREFIX) }?.removePrefix(SCOPE_TAG_PREFIX)

    private const val SCOPE_TAG_PREFIX = "analysis-scope:"
}
