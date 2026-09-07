package kr.sdbk.bodyplan.feature.dietlog.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseImage
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.DietDayStatus

/**
 * 식단 캘린더 셀의 날짜 아래에 붙는 표시. 높이를 고정해 표시가 있든 없든 칸이 흔들리지 않는다.
 *
 * 쓸 수 있는 기간이 지나도록 비어 있는 날만 글자로 알린다. 오늘과 어제는 아직 쓸 수 있으므로
 * 비어 있어도 아무것도 그리지 않는다.
 */
@Composable
internal fun DietDayStatusIndicator(status: DietDayStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(INDICATOR_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            is DietDayStatus.Recorded ->
                BaseImage(
                    url = status.imagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    placeholder = ColorPainter(Color.LightGray),
                )

            is DietDayStatus.Missed ->
                BaseText(
                    text = "기록 안됨",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )

            is DietDayStatus.Pending, is DietDayStatus.Upcoming -> Unit
        }
    }
}

private val INDICATOR_SIZE = 36.dp

@Preview(showBackground = true)
@Composable
private fun DietDayStatusIndicatorPreview() {
    BodyPlanTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DietDayStatusIndicator(DietDayStatus.Missed)
            DietDayStatusIndicator(DietDayStatus.Pending)
            DietDayStatusIndicator(DietDayStatus.Upcoming)
        }
    }
}
