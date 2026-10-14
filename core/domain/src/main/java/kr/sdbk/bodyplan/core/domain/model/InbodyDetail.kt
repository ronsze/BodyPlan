package kr.sdbk.bodyplan.core.domain.model

/** 상세의 지표 한 줄. 값은 십분의 일 단위 정수이고, 비교할 값이 없으면 해당 필드가 `null`이다. */
data class InbodyDetailMetric(
    val kind: InbodyMetricKind,
    val valueTenths: Int?,
    val previousTenths: Int?,
    val changeTenths: Int?,
)

/** 기록 상세가 그리는 값. [previous]는 측정 순서에서 바로 앞의 측정 기록이다. */
data class InbodyDetail(
    val entry: InbodyEntry,
    val previous: InbodyEntry?,
    val metrics: List<InbodyDetailMetric>,
    val bodyFatTenths: Int?,
    /** 체중에서 체지방량을 뺀 값. 표시값끼리 빼서 화면의 두 수와 어긋나지 않는다. */
    val leanMassTenths: Int?,
)
