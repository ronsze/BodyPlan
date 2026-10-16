package kr.sdbk.bodyplan.core.domain.usecase

import javax.inject.Inject
import kr.sdbk.bodyplan.core.domain.model.InbodyDetail
import kr.sdbk.bodyplan.core.domain.model.InbodyDetailMetric
import kr.sdbk.bodyplan.core.domain.model.InbodyEntry
import kr.sdbk.bodyplan.core.domain.model.InbodyMetricKind

/** 기록 하나의 상세 값과 이전 측정 대비 변화량을 만든다. 없는 기록이면 `null`. */
class BuildInbodyDetailUseCase @Inject constructor() {
    operator fun invoke(entries: List<InbodyEntry>, recordId: Long): InbodyDetail? {
        val entry = entries.firstOrNull { it.record.id == recordId } ?: return null
        // 이 기록에 값이 없으면 비교할 것이 없다. "-" 옆에 이전 값이 같이 나오면 읽는 사람이 헷갈린다.
        val previous = InbodyTimeline.previousMeasured(entries, recordId).takeIf { InbodyTimeline.isMeasured(entry) }

        val metrics = InbodyMetricKind.entries.map { kind ->
            val value = InbodyMetrics.tenthsOf(entry.measurement, kind)
            val previousValue = InbodyMetrics.tenthsOf(previous?.measurement, kind)
            InbodyDetailMetric(
                kind = kind,
                valueTenths = value,
                previousTenths = previousValue,
                changeTenths = if (value != null && previousValue != null) value - previousValue else null,
            )
        }
        val weight = InbodyMetrics.tenthsOf(entry.measurement, InbodyMetricKind.WEIGHT)
        val bodyFat = InbodyMetrics.bodyFatTenths(entry.measurement)
        return InbodyDetail(
            entry = entry,
            previous = previous,
            metrics = metrics,
            bodyFatTenths = bodyFat,
            leanMassTenths = if (weight != null && bodyFat != null) weight - bodyFat else null,
        )
    }
}
