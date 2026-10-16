package kr.sdbk.bodyplan.feature.my.impl.inbodyadd.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Surface

/** 사진을 가져올 길 둘. 촬영과 앨범 선택은 같은 결과(사진 하나)를 낸다. */
@Composable
internal fun PhotoSourceButtons(onClickCamera: () -> Unit, onClickAlbum: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SourceButton(text = "사진 촬영", onClick = onClickCamera, modifier = Modifier.weight(1f))
        SourceButton(text = "앨범에서 선택", onClick = onClickAlbum, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SourceButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BaseText(text = text, style = MaterialTheme.typography.bodySmall, color = Accent)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun PhotoSourceButtonsPreview() {
    BodyPlanTheme {
        PhotoSourceButtons(onClickCamera = {}, onClickAlbum = {}, modifier = Modifier.padding(16.dp))
    }
}
