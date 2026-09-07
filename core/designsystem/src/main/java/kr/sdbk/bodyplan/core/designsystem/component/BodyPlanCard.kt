package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Surface

/** 바탕 위에 얹는 흰 면. 그림자는 아주 옅게만 줘서 카드 경계만 드러낸다. */
@Composable
fun BodyPlanCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(cornerRadius), clip = false)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Surface)
            .padding(contentPadding),
        content = content,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun BodyPlanCardPreview() {
    BodyPlanTheme {
        BodyPlanCard(modifier = Modifier.padding(16.dp)) {
            BaseText(text = "카드 안의 내용")
        }
    }
}
