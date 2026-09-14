package kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.TrendChart
import kr.sdbk.bodyplan.core.designsystem.component.TrendChartLine
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.defaultValueText
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrend
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrendMetric
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrendPoint
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.RecordedExercise
import kr.sdbk.bodyplan.core.domain.model.trendMetrics
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend.color
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend.label
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend.unit
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend.weekLabel

/** 종목 하나의 주간 추이. 그릴 지표는 그 종목의 강도 축이 정한다. */
@Composable
internal fun ExerciseTrendCard(trend: ExerciseTrend, modifier: Modifier = Modifier) {
    if (trend.points.isEmpty()) return

    val metrics = trend.exercise.intensityType.trendMetrics
    val lines = metrics.map { metric ->
        TrendChartLine(
            label = metric.label,
            color = metric.color,
            values = trend.points.map { point -> point.values[metric]?.toDouble() },
        )
    }.filter { line -> line.values.any { it != null } }

    BodyPlanCard(modifier = modifier) {
        BaseText(
            text = trend.exercise.name,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
        )
        VerticalSpacer(space = 4.dp)
        BaseText(
            text = "최근 8주를 주 단위로 묶었습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
        VerticalSpacer(space = 16.dp)

        TrendChart(
            lines = lines,
            startLabel = weekLabel(trend.points.first().weekStart),
            endLabel = weekLabel(trend.points.last().weekStart),
            // 단위는 축마다 다르다. 무게 종목은 두 줄이 같은 kg이라 한 표기로 묶어도 어긋나지 않는다.
            valueText = { value -> defaultValueText(value) + metrics.first().unit },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun ExerciseTrendCardPreview() {
    BodyPlanTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ExerciseTrendCard(trend = previewTrend)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun ExerciseTrendCardDurationPreview() {
    BodyPlanTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ExerciseTrendCard(
                trend = ExerciseTrend(
                    exercise = RecordedExercise(2L, "트레드밀", BodyPart.CARDIO, IntensityType.DURATION),
                    points = listOf(30, 0, 45, 40).mapIndexed { index, minutes ->
                        ExerciseTrendPoint(
                            weekStart = LocalDate.of(2026, 8, 17).plusWeeks(index.toLong()),
                            values = if (minutes == 0) {
                                emptyMap()
                            } else {
                                mapOf(ExerciseTrendMetric.TOTAL_MINUTES to minutes)
                            },
                        )
                    },
                ),
            )
        }
    }
}

private val previewTrend = ExerciseTrend(
    exercise = RecordedExercise(1L, "인클라인 벤치프레스 머신", BodyPart.CHEST, IntensityType.WEIGHT),
    points = listOf(60 to 1200, 65 to 1400, 65 to 1310, 70 to 1580).mapIndexed { index, (max, volume) ->
        ExerciseTrendPoint(
            weekStart = LocalDate.of(2026, 8, 17).plusWeeks(index.toLong()),
            values = mapOf(
                ExerciseTrendMetric.MAX_WEIGHT to max,
                ExerciseTrendMetric.TOTAL_VOLUME to volume,
            ),
        )
    },
)
