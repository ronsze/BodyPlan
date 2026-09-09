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
import kr.sdbk.bodyplan.core.domain.model.ProgressMetric
import kr.sdbk.bodyplan.core.domain.model.ProgressMetricKey

/**
 * 체성분. 인바디를 매일 찍지 않아 기간이 아니라 최근 두 번을 견준다 —
 * 그래서 흐름 카드와 한 카드에 담지 않는다.
 */
@Composable
internal fun BodyCompositionCard(metrics: List<ProgressMetric>, modifier: Modifier = Modifier) {
    val hasAny = metrics.any { it.changeValue != null }
    BodyPlanCard(modifier = modifier) {
        BaseText(text = "체성분", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        VerticalSpacer(space = 4.dp)
        BaseText(
            text = "최근 두 번의 인바디 차이입니다.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
        VerticalSpacer(space = 16.dp)

        if (hasAny) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                metrics.forEach { MetricRow(metric = it) }
            }
        } else {
            BaseText(
                text = "인바디를 두 번 이상 분석하면 변화를 보여드려요.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun BodyCompositionCardPreview() {
    BodyPlanTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BodyCompositionCard(
                metrics = listOf(
                    ProgressMetric(ProgressMetricKey.SKELETAL_MUSCLE, 0.8, ProgressDirection.IMPROVING),
                    ProgressMetric(ProgressMetricKey.BODY_FAT, 0.4, ProgressDirection.WORSENING),
                ),
            )
            BodyCompositionCard(
                metrics = listOf(
                    ProgressMetric(ProgressMetricKey.SKELETAL_MUSCLE, null, ProgressDirection.UNKNOWN),
                    ProgressMetric(ProgressMetricKey.BODY_FAT, null, ProgressDirection.UNKNOWN),
                ),
            )
        }
    }
}
