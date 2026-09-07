package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary

/** 설정 목록의 줄 하나. 제목과 지금 상태를 보여주고 눌러 들어간다. */
@Composable
fun SectionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    /** 제목 왼쪽에 붙는 표시. 없으면 자리도 잡지 않는다. */
    leading: (@Composable () -> Unit)? = null,
) {
    BodyPlanCard(
        modifier = modifier.clickable(onClick = onClick),
        contentPadding = 18.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leading?.invoke()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BaseText(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    ),
                    color = TextPrimary,
                )
                if (description != null) {
                    BaseText(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
            }
            BodyPlanIcon(
                painter = BodyPlanIcons.ChevronRight,
                contentDescription = null,
                boxSize = 20.dp,
                iconSize = 16.dp,
                tint = TextTertiary,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun SectionRowPreview() {
    BodyPlanTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionRow(title = "AI 연동", onClick = {}, description = "클로드 연결됨")
            SectionRow(title = "AI 연동", onClick = {}, description = "연결되지 않음")
        }
    }
}
