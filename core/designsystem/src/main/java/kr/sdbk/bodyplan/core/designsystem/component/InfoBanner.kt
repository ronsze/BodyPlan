package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.AccentSurface
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.SurfaceMuted
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary

/** 배너의 결. 강조는 알릴 것이 있을 때, 차분함은 쉬어 가는 이야기에 쓴다. */
enum class BannerTone { Accent, Muted }

/** 목록 아래에서 그 달의 흐름을 한 줄로 말해 주는 띠. 누를 수 없다. */
@Composable
fun InfoBanner(
    icon: Painter,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Accent,
) {
    val shape = RoundedCornerShape(16.dp)
    val background = if (tone == BannerTone.Accent) AccentSurface else SurfaceMuted
    val titleColor = if (tone == BannerTone.Accent) Accent else TextPrimary
    val iconTint = if (tone == BannerTone.Accent) Accent else TextSecondary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .then(
                if (tone == BannerTone.Muted) {
                    Modifier.border(BorderStroke(1.dp, Border), shape)
                } else {
                    Modifier
                },
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BodyPlanIcon(
            painter = icon,
            contentDescription = null,
            boxSize = 28.dp,
            iconSize = 24.dp,
            tint = iconTint,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BaseText(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                ),
                color = titleColor,
            )
            BaseText(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun InfoBannerPreview() {
    BodyPlanTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoBanner(
                icon = BodyPlanIcons.ZapOff,
                title = "이번 달 벌써 12일이나 운동했어요!",
                description = "꾸준한 노력은 배신하지 않아요.",
            )
            InfoBanner(
                icon = BodyPlanIcons.Bed,
                title = "이번 달은 휴식이 많았어요",
                description = "몸을 회복하고 다시 도전해보세요.",
                tone = BannerTone.Muted,
            )
        }
    }
}
