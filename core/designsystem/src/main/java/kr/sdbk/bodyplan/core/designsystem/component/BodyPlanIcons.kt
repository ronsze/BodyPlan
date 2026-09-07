package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import kr.sdbk.bodyplan.core.designsystem.R

/** 디자인에서 내보낸 아이콘. 선 굵기와 비율이 원본 그대로라 크기만 정하면 된다. */
object BodyPlanIcons {
    val ArrowLeft: Painter @Composable get() = painterResource(R.drawable.ic_arrow_left)
    val ChevronLeft: Painter @Composable get() = painterResource(R.drawable.ic_chevron_left)
    val ChevronRight: Painter @Composable get() = painterResource(R.drawable.ic_chevron_right)
    val Dumbbell: Painter @Composable get() = painterResource(R.drawable.ic_dumbbell)
    val ForkKnifeCrossed: Painter @Composable get() = painterResource(R.drawable.ic_fork_knife_crossed)
    val ZapOff: Painter @Composable get() = painterResource(R.drawable.ic_zap_off)
    val Bed: Painter @Composable get() = painterResource(R.drawable.ic_bed)
    val CameraOff: Painter @Composable get() = painterResource(R.drawable.ic_camera_off)
    val User: Painter @Composable get() = painterResource(R.drawable.ic_user)
}

/**
 * 아이콘 하나. 디자인은 아이콘마다 바깥 자리와 그림 크기를 따로 잡아 두었다 —
 * [boxSize]가 이웃과의 간격을 정하고 [iconSize]가 선 굵기의 비율을 지킨다. 둘을 합치면 그림이 어긋난다.
 */
@Composable
fun BodyPlanIcon(
    painter: Painter,
    contentDescription: String?,
    boxSize: Dp,
    iconSize: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(boxSize),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = tint,
        )
    }
}
