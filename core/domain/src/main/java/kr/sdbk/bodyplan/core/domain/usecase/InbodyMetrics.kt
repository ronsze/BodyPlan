package kr.sdbk.bodyplan.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.InbodyMetricKind

/**
 * 지표 값을 화면에 적히는 자리(소수 첫째)로 맞춘 정수로 낸다.
 *
 * 십진 문자열로 읽어 반올림한다. `Double`을 그대로 곱하면 20.45가 20.449999…로 읽혀
 * 표시와 계산이 어긋난다.
 */
object InbodyMetrics {
    private val TEN = BigDecimal(10)
    private val HUNDRED = BigDecimal(100)

    /** 68.4 → 684. 읽힌 값이 없으면 `null`. */
    fun tenthsOf(measurement: InbodyMeasurement?, kind: InbodyMetricKind): Int? = when (kind) {
        InbodyMetricKind.WEIGHT -> measurement?.weightKg?.let(::toTenths)
        InbodyMetricKind.SKELETAL_MUSCLE -> measurement?.skeletalMuscleKg?.let(::toTenths)
        InbodyMetricKind.BODY_FAT_PERCENT -> bodyFatPercentTenths(measurement)
    }

    fun bodyFatTenths(measurement: InbodyMeasurement?): Int? = measurement?.bodyFatKg?.let(::toTenths)

    fun toTenths(value: Double): Int = BigDecimal(value.toString())
        .setScale(1, RoundingMode.HALF_UP)
        .multiply(TEN)
        .toInt()

    /** 체지방량÷체중의 백분율. 체중이 0 이하이거나 둘 중 하나가 없으면 값이 없다. */
    private fun bodyFatPercentTenths(measurement: InbodyMeasurement?): Int? {
        val fat = measurement?.bodyFatKg ?: return null
        val weight = measurement.weightKg?.takeIf { it > 0.0 } ?: return null
        val percent = BigDecimal(fat.toString())
            .multiply(HUNDRED)
            .divide(BigDecimal(weight.toString()), PERCENT_SCALE, RoundingMode.HALF_UP)
        return percent.setScale(1, RoundingMode.HALF_UP).multiply(TEN).toInt()
    }

    private const val PERCENT_SCALE = 10
}
