package kr.sdbk.bodyplan.feature.home.impl.home

import androidx.compose.ui.graphics.Color
import java.util.Locale
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.Danger
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.domain.model.ProgressDirection
import kr.sdbk.bodyplan.core.domain.model.ProgressMetric
import kr.sdbk.bodyplan.core.domain.model.ProgressMetricKey

internal val ProgressMetricKey.label: String
    get() = when (this) {
        ProgressMetricKey.WEIGHT -> "체중"
        ProgressMetricKey.WORKOUT_VOLUME -> "무게 볼륨"
        ProgressMetricKey.WORKOUT_DAYS -> "운동한 날"
        ProgressMetricKey.SKELETAL_MUSCLE -> "골격근량"
        ProgressMetricKey.BODY_FAT -> "체지방량"
    }

/**
 * 늘었는지 줄었는지가 크기보다 먼저 읽혀야 해 부호를 늘 붙인다.
 * 견줄 것이 없으면 0이 아니라 `-`다 — 0으로 적으면 "변화가 없었다"로 읽힌다.
 */
internal fun changeText(metric: ProgressMetric): String {
    val value = metric.changeValue ?: return "-"
    return when (metric.key) {
        ProgressMetricKey.WORKOUT_VOLUME -> String.format(Locale.US, "%+,dkg", value.toInt())
        ProgressMetricKey.WORKOUT_DAYS -> String.format(Locale.US, "%+d일", value.toInt())
        else -> String.format(Locale.US, "%+.1fkg", value)
    }
}

/** 상승·하락 전용 색이 디자인시스템에 없어 강조색과 위험색을 쓴다. */
internal val ProgressDirection.valueColor: Color
    get() = when (this) {
        ProgressDirection.IMPROVING -> Accent
        ProgressDirection.WORSENING -> Danger
        ProgressDirection.STEADY, ProgressDirection.UNKNOWN -> TextPrimary
    }
