package kr.sdbk.bodyplan.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.Badge
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet

/** 종목 하나와 그 세트를 보이는 카드. 일간 일지와 루틴 상세가 같은 표기를 쓴다. */
@Composable
fun WorkoutEntryCard(
    entry: WorkoutEntry,
    isEditable: Boolean,
    onClick: () -> Unit,
    onClickDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyPlanCard(
        modifier = if (isEditable) modifier.clickable(onClick = onClick) else modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BaseText(
                text = entry.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            Badge(text = entry.bodyPart.label, modifier = Modifier.padding(start = 8.dp))
            WeightSpacer()
            if (isEditable) {
                BaseText(
                    text = "삭제",
                    modifier = Modifier.clickable(onClick = onClickDelete),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
        }
        VerticalSpacer(space = 14.dp)
        HorizontalDivider(color = Border)
        VerticalSpacer(space = 14.dp)
        entry.sets.forEachIndexed { index, set ->
            if (index > 0) VerticalSpacer(space = 8.dp)
            SetRow(setNumber = index + 1, set = set)
        }
    }
}

@Composable
private fun SetRow(setNumber: Int, set: WorkoutSet) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BaseText(
            text = "${setNumber}세트",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        BaseText(
            text = set.summaryText(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
        )
    }
}

/** 세트 한 줄의 표기. 시간으로 재는 종목은 한 세트가 한 회차라 횟수를 적지 않는다. */
private fun WorkoutSet.summaryText(): String = when (intensity) {
    is Intensity.Weight -> "${intensity.value}kg × ${repeatCount}회"
    is Intensity.Angle -> "${intensity.value}도 × ${repeatCount}회"
    is Intensity.Duration -> "${intensity.value}분"
}

@Preview(showBackground = true)
@Composable
private fun WorkoutEntryCardPreview() {
    BodyPlanTheme {
        WorkoutEntryCard(
            entry = WorkoutEntry(
                id = 1L,
                exerciseId = 1L,
                exerciseName = "인클라인 벤치프레스 머신",
                bodyPart = BodyPart.CHEST,
                intensityType = IntensityType.WEIGHT,
                sets = listOf(
                    WorkoutSet(repeatCount = 4, intensity = Intensity.Weight(10)),
                    WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(15)),
                ),
            ),
            isEditable = true,
            onClick = {},
            onClickDelete = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
