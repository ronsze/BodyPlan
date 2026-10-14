package kr.sdbk.bodyplan.feature.my.impl.inbodycommon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.AccentSurface
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary

/** 제목과 설명을 연한 면에 담은 안내. 눌리지 않는다. */
@Composable
internal fun NoticeBox(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AccentSurface)
            .padding(16.dp),
    ) {
        BaseText(text = title, style = MaterialTheme.typography.bodySmall, color = Accent)
        VerticalSpacer(space = 4.dp)
        BaseText(text = body, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Preview(showBackground = true)
@Composable
private fun NoticeBoxPreview() {
    BodyPlanTheme {
        NoticeBox(
            title = "사진만 추가해도 괜찮아요",
            body = "저장한 사진은 기록 목록에서 언제든 분석을 요청할 수 있어요.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
