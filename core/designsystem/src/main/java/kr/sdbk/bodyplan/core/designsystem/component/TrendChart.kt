package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.PartCardio
import kr.sdbk.bodyplan.core.designsystem.theme.PartChest
import kr.sdbk.bodyplan.core.designsystem.theme.PartLeg
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary

/**
 * 꺾은선 하나. [values]는 왼쪽이 오래된 것이고, 값이 없는 자리는 `null`이다.
 *
 * 줄마다 길이가 같아야 가로 자리가 맞는다 — 짧은 줄은 뒤를 `null`로 채워 넘긴다.
 */
data class TrendChartLine(val label: String, val color: Color, val values: List<Double?>)

/**
 * 값 몇 개의 흐름. 왼쪽이 오래된 것이다.
 *
 * 눈금 숫자를 그리지 않는 것은 함께 그리는 값들의 크기가 크게 달라서다(체중 70대와 체지방 15대,
 * 최고중량 수십과 총볼륨 수천). 한 축에 두면 작은 줄이 바닥에 눌려 변화가 보이지 않는다.
 * 줄마다 자기 최소·최대에 맞춰 펴고, 이름 옆에 마지막 값을 적는다.
 *
 * [separated]가 켜지면 줄마다 가로 띠를 따로 받아 서로 겹치지 않고, 각 점 위에 값을 적는다.
 * 띠마다 자기 최소·최대에 맞추므로 값이 가까운 줄끼리도 위아래로 갈라져 읽힌다.
 *
 * 카드 껍데기와 제목은 호출부가 씌운다 — 화면마다 문구가 다르다.
 * 값이 하나도 없는 줄은 호출부가 걸러 넘긴다.
 */
@Composable
fun TrendChart(
    lines: List<TrendChartLine>,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
    separated: Boolean = false,
    valueText: (Double) -> String = ::defaultValueText,
) {
    if (lines.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (separated) LANE_HEIGHT * lines.size else CHART_HEIGHT),
        ) {
            if (separated) {
                val laneHeight = size.height / lines.size
                val valueLabel = ValueLabel(textMeasurer, labelStyle, valueText)
                lines.forEachIndexed { index, line ->
                    drawTrend(line, laneTop = laneHeight * index, laneHeight = laneHeight, valueLabel = valueLabel)
                }
            } else {
                lines.forEach { line -> drawTrend(line) }
            }
        }

        // 좌우 어느 쪽이 최근인지 알려 준다. 목록을 최신순으로 두는 화면과 방향이 반대일 수 있다.
        VerticalSpacer(space = 6.dp)
        Row(modifier = Modifier.fillMaxWidth()) {
            BaseText(text = startLabel, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            WeightSpacer()
            BaseText(text = endLabel, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }

        VerticalSpacer(space = 12.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            lines.forEach { line -> Legend(line = line, valueText = valueText) }
        }
    }
}

@Composable
private fun Legend(line: TrendChartLine, valueText: (Double) -> String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = Modifier.size(DOT_SIZE)) { drawCircle(line.color) }
        BaseText(
            text = "${line.label} ${line.values.lastOrNull { it != null }?.let(valueText) ?: "-"}",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
    }
}

/** 점 위에 값을 적는 데 필요한 것. 글자는 캔버스 안에서 재서 그린다. */
private class ValueLabel(val measurer: TextMeasurer, val style: TextStyle, val text: (Double) -> String)

/**
 * 줄 하나를 그린다. 값들의 크기가 달라 한 눈금을 함께 쓸 수 없어 줄마다 따로 편다.
 *
 * 점이 하나면 선 없이 점만 찍는다 — 한 번만 기록한 사용자에게도 그것이 보여야 한다.
 * [valueLabel]이 있으면 점 위에 값을 적는다 — 글자 자리를 위에 비워 두고 그 아래로 편다.
 */
private fun DrawScope.drawTrend(
    line: TrendChartLine,
    laneTop: Float = 0f,
    laneHeight: Float = size.height,
    valueLabel: ValueLabel? = null,
) {
    val points = line.values.mapIndexedNotNull { index, value -> value?.let { index to it } }
    if (points.isEmpty()) return

    val minValue = points.minOf { it.second }
    val maxValue = points.maxOf { it.second }
    val span = maxValue - minValue
    val lastIndex = (line.values.size - 1).coerceAtLeast(1)
    val labelHeight = valueLabel?.let { it.measurer.measure("0", it.style).size.height + LABEL_GAP } ?: 0f
    // 점이 위아래 끝에 닿으면 반이 잘린다. 그만큼 안쪽으로 들인다.
    val top = laneTop + labelHeight + POINT_RADIUS
    val usableHeight = laneHeight - labelHeight - POINT_RADIUS * 2

    val path = Path()
    points.forEachIndexed { order, (index, value) ->
        val x = size.width * index / lastIndex
        // 값이 다 같으면 펼 것이 없다. 가운데에 둔다 — 바닥에 그리면 떨어진 것처럼 보인다.
        val ratio = if (span > 0.0) ((value - minValue) / span).toFloat() else 0.5f
        val y = top + usableHeight * (1f - ratio)
        if (order == 0) path.moveTo(x, y) else path.lineTo(x, y)
        drawCircle(color = line.color, radius = POINT_RADIUS, center = Offset(x, y))
        valueLabel?.let { drawValue(it, valueText = it.text(value), color = line.color, x = x, y = y) }
    }
    if (points.size >= 2) drawPath(path = path, color = line.color, style = Stroke(width = LINE_WIDTH))
}

/** 점 바로 위에 가운데 맞춰 적는다. 양끝 점은 글자가 캔버스 밖으로 나가지 않게 안으로 민다. */
private fun DrawScope.drawValue(valueLabel: ValueLabel, valueText: String, color: Color, x: Float, y: Float) {
    val layout = valueLabel.measurer.measure(valueText, valueLabel.style.copy(color = color))
    val left = (x - layout.size.width / 2f).coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
    drawText(layout, topLeft = Offset(left, y - POINT_RADIUS - LABEL_GAP - layout.size.height))
}

/** 정수면 소수를 적지 않는다. 단위는 호출부가 붙인다. */
fun defaultValueText(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()}" else String.format(Locale.US, "%.1f", value)

private const val LINE_WIDTH = 3f
private const val POINT_RADIUS = 5f
private const val LABEL_GAP = 4f
private val DOT_SIZE = 8.dp
private val CHART_HEIGHT = 160.dp
private val LANE_HEIGHT = 80.dp

@Preview(showBackground = true)
@Composable
private fun TrendChartPreview() {
    BodyPlanTheme {
        TrendChart(
            lines = listOf(
                TrendChartLine("체중", PartChest, listOf(74.0, 73.1, 72.4)),
                TrendChartLine("골격근량", PartLeg, listOf(32.0, 32.6, 33.1)),
                TrendChartLine("체지방량", PartCardio, listOf(18.0, 16.8, 15.4)),
            ),
            startLabel = "2026.8.1",
            endLabel = "2026.9.8",
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrendChartSinglePreview() {
    BodyPlanTheme {
        TrendChart(
            lines = listOf(TrendChartLine("최고중량", PartChest, listOf(72.4))),
            startLabel = "2026.9.8",
            endLabel = "2026.9.8",
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrendChartGapPreview() {
    BodyPlanTheme {
        TrendChart(
            lines = listOf(
                TrendChartLine("총볼륨", PartLeg, listOf(1200.0, null, 1580.0, 1490.0)),
            ),
            startLabel = "2026.8.17",
            endLabel = "2026.9.7",
            modifier = Modifier.padding(20.dp),
        )
    }
}
