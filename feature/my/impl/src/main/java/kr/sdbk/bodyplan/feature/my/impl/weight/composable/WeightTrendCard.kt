package kr.sdbk.bodyplan.feature.my.impl.weight.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.WeightTrend
import kr.sdbk.bodyplan.feature.my.impl.weight.weightChangeText

/** 체중이 어느 쪽으로 움직였는지. 견줄 기록이 없는 칸은 `-`로 둔다. */
@Composable
internal fun WeightTrendCard(trend: WeightTrend, modifier: Modifier = Modifier) {
    BodyPlanCard(modifier = modifier) {
        BaseText(text = "변동", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        VerticalSpacer(space = 4.dp)
        BaseText(
            text = "최근 구간과 그 이전 같은 길이 구간의 차이입니다.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
        VerticalSpacer(space = 16.dp)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TrendRow(label = "일간", value = trend.dailyChangeKg)
            TrendRow(label = "주간 평균", value = trend.weeklyAverageChangeKg)
            TrendRow(label = "월간 평균", value = trend.monthlyAverageChangeKg)
        }
    }
}

@Composable
private fun TrendRow(label: String, value: Double?) {
    Row(modifier = Modifier.fillMaxWidth()) {
        BaseText(text = label, style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
        WeightSpacer()
        BaseText(
            text = weightChangeText(value),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun WeightTrendCardPreview() {
    BodyPlanTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            WeightTrendCard(
                trend = WeightTrend(
                    dailyChangeKg = -0.5,
                    weeklyAverageChangeKg = -1.2,
                    monthlyAverageChangeKg = 0.3,
                ),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun WeightTrendCardEmptyPreview() {
    BodyPlanTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            WeightTrendCard(trend = WeightTrend())
        }
    }
}
