package kr.sdbk.bodyplan.feature.my.impl.inbody.composable

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.PartCardio
import kr.sdbk.bodyplan.core.designsystem.theme.PartChest
import kr.sdbk.bodyplan.core.designsystem.theme.PartLeg
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.designsystem.component.TrendChart
import kr.sdbk.bodyplan.core.designsystem.component.TrendChartLine

/** 그래프에 찍는 한 점. 언제 잰 것인지 알아야 좌우 방향을 적을 수 있다. */
internal data class InbodyTrendPoint(val measuredAtMillis: Long, val measurement: InbodyMeasurement)

/**
 * 인바디 측정값의 흐름. 왼쪽이 오래된 것이다.
 *
 * 읽을 값이 하나도 없으면 카드째 그리지 않는다 — 빈 그래프는 알려 주는 것이 없다.
 *
 * [points]는 오래된 것부터다.
 */
@Composable
internal fun InbodyTrendChart(points: List<InbodyTrendPoint>, modifier: Modifier = Modifier) {
    val lines = listOf(
        TrendChartLine("체중", PartChest, points.map { it.measurement.weightKg }),
        TrendChartLine("골격근량", PartLeg, points.map { it.measurement.skeletalMuscleKg }),
        TrendChartLine("체지방량", PartCardio, points.map { it.measurement.bodyFatKg }),
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

        TrendChart(
            lines = lines,
            startLabel = points.first().measuredAtMillis.toDateText(),
            endLabel = points.last().measuredAtMillis.toDateText(),
        )
    }
}

private fun Long.toDateText(): String = DATE_FORMAT.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.M.d")

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
