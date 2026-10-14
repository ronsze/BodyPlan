package kr.sdbk.bodyplan.feature.my.impl.inbodydetail.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import kr.sdbk.bodyplan.core.designsystem.component.BaseImage
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.SurfaceMuted

/** 결과지 사진 카드. 눌러도 「결과지 확대 보기」와 같이 확대 보기가 열린다. */
@Composable
internal fun ResultImageCard(imagePath: String, onClickEnlarge: () -> Unit, modifier: Modifier = Modifier) {
    BodyPlanCard(modifier = modifier, contentPadding = 16.dp) {
        BaseImage(
            file = File(imagePath),
            contentDescription = "인바디 결과지",
            modifier = Modifier
                .fillMaxWidth()
                .height(IMAGE_HEIGHT)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceMuted)
                .clickable(onClick = onClickEnlarge),
            contentScale = ContentScale.Fit,
        )
        VerticalSpacer(space = 12.dp)
        BaseText(
            text = "결과지 확대 보기",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onClickEnlarge),
            style = MaterialTheme.typography.bodySmall,
            color = Accent,
        )
    }
}

/** 전체 화면 확대 보기. 두 손가락으로 확대하고 끌어 옮기며, 닫기를 누르면 닫힌다. */
@Composable
internal fun ImageViewerDialog(imagePath: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            BaseImage(
                file = File(imagePath),
                contentDescription = "인바디 결과지 확대",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            // 원래 크기 이하에서는 옮길 이유가 없다. 확대한 만큼만 끌 수 있게 한다.
                            offset = if (scale <= MIN_SCALE) Offset.Zero else offset + pan
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
                contentScale = ContentScale.Fit,
            )
            Column(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                BaseText(
                    text = "닫기",
                    modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
        }
    }
}

private val IMAGE_HEIGHT = 280.dp
private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
