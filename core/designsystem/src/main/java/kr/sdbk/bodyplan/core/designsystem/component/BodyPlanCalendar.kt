package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme

/**
 * 월 단위 날짜 격자.
 *
 * 셀 안에 무엇을 표시할지는 [dayContent]로 호출부가 정한다 — 기능마다 그리는 것이 달라
 * 이 컴포넌트는 날짜 배치와 이동만 맡는다.
 */
@Composable
fun BodyPlanCalendar(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    dayContent: @Composable (LocalDate) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MonthHeader(yearMonth = yearMonth, onChangeMonth = onChangeMonth)
        WeekdayHeader()
        MonthGrid(
            yearMonth = yearMonth,
            selectedDate = selectedDate,
            onSelectDate = onSelectDate,
            dayContent = dayContent,
        )
    }
}

@Composable
private fun MonthHeader(yearMonth: YearMonth, onChangeMonth: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        TextButton(onClick = { onChangeMonth(yearMonth.minusMonths(1)) }) {
            BaseText(text = "이전")
        }
        BaseText(
            text = yearMonth.atDay(1).format(MONTH_FORMAT),
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = { onChangeMonth(yearMonth.plusMonths(1)) }) {
            BaseText(text = "다음")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        WEEKDAY_LABELS.forEach { label ->
            BaseText(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    dayContent: @Composable (LocalDate) -> Unit,
) {
    val firstDay = yearMonth.atDay(1)
    // 일요일을 한 주의 첫 칸으로 둔다. DayOfWeek는 월요일이 1이라 7로 나눈 나머지가 곧 칸 번호다.
    val leadingBlanks = firstDay.dayOfWeek.value % DAYS_IN_WEEK
    val lengthOfMonth = yearMonth.lengthOfMonth()

    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(WEEK_ROWS) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(DAYS_IN_WEEK) { column ->
                    val dayNumber = row * DAYS_IN_WEEK + column - leadingBlanks + 1
                    if (dayNumber in 1..lengthOfMonth) {
                        DayCell(
                            date = firstDay.withDayOfMonth(dayNumber),
                            selectedDate = selectedDate,
                            onSelectDate = onSelectDate,
                            dayContent = dayContent,
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).height(CELL_HEIGHT))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    date: LocalDate,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    dayContent: @Composable (LocalDate) -> Unit,
) {
    val isSelected = date == selectedDate
    val background =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    Column(
        modifier = Modifier
            .weight(1f)
            .height(CELL_HEIGHT)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable { onSelectDate(date) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BaseText(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
        )
        dayContent(date)
    }
}

private val WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")
private const val DAYS_IN_WEEK = 7
private const val WEEK_ROWS = 6
private val CELL_HEIGHT = 52.dp
private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월")

@Preview(showBackground = true)
@Composable
private fun BodyPlanCalendarPreview() {
    BodyPlanTheme {
        BodyPlanCalendar(
            yearMonth = YearMonth.of(2026, 9),
            selectedDate = LocalDate.of(2026, 9, 7),
            onSelectDate = {},
            onChangeMonth = {},
        )
    }
}
