package kr.sdbk.bodyplan.feature.workoutlog.impl.log.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Locale
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.HorizontalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Danger
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolumeComparison
import kr.sdbk.bodyplan.core.ui.components.color
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.core.ui.components.volumeText

/**
 * 부위별 볼륨을 사용자가 고른 날과 견준다. 날짜를 고르기 전에는 고르는 버튼만 보이고 비교는 나오지 않는다.
 */
@Composable
internal fun BodyPartVolumeComparisonCard(
    comparisonDate: LocalDate?,
    comparisons: List<BodyPartVolumeComparison>,
    onClickPickDate: () -> Unit,
    onClickClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyPlanCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BaseText(
                text = comparisonDate?.let { "${it.monthValue}/${it.dayOfMonth}과 비교" } ?: "다른 날과 비교",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            WeightSpacer()
            if (comparisonDate != null) {
                BaseText(
                    text = "해제",
                    modifier = Modifier.clickable(onClick = onClickClear),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
                HorizontalSpacer(space = 12.dp)
            }
            BaseText(
                text = if (comparisonDate == null) "날짜 선택" else "변경",
                modifier = Modifier.clickable(onClick = onClickPickDate),
                style = MaterialTheme.typography.bodySmall,
                color = Accent,
            )
        }
        if (comparisonDate != null) {
            VerticalSpacer(space = 12.dp)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                comparisons.forEach { ComparisonRow(it, comparisonDate) }
            }
        }
    }
}

@Composable
private fun ComparisonRow(comparison: BodyPartVolumeComparison, comparedDate: LocalDate) {
    val change = comparison.changeKg
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(comparison.bodyPart.color, CircleShape),
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            BaseText(
                text = comparison.bodyPart.label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            BaseText(
                text = comparison.comparedVolumeKg
                    ?.let { "${comparedDate.monthValue}/${comparedDate.dayOfMonth} · ${volumeText(it)}" }
                    ?: "그날 기록 없음",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
        WeightSpacer()
        BaseText(
            text = change?.let { changeText(it) } ?: "-",
            style = MaterialTheme.typography.titleSmall,
            color = changeColor(change),
        )
    }
}

private fun changeText(changeKg: Int): String = when {
    changeKg == 0 -> "변화 없음"
    else -> String.format(Locale.US, "%+,dkg", changeKg)
}

/** 상승·하락 전용 색이 디자인시스템에 없어 강조색과 위험색을 쓴다. 홈의 볼륨 추이 카드와 같은 기준이다. */
private fun changeColor(changeKg: Int?): Color = when {
    changeKg == null || changeKg == 0 -> TextTertiary
    changeKg > 0 -> Accent
    else -> Danger
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun BodyPartVolumeComparisonCardPreview() {
    BodyPlanTheme {
        BodyPartVolumeComparisonCard(
            comparisonDate = LocalDate.of(2026, 9, 24),
            comparisons = listOf(
                BodyPartVolumeComparison(BodyPart.CHEST, volumeKg = 1360, comparedVolumeKg = 1240),
                BodyPartVolumeComparison(BodyPart.BACK, volumeKg = 900, comparedVolumeKg = 1100),
                BodyPartVolumeComparison(BodyPart.LEG, volumeKg = 2000, comparedVolumeKg = null),
            ),
            onClickPickDate = {},
            onClickClear = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun BodyPartVolumeComparisonCardEmptyPreview() {
    BodyPlanTheme {
        BodyPartVolumeComparisonCard(
            comparisonDate = null,
            comparisons = emptyList(),
            onClickPickDate = {},
            onClickClear = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
