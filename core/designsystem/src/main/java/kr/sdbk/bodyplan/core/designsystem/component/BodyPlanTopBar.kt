package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary

/**
 * 화면 상단 줄. 왼쪽에 뒤로가기와 제목, 오른쪽에 동작을 둔다.
 *
 * [onBack]이 null이면 뒤로가기를 그리지 않는다 — 탭의 첫 화면처럼 돌아갈 곳이 없는 화면이다.
 * 오른쪽은 아이콘과 글자를 함께 둘 수 있고, 아이콘이 글자 왼쪽에 온다. 둘 다 null이면 비운다.
 */
@Composable
fun BodyPlanTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actionText: String? = null,
    onClickAction: () -> Unit = {},
    actionIcon: Painter? = null,
    actionIconDescription: String? = null,
    onClickActionIcon: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (onBack != null) {
                BodyPlanIcon(
                    painter = BodyPlanIcons.ArrowLeft,
                    contentDescription = "뒤로",
                    boxSize = 24.dp,
                    iconSize = 20.dp,
                    tint = TextPrimary,
                    modifier = Modifier.clickable(onClick = onBack),
                )
            }
            BaseText(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (actionIcon != null) {
                BodyPlanIcon(
                    painter = actionIcon,
                    contentDescription = actionIconDescription,
                    boxSize = 24.dp,
                    iconSize = 20.dp,
                    tint = TextPrimary,
                    modifier = Modifier.clickable(onClick = onClickActionIcon),
                )
            }
            if (actionText != null) {
                BaseText(
                    text = actionText,
                    modifier = Modifier.clickable(onClick = onClickAction),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BodyPlanTopBarPreview() {
    BodyPlanTheme {
        BodyPlanTopBar(
            title = "2026년 9월 7일",
            onBack = {},
            actionText = "운동 추가",
            actionIcon = BodyPlanIcons.ChartLine,
            actionIconDescription = "종목 추이",
        )
    }
}
