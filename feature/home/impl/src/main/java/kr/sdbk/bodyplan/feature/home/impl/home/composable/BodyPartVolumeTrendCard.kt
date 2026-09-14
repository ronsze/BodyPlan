package kr.sdbk.bodyplan.feature.home.impl.home.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolumeTrend
import kr.sdbk.bodyplan.core.domain.model.ProgressDirection
import kr.sdbk.bodyplan.core.ui.components.color
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.core.ui.components.volumeText
import kr.sdbk.bodyplan.feature.home.impl.home.trendChangeText
import kr.sdbk.bodyplan.feature.home.impl.home.valueColor
import kr.sdbk.bodyplan.feature.home.impl.home.volumeTrendMessage

/** 부위별 볼륨이 어디로 갔는지. 「지금 흐름」과 같은 두 구간을 부위로 가른 것이라 부제도 같게 둔다. */
@Composable
internal fun BodyPartVolumeTrendCard(trends: List<BodyPartVolumeTrend>, modifier: Modifier = Modifier) {
    BodyPlanCard(modifier = modifier) {
        BaseText(text = "부위별 볼륨 변화", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        VerticalSpacer(space = 4.dp)
        BaseText(
            text = "최근 7일과 그 앞 7일을 견준 결과입니다.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
        VerticalSpacer(space = 16.dp)

        if (trends.isEmpty()) {
            BaseText(
                text = "최근 2주 무게 기록이 없습니다",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
            )
            return@BodyPlanCard
        }

        BaseText(
            text = volumeTrendMessage(trends),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        VerticalSpacer(space = 16.dp)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            trends.forEach { TrendRow(trend = it) }
        }
    }
}

@Composable
private fun TrendRow(trend: BodyPartVolumeTrend) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(trend.bodyPart.color, CircleShape),
        )
        BaseText(
            text = trend.bodyPart.label,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
        WeightSpacer()
        BaseText(
            text = volumeText(trend.recentVolumeKg),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        BaseText(
            text = trendChangeText(trend),
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = trend.direction.valueColor,
        )
    }
}

internal val previewVolumeTrends = listOf(
    BodyPartVolumeTrend(BodyPart.CHEST, 1240, 300, ProgressDirection.IMPROVING),
    BodyPartVolumeTrend(BodyPart.BACK, 980, 120, ProgressDirection.IMPROVING),
    BodyPartVolumeTrend(BodyPart.LEG, 0, -1500, ProgressDirection.WORSENING),
    BodyPartVolumeTrend(BodyPart.SHOULDER, 360, 0, ProgressDirection.STEADY),
)

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun BodyPartVolumeTrendCardPreview() {
    BodyPlanTheme {
        BodyPartVolumeTrendCard(trends = previewVolumeTrends, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun BodyPartVolumeTrendCardEmptyPreview() {
    BodyPlanTheme {
        BodyPartVolumeTrendCard(trends = emptyList(), modifier = Modifier.padding(16.dp))
    }
}
