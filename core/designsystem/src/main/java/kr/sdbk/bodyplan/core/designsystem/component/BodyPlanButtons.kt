package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.Disabled
import kr.sdbk.bodyplan.core.designsystem.theme.OnAccent
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextDisabled
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary

/** 화면 하단에 고정하는 주 동작 버튼. 비활성일 때 회색 면으로 바뀐다. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Accent else Disabled)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BaseText(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) OnAccent else TextDisabled,
        )
    }
}

/** 주 동작 옆에서 쓰는 테두리 버튼. 사진 고르기처럼 되돌릴 수 있는 동작에 쓴다. */
@Composable
fun OutlinedActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .border(BorderStroke(1.dp, Border), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BaseText(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun ButtonsPreview() {
    BodyPlanTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(text = "저장", onClick = {})
            PrimaryButton(text = "저장", onClick = {}, enabled = false)
            OutlinedActionButton(text = "사진 선택", onClick = {})
        }
    }
}
