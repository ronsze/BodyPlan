package kr.sdbk.bodyplan.feature.my.impl.inbodyadd.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.YearMonth
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCalendar
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.fullText

/** 고른 측정일을 보이고, 누르면 선택창을 연다. */
@Composable
internal fun MeasuredDateField(measuredOn: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BaseText(
            text = measuredOn.fullText(),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
        WeightSpacer()
        BaseText(text = "변경", style = MaterialTheme.typography.bodySmall, color = Accent)
    }
}

/**
 * 측정일을 고르는 팝업. 오늘 이후 날짜는 눌러도 고르지 않는다.
 *
 * 운동 기록의 비교 날짜 선택창과 같은 방식이다. 다른 feature의 구현은 가져다 쓸 수 없어 따로 둔다.
 */
@Composable
internal fun MeasuredDatePickerDialog(
    selected: LocalDate,
    today: LocalDate,
    yearMonth: YearMonth,
    onSelect: (LocalDate) -> Unit,
    onChangeMonth: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            BaseText(
                text = "측정 날짜 선택",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            BodyPlanCalendar(
                yearMonth = yearMonth,
                selectedDate = selected,
                onSelectDate = { date -> if (!date.isAfter(today)) onSelect(date) },
                onChangeMonth = onChangeMonth,
            )
            BaseText(
                text = "닫기",
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onDismiss)
                    .padding(top = 12.dp, start = 12.dp, bottom = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = TextTertiary,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun MeasuredDateFieldPreview() {
    BodyPlanTheme {
        MeasuredDateField(measuredOn = LocalDate.of(2026, 9, 29), onClick = {}, modifier = Modifier.padding(16.dp))
    }
}
