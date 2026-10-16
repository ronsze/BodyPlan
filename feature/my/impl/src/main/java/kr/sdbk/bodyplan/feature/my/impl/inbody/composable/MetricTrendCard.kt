package kr.sdbk.bodyplan.feature.my.impl.inbody.composable

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kr.sdbk.bodyplan.core.designsystem.component.Badge
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.InbodyMetricCard
import kr.sdbk.bodyplan.core.domain.model.InbodyMetricKind
import kr.sdbk.bodyplan.core.domain.model.InbodyTrendPoint
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.changeText
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.shortText
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.tenthsText
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.title
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.unit

/**
 * 지표 하나의 카드. 최근 값과 기간 시작 대비 변화, 눈금이 있는 꺾은선을 보인다.
 *
 * 지표마다 세로축이 달라 눈금 숫자를 적는다. 값의 크기가 다른 세 지표를 한 축에 둘 수 없다.
 */
@Composable
internal fun MetricTrendCard(card: InbodyMetricCard, modifier: Modifier = Modifier) {
    BodyPlanCard(modifier = modifier, contentPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                BaseText(
                    text = card.kind.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BaseText(
                        text = card.latestTenths.tenthsText(),
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary,
                    )
                    BaseText(
                        text = card.kind.unit,
                        modifier = Modifier.padding(bottom = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                }
            }
            WeightSpacer()
            card.changeTenths?.let { change ->
                Column(horizontalAlignment = Alignment.End) {
                    BaseText(
                        text = "기간 시작 대비",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                    VerticalSpacer(space = 4.dp)
                    Badge(text = changeText(card.kind, change))
                }
            }
        }
        VerticalSpacer(space = 8.dp)
        TrendCanvas(card)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AXIS_LABEL_WIDTH, end = PLOT_END_PADDING),
        ) {
            // 첫 기록은 왼쪽 끝, 마지막 기록은 오른쪽 끝에 둔다. 기록이 하나면 점과 같이 가운데다.
            if (card.dateLabels.size == 1) WeightSpacer()
            card.dateLabels.forEachIndexed { index, date ->
                if (index > 0) WeightSpacer()
                BaseText(
                    text = date.shortText(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
            if (card.dateLabels.size == 1) WeightSpacer()
        }
    }
}

@Composable
private fun TrendCanvas(card: InbodyMetricCard) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
    val axisLength = (card.axisMaxTenths - card.axisMinTenths).toFloat()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT),
    ) {
        val left = AXIS_LABEL_WIDTH.toPx()
        val plotWidth = size.width - left - PLOT_END_PADDING.toPx()
        // 점이 위아래 끝에 닿으면 반이 잘린다. 그만큼 안쪽으로 들인다.
        val pointRadius = POINT_RADIUS.toPx()
        val plotHeight = size.height - pointRadius * 2

        fun yOf(fraction: Float): Float = pointRadius + plotHeight * (1f - fraction)

        card.tickTenths.forEach { tick ->
            val y = yOf((tick - card.axisMinTenths) / axisLength)
            drawLine(
                color = Border,
                start = Offset(left, y),
                end = Offset(left + plotWidth, y),
                strokeWidth = 1.dp.toPx(),
            )
            val layout = measurer.measure(tick.tenthsText(), labelStyle)
            drawText(layout, topLeft = Offset(left - 6.dp.toPx() - layout.size.width, y - layout.size.height / 2f))
        }

        val path = Path()
        card.points.forEachIndexed { index, point ->
            val x = left + plotWidth * point.xFraction
            val y = yOf(point.yFraction)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (card.points.size >= 2) {
            drawPath(path = path, color = Accent, style = Stroke(width = LINE_WIDTH.toPx()))
        }
        card.points.forEachIndexed { index, point ->
            val isLast = index == card.points.lastIndex
            drawCircle(
                color = Accent,
                radius = if (isLast) LAST_POINT_RADIUS.toPx() else pointRadius,
                center = Offset(left + plotWidth * point.xFraction, yOf(point.yFraction)),
            )
        }
    }
}

private val CHART_HEIGHT = 78.dp
private val AXIS_LABEL_WIDTH = 38.dp
private val PLOT_END_PADDING = 8.dp
private val LINE_WIDTH = 2.dp
private val POINT_RADIUS = 3.dp
private val LAST_POINT_RADIUS = 4.dp

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun MetricTrendCardPreview() {
    BodyPlanTheme {
        MetricTrendCard(
            card = InbodyMetricCard(
                kind = InbodyMetricKind.WEIGHT,
                latestTenths = 684,
                changeTenths = -16,
                points = listOf(
                    InbodyTrendPoint(LocalDate.of(2026, 9, 1), 700, 0f, 0.8f),
                    InbodyTrendPoint(LocalDate.of(2026, 9, 15), 697, 0.48f, 0.75f),
                    InbodyTrendPoint(LocalDate.of(2026, 9, 30), 684, 1f, 0.1f),
                ),
                axisMinTenths = 680,
                axisMaxTenths = 710,
                tickTenths = listOf(680, 695, 710),
                dateLabels = listOf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
