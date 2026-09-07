package kr.sdbk.bodyplan.feature.my.impl.inbody.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.PartCardio
import kr.sdbk.bodyplan.core.designsystem.theme.PartChest
import kr.sdbk.bodyplan.core.designsystem.theme.PartLeg
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement

/** 그래프에 찍는 한 점. 언제 잰 것인지 알아야 좌우 방향을 적을 수 있다. */
internal data class InbodyTrendPoint(val measuredAtMillis: Long, val measurement: InbodyMeasurement)

private data class TrendLine(val label: String, val color: Color, val values: List<Double?>)

/**
 * 인바디 측정값의 흐름. 왼쪽이 오래된 것이다.
 *
 * 눈금 숫자를 그리지 않는 것은 세 값의 크기가 크게 달라(체중 70대, 체지방 15대) 한 축에
 * 숫자를 붙이면 체지방 줄이 바닥에 눌려 변화가 보이지 않아서다. 줄마다 자기 최소·최대에
 * 맞춰 펴고, 이름 옆에 지금 값을 적는다.
 *
 * [points]는 오래된 것부터다.
 */
@Composable
internal fun InbodyTrendChart(points: List<InbodyTrendPoint>, modifier: Modifier = Modifier) {
    val lines = listOf(
        TrendLine("체중", PartChest, points.map { it.measurement.weightKg }),
        TrendLine("골격근량", PartLeg, points.map { it.measurement.skeletalMuscleKg }),
        TrendLine("체지방량", PartCardio, points.map { it.measurement.bodyFatKg }),
    ).filter { line -> line.values.any { it != null } }

    if (lines.isEmpty()) return

    BodyPlanCard(modifier = modifier) {
        BaseText(text = "변화", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        VerticalSpacer(space = 4.dp)
        BaseText(
            text = "사진에서 읽은 추정값입니다.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
        VerticalSpacer(space = 16.dp)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT),
        ) {
            lines.forEach { line -> drawTrend(line) }
        }

        // 좌우 어느 쪽이 최근인지 알려 준다. 아래 이력 목록은 최신순이라 방향이 반대다.
        VerticalSpacer(space = 6.dp)
        Row(modifier = Modifier.fillMaxWidth()) {
            BaseText(
                text = points.first().measuredAtMillis.toDateText(),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
            WeightSpacer()
            BaseText(
                text = points.last().measuredAtMillis.toDateText(),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }

        VerticalSpacer(space = 12.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            lines.forEach { line -> Legend(line) }
        }
    }
}

@Composable
private fun Legend(line: TrendLine) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = Modifier.size(DOT_SIZE)) { drawCircle(line.color) }
        BaseText(
            text = "${line.label} ${line.values.lastOrNull { it != null }?.let(::format) ?: "-"}",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
    }
}

/**
 * 줄 하나를 그린다. 세 값의 크기가 달라 한 눈금을 함께 쓸 수 없어 줄마다 따로 편다.
 *
 * 점이 하나면 선 없이 점만 찍는다 — 첫 분석을 한 사용자에게도 숫자를 읽었다는 것이 보여야 한다.
 */
private fun DrawScope.drawTrend(line: TrendLine) {
    val points = line.values.mapIndexedNotNull { index, value -> value?.let { index to it } }
    if (points.isEmpty()) return

    val minValue = points.minOf { it.second }
    val maxValue = points.maxOf { it.second }
    val span = maxValue - minValue
    val lastIndex = (line.values.size - 1).coerceAtLeast(1)
    // 점이 위아래 끝에 닿으면 반이 잘린다. 그만큼 안쪽으로 들인다.
    val top = POINT_RADIUS
    val usableHeight = size.height - POINT_RADIUS * 2

    val path = Path()
    points.forEachIndexed { order, (index, value) ->
        val x = size.width * index / lastIndex
        // 값이 다 같으면 펼 것이 없다. 가운데에 둔다 — 바닥에 그리면 떨어진 것처럼 보인다.
        val ratio = if (span > 0.0) ((value - minValue) / span).toFloat() else 0.5f
        val y = top + usableHeight * (1f - ratio)
        if (order == 0) path.moveTo(x, y) else path.lineTo(x, y)
        drawCircle(color = line.color, radius = POINT_RADIUS, center = Offset(x, y))
    }
    if (points.size >= 2) drawPath(path = path, color = line.color, style = Stroke(width = LINE_WIDTH))
}

private fun format(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()}" else String.format(Locale.US, "%.1f", value)

private fun Long.toDateText(): String = DATE_FORMAT.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.M.d")
private const val LINE_WIDTH = 3f
private const val POINT_RADIUS = 5f
private val DOT_SIZE = 8.dp
private val CHART_HEIGHT = 160.dp

@Preview(showBackground = true)
@Composable
private fun InbodyTrendChartPreview() {
    BodyPlanTheme {
        InbodyTrendChart(
            points = listOf(
                InbodyTrendPoint(1_754_000_000_000L, InbodyMeasurement(74.0, 32.0, 18.0)),
                InbodyTrendPoint(1_755_500_000_000L, InbodyMeasurement(73.1, 32.6, 16.8)),
                InbodyTrendPoint(1_757_300_000_000L, InbodyMeasurement(72.4, 33.1, 15.4)),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InbodyTrendChartSinglePreview() {
    BodyPlanTheme {
        InbodyTrendChart(
            points = listOf(InbodyTrendPoint(1_757_300_000_000L, InbodyMeasurement(72.4, 33.1, 15.4))),
        )
    }
}
