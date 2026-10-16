package kr.sdbk.bodyplan.feature.my.impl.inbodydetail.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.InbodyDetail
import kr.sdbk.bodyplan.core.domain.model.InbodyDetailMetric
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.changeText
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.fullText
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.tenthsText
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.title
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.unit

/** 지표 세 줄과 체지방량·제지방량. 이전 측정이 있으면 이전 값과 변화량을 함께 보인다. */
@Composable
internal fun InbodyMeasurementCard(detail: InbodyDetail, modifier: Modifier = Modifier) {
    BodyPlanCard(modifier = modifier, contentPadding = 16.dp) {
        val previous = detail.previous
        BaseText(
            text = previous?.let { "이전 측정 ${it.record.measuredOn.fullText()} 대비" } ?: "비교할 이전 기록이 없어요",
            style = MaterialTheme.typography.bodySmall,
            color = if (previous != null) Accent else TextTertiary,
        )
        VerticalSpacer(space = 12.dp)
        Divider()

        detail.metrics.forEach { metric ->
            MetricRow(metric)
            Divider()
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            MassColumn(label = "체지방량", tenths = detail.bodyFatTenths)
            MassColumn(label = "제지방량", tenths = detail.leanMassTenths)
        }
    }
}

@Composable
private fun MetricRow(metric: InbodyDetailMetric) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            BaseText(text = metric.kind.title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            metric.previousTenths?.let { previous ->
                BaseText(
                    text = "이전 ${previous.tenthsText()} ${metric.kind.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
        }
        WeightSpacer()
        Column(horizontalAlignment = Alignment.End) {
            BaseText(
                text = metric.valueTenths?.let { "${it.tenthsText()} ${metric.kind.unit}" } ?: "-",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            metric.changeTenths?.let { change ->
                BaseText(
                    text = changeText(metric.kind, change),
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                )
            }
        }
    }
}

@Composable
private fun MassColumn(label: String, tenths: Int?) {
    Column {
        BaseText(text = label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        BaseText(
            text = tenths?.let { "${it.tenthsText()} kg" } ?: "-",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
        )
    }
}

@Composable
private fun Divider() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )
}
