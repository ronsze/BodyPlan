package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.AccentSurface
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.OnAccent
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary

/**
 * 큰 분류를 고르는 알약 칩. 고르면 강조색으로 꽉 찬다.
 * 부위처럼 항상 하나만 고르는 목록에 쓴다.
 */
@Composable
fun PillChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Chip(
        text = text,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        selectedBackground = Accent,
        selectedContent = OnAccent,
        horizontalPadding = 16.dp,
    )
}

/**
 * 분류 아래의 항목을 고르는 칩. 고르면 옅은 강조 배경에 강조색 글자가 된다.
 * 종목처럼 개수가 많고 글자가 긴 목록에 쓴다.
 */
@Composable
fun ItemChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Chip(
        text = text,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        selectedBackground = AccentSurface,
        selectedContent = Accent,
        horizontalPadding = 14.dp,
        textStyle = ChipTextStyle.Small,
    )
}

private enum class ChipTextStyle { Normal, Small }

@Composable
private fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    shape: RoundedCornerShape,
    selectedBackground: Color,
    selectedContent: Color,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    textStyle: ChipTextStyle = ChipTextStyle.Normal,
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) selectedBackground else Surface)
            .border(BorderStroke(1.dp, if (selected) selectedContent else Border), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
    ) {
        BaseText(
            text = text,
            style = when (textStyle) {
                ChipTextStyle.Normal -> MaterialTheme.typography.bodySmall
                ChipTextStyle.Small -> MaterialTheme.typography.labelMedium
            },
            color = if (selected) selectedContent else TextSecondary,
        )
    }
}

/** 카드 제목 옆에 붙는 작은 표시. 고를 수 없는 읽기 전용이다. */
@Composable
fun Badge(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AccentSurface)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        BaseText(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Accent,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun ChipsPreview() {
    BodyPlanTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillChip(text = "가슴", selected = true, onClick = {})
            PillChip(text = "등", selected = false, onClick = {})
            ItemChip(text = "푸쉬업", selected = true, onClick = {})
            Badge(text = "어깨")
        }
    }
}
