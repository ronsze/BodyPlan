package kr.sdbk.bodyplan.feature.my.impl.weight.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.feature.my.impl.weight.weightText

/** 남긴 기록. 최근 것이 위다. [records]는 이미 최신순으로 잘라 온 것이다. */
@Composable
internal fun WeightHistoryCard(records: List<WeightRecord>, isLoading: Boolean, modifier: Modifier = Modifier) {
    BodyPlanCard(modifier = modifier) {
        BaseText(text = "최근 기록", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        VerticalSpacer(space = 12.dp)

        when {
            isLoading -> BaseText(
                text = "불러오는 중...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
            )

            records.isEmpty() -> BaseText(
                text = "아직 기록이 없어요",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
            )

            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                records.forEach { record -> HistoryRow(record) }
            }
        }
    }
}

@Composable
private fun HistoryRow(record: WeightRecord) {
    Row(modifier = Modifier.fillMaxWidth()) {
        BaseText(
            text = DATE_FORMAT.format(record.date),
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
        )
        WeightSpacer()
        BaseText(
            text = "${weightText(record.weightKg)}kg",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일")

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun WeightHistoryCardPreview() {
    val today = LocalDate.of(2026, 9, 10)
    BodyPlanTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            WeightHistoryCard(
                records = listOf(
                    WeightRecord(today, 72.4),
                    WeightRecord(today.minusDays(1), 72.9),
                    WeightRecord(today.minusDays(3), 73.2),
                ),
                isLoading = false,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun WeightHistoryCardEmptyPreview() {
    BodyPlanTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            WeightHistoryCard(records = emptyList(), isLoading = false)
        }
    }
}
