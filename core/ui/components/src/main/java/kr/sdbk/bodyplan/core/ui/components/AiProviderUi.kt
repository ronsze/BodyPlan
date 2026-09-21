package kr.sdbk.bodyplan.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.theme.AiClaude
import kr.sdbk.bodyplan.core.designsystem.theme.AiGemini
import kr.sdbk.bodyplan.core.designsystem.theme.AiGpt
import kr.sdbk.bodyplan.core.designsystem.theme.AiOnDevice
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.domain.model.AiProvider

val AiProvider.label: String
    get() = when (this) {
        AiProvider.CLAUDE -> "클로드"
        AiProvider.GPT -> "GPT"
        AiProvider.GEMINI -> "제미나이"
        AiProvider.ON_DEVICE -> "온디바이스"
    }

/**
 * 제공자를 가리키는 색.
 *
 * 각 제공자의 상징색에 가깝게 잡았다. 공식 로고 파일을 넣게 되면 이 색과 [AiProviderMark]가
 * 함께 그것으로 바뀐다.
 */
val AiProvider.brandColor: Color
    get() = when (this) {
        AiProvider.CLAUDE -> AiClaude
        AiProvider.GPT -> AiGpt
        AiProvider.GEMINI -> AiGemini
        AiProvider.ON_DEVICE -> AiOnDevice
    }

private val AiProvider.initial: String
    get() = when (this) {
        AiProvider.CLAUDE -> "C"
        AiProvider.GPT -> "G"
        AiProvider.GEMINI -> "G"
        AiProvider.ON_DEVICE -> "N"
    }

/**
 * 제공자 표시. 상징색 원에 머리글자를 넣는다.
 *
 * 공식 로고를 기억으로 그리면 엉성한 짝퉁이 되므로, 정확한 것만 그린다.
 */
@Composable
fun AiProviderMark(provider: AiProvider, modifier: Modifier = Modifier, size: Dp = MARK_SIZE) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(provider.brandColor),
        contentAlignment = Alignment.Center,
    ) {
        BaseText(
            text = provider.initial,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
    }
}

private val MARK_SIZE = 28.dp

@Preview(showBackground = true)
@Composable
private fun AiProviderMarkPreview() {
    BodyPlanTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AiProvider.entries.forEach { provider ->
                AiProviderMark(provider)
                BaseText(text = provider.label)
            }
        }
    }
}
