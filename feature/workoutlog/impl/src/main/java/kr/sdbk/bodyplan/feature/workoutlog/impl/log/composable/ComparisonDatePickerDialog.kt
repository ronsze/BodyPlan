package kr.sdbk.bodyplan.feature.workoutlog.impl.log.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.DayStatus
import kr.sdbk.bodyplan.core.ui.components.DayStatusIndicator

/**
 * 볼륨을 견줄 날을 고르는 팝업. 운동 기록 캘린더와 같은 격자·같은 셀 표시(부위·휴식·미기록)를 쓴다.
 * 어느 날이 운동한 날인지 보면서 골라야 견줄 날을 찾기 쉽다.
 *
 * [logDate]와 그 이후 날짜는 견줄 기록이 아니라 눌러도 고르지 않는다.
 */
@Composable
internal fun ComparisonDatePickerDialog(
    logDate: LocalDate,
    selected: LocalDate?,
    yearMonth: YearMonth,
    dayStatuses: Map<LocalDate, DayStatus>,
    onSelect: (LocalDate) -> Unit,
    onChangeMonth: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            BaseText(
                text = "비교할 날짜 선택",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            BodyPlanCalendar(
                yearMonth = yearMonth,
                selectedDate = selected,
                onSelectDate = { date -> if (date.isBefore(logDate)) onSelect(date) },
                onChangeMonth = onChangeMonth,
                cellHeight = CELL_HEIGHT,
                dayContent = { date ->
                    DayStatusIndicator(status = dayStatuses[date] ?: DayStatus.Pending)
                },
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

/** 운동 기록 캘린더와 같은 칸 높이다. 부위를 이름으로 적어 두 줄까지 늘어난다. */
private val CELL_HEIGHT = 72.dp

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ComparisonDatePickerDialogPreview() {
    BodyPlanTheme {
        ComparisonDatePickerDialog(
            logDate = LocalDate.of(2026, 9, 8),
            selected = LocalDate.of(2026, 9, 3),
            yearMonth = YearMonth.of(2026, 9),
            dayStatuses = mapOf(
                LocalDate.of(2026, 9, 1) to DayStatus.Recorded(setOf(BodyPart.CHEST, BodyPart.TRICEPS)),
                LocalDate.of(2026, 9, 3) to DayStatus.Recorded(setOf(BodyPart.BACK)),
            ),
            onSelect = {},
            onChangeMonth = {},
            onDismiss = {},
        )
    }
}
