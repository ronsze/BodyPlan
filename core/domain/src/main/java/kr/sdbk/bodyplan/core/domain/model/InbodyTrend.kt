package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate

enum class InbodyPeriod { ONE_MONTH, THREE_MONTHS, SIX_MONTHS, ALL }

enum class InbodyMetricKind { WEIGHT, SKELETAL_MUSCLE, BODY_FAT_PERCENT }

/**
 * 그래프의 점 하나.
 *
 * 값은 표시값을 십분의 일 단위 정수로 든다(68.4 → 684). 비교와 변화량을 정수로 해야 부동소수 오차로
 * 화면의 두 수가 같은데 다르게 계산되는 일이 없다. [xFraction]은 기록 순서에 따른 가로 위치(0~1, 날짜 간격이 아니라 같은 간격),
 * [yFraction]은 축 안의 세로 위치(0이 아래, 1이 위)다.
 */
data class InbodyTrendPoint(
    val measuredOn: LocalDate,
    val valueTenths: Int,
    val xFraction: Float,
    val yFraction: Float,
)

/** 지표 하나의 카드. [changeTenths]는 값이 하나뿐이면 `null`이다. */
data class InbodyMetricCard(
    val kind: InbodyMetricKind,
    val latestTenths: Int,
    val changeTenths: Int?,
    val points: List<InbodyTrendPoint>,
    val axisMinTenths: Int,
    val axisMaxTenths: Int,
    /** 아래, 가운데, 위 순서의 세로 눈금 셋. */
    val tickTenths: List<Int>,
    /** 첫 기록과 마지막 기록의 측정일. 기록이 하나면 그 하나뿐이다. */
    val dateLabels: List<LocalDate>,
)

/**
 * 선택한 기간의 변화 그래프 전체.
 *
 * [hasRecords]가 거짓이면 기록이 하나도 없는 것이고, 참인데 [cards]가 비면 그 기간에 측정이 없는 것이다.
 */
data class InbodyTrend(
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val hasRecords: Boolean,
    val measuredCount: Int,
    val latestMeasuredOn: LocalDate?,
    /** 결과가 아직 없는 기록의 수. 그래프에 쓰이지 않는다. */
    val pendingCount: Int,
    val cards: List<InbodyMetricCard>,
)
