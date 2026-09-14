package kr.sdbk.bodyplan.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolume

/**
 * 부위별 무게 볼륨을 줄로 늘어놓고 합계를 단다. 하루 기록과 이번 주가 같은 카드를 쓴다.
 *
 * 로딩 표시를 두지 않는다 — 값이 오기 전엔 빈 목록이라 [emptyText]가 잠깐 보이고, 그게 스피너보다 덜 흔들린다.
 * [errorMessage]가 있으면 목록 대신 그것과 다시 시도를 보인다.
 */
@Composable
fun BodyPartVolumeCard(
    title: String,
    volumes: List<BodyPartVolume>,
    emptyText: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    onClickRetry: () -> Unit = {},
) {
    BodyPlanCard(modifier = modifier) {
        BaseText(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
        )
        VerticalSpacer(space = 12.dp)
        when {
            errorMessage != null -> ErrorContent(errorMessage, onClickRetry)

            volumes.isEmpty() ->
                BaseText(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary,
                )

            else -> VolumeRows(volumes)
        }
    }
}

@Composable
private fun VolumeRows(volumes: List<BodyPartVolume>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        volumes.forEach { volume ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(volume.bodyPart.color, CircleShape),
                )
                BaseText(
                    text = volume.bodyPart.label,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
                WeightSpacer()
                BaseText(
                    text = volumeText(volume.weightVolumeKg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
            }
        }
        HorizontalDivider(color = Border)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BaseText(
                text = "합계",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            WeightSpacer()
            BaseText(
                text = volumeText(volumes.sumOf { it.weightVolumeKg }),
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onClickRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BaseText(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        OutlinedActionButton(text = "다시 시도", onClick = onClickRetry)
    }
}

/** `1,240kg` 꼴. 천 단위 구분은 로케일과 무관하게 쉼표로 고정한다. 홈의 추이 카드도 같은 표기를 쓴다. */
fun volumeText(kg: Int): String = String.format(Locale.US, "%,dkg", kg)

private val previewVolumes = listOf(
    BodyPartVolume(BodyPart.CHEST, 1240),
    BodyPartVolume(BodyPart.SHOULDER, 360),
    BodyPartVolume(BodyPart.TRICEPS, 0),
)

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun BodyPartVolumeCardPreview() {
    BodyPlanTheme {
        BodyPartVolumeCard(
            title = "부위별 볼륨",
            volumes = previewVolumes,
            emptyText = "무게 기록이 없습니다",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun BodyPartVolumeCardEmptyPreview() {
    BodyPlanTheme {
        BodyPartVolumeCard(
            title = "이번 주 부위별 볼륨",
            volumes = emptyList(),
            emptyText = "이번 주 무게 기록이 없습니다",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun BodyPartVolumeCardErrorPreview() {
    BodyPlanTheme {
        BodyPartVolumeCard(
            title = "이번 주 부위별 볼륨",
            volumes = emptyList(),
            emptyText = "이번 주 무게 기록이 없습니다",
            modifier = Modifier.padding(16.dp),
            errorMessage = "불러오지 못했습니다",
        )
    }
}
