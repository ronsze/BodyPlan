package kr.sdbk.bodyplan.feature.my.impl.weight.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BaseTextField
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.PillChip
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary

/**
 * 체중을 넣는 칸. 고를 수 있는 날짜는 오늘과 어제뿐이라 캘린더 대신 칩 두 개를 둔다.
 *
 * [dates]는 오늘이 먼저다. 칩 문구는 [today]와 견줘 정한다.
 */
@Composable
internal fun WeightInputCard(
    dates: List<LocalDate>,
    today: LocalDate?,
    selectedDate: LocalDate?,
    input: String,
    canSave: Boolean,
    isSaving: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onChangeInput: (String) -> Unit,
    onClickSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyPlanCard(modifier = modifier) {
        BaseText(text = "오늘의 체중", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        VerticalSpacer(space = 12.dp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dates.forEach { date ->
                PillChip(
                    text = if (date == today) "오늘" else "어제",
                    selected = date == selectedDate,
                    onClick = { onSelectDate(date) },
                )
            }
        }

        VerticalSpacer(space = 12.dp)
        WeightInputBox(input = input, onChangeInput = onChangeInput)
        VerticalSpacer(space = 12.dp)
        PrimaryButton(text = if (isSaving) "저장 중..." else "저장", onClick = onClickSave, enabled = canSave)
    }
}

@Composable
private fun WeightInputBox(input: String, onChangeInput: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface)
            .border(BorderStroke(1.dp, Border), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BaseTextField(
            value = input,
            // 걸러 내는 일은 ViewModel이 한다 — 무엇이 유효한 입력인지가 저장 규칙과 같은 곳에 있어야 한다.
            onValueChange = onChangeInput,
            modifier = Modifier.weight(1f),
            placeholder = "72.4",
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        BaseText(text = "kg", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun WeightInputCardPreview() {
    val today = LocalDate.of(2026, 9, 10)
    BodyPlanTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            WeightInputCard(
                dates = listOf(today, today.minusDays(1)),
                today = today,
                selectedDate = today,
                input = "72.4",
                canSave = true,
                isSaving = false,
                onSelectDate = {},
                onChangeInput = {},
                onClickSave = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun WeightInputCardEmptyPreview() {
    val today = LocalDate.of(2026, 9, 10)
    BodyPlanTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            WeightInputCard(
                dates = listOf(today, today.minusDays(1)),
                today = today,
                selectedDate = today.minusDays(1),
                input = "",
                canSave = false,
                isSaving = false,
                onSelectDate = {},
                onChangeInput = {},
                onClickSave = {},
            )
        }
    }
}
