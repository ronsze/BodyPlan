package kr.sdbk.bodyplan.feature.home.impl.home.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.ProgressMetric
import kr.sdbk.bodyplan.feature.home.impl.home.changeText
import kr.sdbk.bodyplan.feature.home.impl.home.label
import kr.sdbk.bodyplan.feature.home.impl.home.valueColor

/** 지표 한 줄. 값의 색이 개선인지 악화인지를 함께 말한다. */
@Composable
internal fun MetricRow(metric: ProgressMetric, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        BaseText(
            text = metric.key.label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
        )
        WeightSpacer()
        BaseText(
            text = changeText(metric),
            style = MaterialTheme.typography.bodyMedium,
            color = metric.direction.valueColor,
        )
    }
}
