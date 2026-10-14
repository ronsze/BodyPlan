package kr.sdbk.bodyplan.core.domain.usecase

import kr.sdbk.bodyplan.core.domain.model.InbodyEntry

/**
 * 기록의 순서와 "가장 최근 측정" 판정을 한 곳에 둔다.
 *
 * 표시, 프로필 반영, 마이 홈, 진행 요약이 같은 기록을 가리켜야 해서 정렬 기준을 여기 하나만 둔다.
 * 순서는 (측정일, 기록 번호)이고 같은 날에는 번호가 큰 쪽이 나중이다.
 */
object InbodyTimeline {
    /** 측정 순서의 오름차순. */
    fun ascending(entries: List<InbodyEntry>): List<InbodyEntry> =
        entries.sortedWith(compareBy({ it.record.measuredOn }, { it.record.id }))

    /** 측정 순서의 내림차순. 목록이 쓴다. */
    fun descending(entries: List<InbodyEntry>): List<InbodyEntry> = ascending(entries).reversed()

    /** 결과에 읽힌 값이 하나라도 있는 기록. */
    fun isMeasured(entry: InbodyEntry): Boolean = entry.measurement?.isEmpty == false

    /** 측정 기록 중 측정 순서가 가장 나중인 기록. 없으면 `null`. */
    fun latestMeasured(entries: List<InbodyEntry>): InbodyEntry? = ascending(entries).lastOrNull(::isMeasured)

    /** [recordId] 바로 앞에 있는 측정 기록. 없으면 `null`. */
    fun previousMeasured(entries: List<InbodyEntry>, recordId: Long): InbodyEntry? {
        val ordered = ascending(entries)
        val index = ordered.indexOfFirst { it.record.id == recordId }
        if (index < 0) return null
        return ordered.subList(0, index).lastOrNull(::isMeasured)
    }
}
