package kr.sdbk.bodyplan.feature.home.impl.home.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.ProgressDirection
import kr.sdbk.bodyplan.core.domain.model.ProgressHeadline
import kr.sdbk.bodyplan.core.domain.model.ProgressMetric
import kr.sdbk.bodyplan.core.domain.model.ProgressMetricKey

/** 최근 흐름. 지표를 다 읽지 않아도 한 줄로 알 수 있게 헤드라인을 맨 위에 둔다. */
@Composable
internal fun ProgressCard(headline: ProgressHeadline, metrics: List<ProgressMetric>, modifier: Modifier = Modifier) {
    BodyPlanCard(modifier = modifier) {
        BaseText(text = "지금 흐름", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        VerticalSpacer(space = 4.dp)
        BaseText(
            text = "최근 7일과 그 앞 7일을 견준 결과입니다.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
        VerticalSpacer(space = 16.dp)

        BaseText(text = headline.text, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        if (headline == ProgressHeadline.NOT_ENOUGH_DATA) {
            VerticalSpacer(space = 4.dp)
            BaseText(
                text = "운동과 체중을 며칠 기록하면 흐름을 보여드려요.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
        VerticalSpacer(space = 16.dp)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            metrics.forEach { MetricRow(metric = it) }
        }
    }
}

private val ProgressHeadline.text: String
    get() = when (this) {
        ProgressHeadline.IMPROVING -> "잘 가고 있어요"
        ProgressHeadline.WORSENING -> "흐름이 처졌어요"
        ProgressHeadline.STEADY -> "큰 변화가 없어요"
        ProgressHeadline.NOT_ENOUGH_DATA -> "아직 견줄 기록이 부족해요"
    }

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun ProgressCardPreview() {
    BodyPlanTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProgressCard(
                headline = ProgressHeadline.IMPROVING,
                metrics = listOf(
                    ProgressMetric(ProgressMetricKey.WEIGHT, -1.2, ProgressDirection.IMPROVING),
                    ProgressMetric(ProgressMetricKey.WORKOUT_VOLUME, 1240.0, ProgressDirection.IMPROVING),
                    ProgressMetric(ProgressMetricKey.WORKOUT_DAYS, -1.0, ProgressDirection.WORSENING),
                ),
            )
            ProgressCard(
                headline = ProgressHeadline.NOT_ENOUGH_DATA,
                metrics = listOf(
                    ProgressMetric(ProgressMetricKey.WEIGHT, null, ProgressDirection.UNKNOWN),
                    ProgressMetric(ProgressMetricKey.WORKOUT_VOLUME, null, ProgressDirection.UNKNOWN),
                    ProgressMetric(ProgressMetricKey.WORKOUT_DAYS, null, ProgressDirection.UNKNOWN),
                ),
            )
        }
    }
}
