package kr.sdbk.bodyplan.feature.workoutlog.impl.routinedetail.composable

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
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.BodyPartWorkload
import kr.sdbk.bodyplan.core.ui.components.color
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.core.ui.components.volumeText

/** 루틴 하단에 붙는 부위별 종목수·세트수·무게와 그 합계. */
@Composable
internal fun RoutineSummaryCard(workloads: List<BodyPartWorkload>, modifier: Modifier = Modifier) {
    BodyPlanCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            workloads.forEach { workload ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(workload.bodyPart.color, CircleShape),
                    )
                    BaseText(
                        text = workload.bodyPart.label,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                    WeightSpacer()
                    BaseText(
                        text = workloadText(workload.entryCount, workload.setCount, workload.weightVolumeKg),
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
                    text = workloadText(
                        entryCount = workloads.sumOf { it.entryCount },
                        setCount = workloads.sumOf { it.setCount },
                        weightVolumeKg = workloads.mapNotNull { it.weightVolumeKg }.takeIf { it.isNotEmpty() }?.sum(),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                )
            }
        }
    }
}

private fun workloadText(entryCount: Int, setCount: Int, weightVolumeKg: Int?): String {
    val weight = weightVolumeKg?.let { volumeText(it) } ?: "-"
    return "${entryCount}종목 · ${setCount}세트 · $weight"
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun RoutineSummaryCardPreview() {
    BodyPlanTheme {
        RoutineSummaryCard(
            workloads = listOf(
                BodyPartWorkload(BodyPart.CHEST, entryCount = 3, setCount = 9, weightVolumeKg = 4200),
                BodyPartWorkload(BodyPart.BACK, entryCount = 2, setCount = 6, weightVolumeKg = 3100),
                BodyPartWorkload(BodyPart.CARDIO, entryCount = 1, setCount = 1, weightVolumeKg = null),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
