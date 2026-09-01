package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme

/** 가로 방향으로 [space]만큼 비운다. */
@Composable
fun HorizontalSpacer(space: Dp) {
    Spacer(modifier = Modifier.width(space))
}

/** 세로 방향으로 [space]만큼 비운다. */
@Composable
fun VerticalSpacer(space: Dp) {
    Spacer(modifier = Modifier.height(space))
}

/** Row의 남은 가로 공간을 [weight] 비율만큼 차지한다. */
@Composable
fun RowScope.WeightSpacer(weight: Float = 1f) {
    Spacer(modifier = Modifier.weight(weight))
}

/** Column의 남은 세로 공간을 [weight] 비율만큼 차지한다. */
@Composable
fun ColumnScope.WeightSpacer(weight: Float = 1f) {
    Spacer(modifier = Modifier.weight(weight))
}

@Preview(showBackground = true)
@Composable
private fun SpacerPreview() {
    BodyPlanTheme {
        Column(modifier = Modifier.height(120.dp)) {
            Row {
                BaseText(text = "왼쪽")
                HorizontalSpacer(space = 8.dp)
                BaseText(text = "8dp 뒤")
                WeightSpacer()
                BaseText(text = "오른쪽 끝")
            }
            VerticalSpacer(space = 12.dp)
            BaseText(text = "12dp 아래")
            WeightSpacer()
            BaseText(text = "바닥")
        }
    }
}
