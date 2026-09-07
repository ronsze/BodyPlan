package kr.sdbk.bodyplan.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary

/**
 * 토큰 없이 분석에 들어왔을 때 등록으로 이끄는 팝업.
 *
 * 식단·운동·인바디 분석이 모두 같은 안내를 쓰므로 공용에 둔다.
 */
@Composable
fun AiTokenRequiredDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(onDismissRequest = onDismiss) {
        BodyPlanCard(modifier = modifier) {
            BaseText(
                text = "AI 연결이 필요해요",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            VerticalSpacer(space = 8.dp)
            BaseText(
                text = "분석하려면 먼저 AI 토큰을 등록해야 합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            VerticalSpacer(space = 20.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedActionButton(text = "닫기", onClick = onDismiss)
                }
                Column(modifier = Modifier.weight(1f)) {
                    PrimaryButton(text = "등록하러 가기", onClick = onConfirm)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AiTokenRequiredDialogPreview() {
    BodyPlanTheme {
        AiTokenRequiredDialog(onConfirm = {}, onDismiss = {})
    }
}
