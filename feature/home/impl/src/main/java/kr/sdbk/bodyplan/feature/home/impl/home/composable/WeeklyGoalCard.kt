package kr.sdbk.bodyplan.feature.home.impl.home.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.WeeklyGoalProgress

/** 이번 주 목표까지 얼마나 왔는지. 목표가 없으면 정하라는 말만 한다 — 홈은 다른 화면으로 나가지 않는다. */
@Composable
internal fun WeeklyGoalCard(progress: WeeklyGoalProgress?, modifier: Modifier = Modifier) {
    BodyPlanCard(modifier = modifier) {
        BaseText(text = "주간 목표", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        VerticalSpacer(space = 16.dp)
        if (progress == null) {
            BaseText(
                text = "마이 탭에서 주간 목표를 정해 보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
            )
            return@BodyPlanCard
        }

        BaseText(text = messageOf(progress), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        VerticalSpacer(space = 12.dp)
        Row {
            BaseText(
                text = "이번 주 ${progress.doneDays} / ${progress.goalDays}회",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            WeightSpacer()
            BaseText(
                text = if (progress.streakWeeks > 0) "${progress.streakWeeks}주 연속 달성" else "아직 연속 달성이 없어요",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
        VerticalSpacer(space = 8.dp)
        ProgressBar(fraction = (progress.doneDays.toFloat() / progress.goalDays).coerceIn(0f, 1f))
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(Accent),
        )
    }
}

private fun messageOf(progress: WeeklyGoalProgress): String = if (progress.isAchieved) {
    "이번 주 목표를 채웠어요!"
} else {
    "${progress.goalDays - progress.doneDays}번만 더 하면 목표예요"
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun WeeklyGoalCardPreview() {
    BodyPlanTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            WeeklyGoalCard(progress = WeeklyGoalProgress(goalDays = 4, doneDays = 3, streakWeeks = 2))
            VerticalSpacer(space = 12.dp)
            WeeklyGoalCard(progress = WeeklyGoalProgress(goalDays = 3, doneDays = 3, streakWeeks = 5))
            VerticalSpacer(space = 12.dp)
            WeeklyGoalCard(progress = null)
        }
    }
}
