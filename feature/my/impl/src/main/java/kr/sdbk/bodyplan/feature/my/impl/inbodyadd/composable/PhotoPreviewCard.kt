package kr.sdbk.bodyplan.feature.my.impl.inbodyadd.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseImage
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.SurfaceMuted
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary

/** 고른 사진을 보이는 카드. 고르기 전에는 자리만 보이고, 고른 뒤에는 「사진 바꾸기」가 붙는다. */
@Composable
internal fun PhotoPreviewCard(imageUri: String?, onClickChange: () -> Unit, modifier: Modifier = Modifier) {
    BodyPlanCard(modifier = modifier, contentPadding = 16.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PREVIEW_HEIGHT)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceMuted),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUri == null) {
                BaseText(
                    text = "사진을 골라 주세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary,
                )
            } else {
                BaseImage(
                    url = imageUri,
                    contentDescription = "고른 인바디 사진",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        if (imageUri != null) {
            VerticalSpacer(space = 12.dp)
            Row {
                WeightSpacer()
                BaseText(
                    text = "사진 바꾸기",
                    modifier = Modifier.clickable(onClick = onClickChange),
                    style = MaterialTheme.typography.bodySmall,
                    color = Accent,
                )
            }
        }
    }
}

private val PREVIEW_HEIGHT = 280.dp

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun PhotoPreviewCardEmptyPreview() {
    BodyPlanTheme {
        PhotoPreviewCard(imageUri = null, onClickChange = {}, modifier = Modifier.padding(16.dp))
    }
}
