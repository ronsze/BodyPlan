package kr.sdbk.bodyplan.feature.my.impl.inbodycommon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.AccentSurface
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Disabled
import kr.sdbk.bodyplan.core.designsystem.theme.TextDisabled

/** 연한 면에 강조색 글자를 얹은 보조 동작. 주 동작 `PrimaryButton`보다 눈에 덜 띄어야 하는 곳에 쓴다. */
@Composable
internal fun SecondaryFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) AccentSurface else Disabled)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BaseText(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) Accent else TextDisabled,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SecondaryFilledButtonPreview() {
    BodyPlanTheme {
        SecondaryFilledButton(text = "새로 분석하기", onClick = {}, modifier = Modifier.padding(16.dp))
    }
}
