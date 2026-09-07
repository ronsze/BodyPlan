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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Danger
import kr.sdbk.bodyplan.core.designsystem.theme.OnAccent
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary

/**
 * 월 단위 날짜 격자.
 *
 * 셀 안에 무엇을 표시할지는 [dayContent]로 호출부가 정한다 — 기능마다 그리는 것이 달라
 * 이 컴포넌트는 날짜 배치와 이동만 맡는다. [selectedDate]는 강조 원으로 그린다.
 *
 * [cellHeight]도 호출부가 정한다. 점 몇 개를 찍는 화면과 사진을 보여주는 화면은 필요한 높이가 다르다.
 */
@Composable
fun BodyPlanCalendar(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    cellHeight: Dp = DEFAULT_CELL_HEIGHT,
    dayContent: @Composable (LocalDate) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MonthHeader(yearMonth = yearMonth, onChangeMonth = onChangeMonth)
        BodyPlanCard(cornerRadius = 20.dp) {
            WeekdayHeader()
            MonthGrid(
                yearMonth = yearMonth,
                selectedDate = selectedDate,
                onSelectDate = onSelectDate,
                cellHeight = cellHeight,
                dayContent = dayContent,
            )
        }
    }
}

@Composable
private fun MonthHeader(yearMonth: YearMonth, onChangeMonth: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        BodyPlanIcon(
            painter = BodyPlanIcons.ChevronLeft,
            contentDescription = "이전 달",
            boxSize = 20.dp,
            iconSize = 16.dp,
            tint = TextSecondary,
            modifier = Modifier.clickable { onChangeMonth(yearMonth.minusMonths(1)) },
        )
        BaseText(
            text = yearMonth.atDay(1).format(MONTH_FORMAT),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        BodyPlanIcon(
            painter = BodyPlanIcons.ChevronRight,
            contentDescription = "다음 달",
            boxSize = 20.dp,
            iconSize = 16.dp,
            tint = TextSecondary,
            modifier = Modifier.clickable { onChangeMonth(yearMonth.plusMonths(1)) },
        )
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WEEKDAY_LABELS.forEachIndexed { index, label ->
            BaseText(
                text = label,
                modifier = Modifier.weight(1f),
                // 주말만 색으로 구분한다. 일요일은 붉게, 토요일은 강조색으로.
                color = when (index) {
                    0 -> Danger
                    DAYS_IN_WEEK - 1 -> Accent
                    else -> TextTertiary
                },
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    cellHeight: Dp,
    dayContent: @Composable (LocalDate) -> Unit,
) {
    val firstDay = yearMonth.atDay(1)
    // 일요일을 한 주의 첫 칸으로 둔다. DayOfWeek는 월요일이 1이라 7로 나눈 나머지가 곧 칸 번호다.
    val leadingBlanks = firstDay.dayOfWeek.value % DAYS_IN_WEEK
    val lengthOfMonth = yearMonth.lengthOfMonth()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(WEEK_ROWS) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(DAYS_IN_WEEK) { column ->
                    val dayNumber = row * DAYS_IN_WEEK + column - leadingBlanks + 1
                    if (dayNumber in 1..lengthOfMonth) {
                        DayCell(
                            date = firstDay.withDayOfMonth(dayNumber),
                            selectedDate = selectedDate,
                            onSelectDate = onSelectDate,
                            cellHeight = cellHeight,
                            dayContent = dayContent,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight),
                        )
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
    cellHeight: Dp,
    dayContent: @Composable (LocalDate) -> Unit,
) {
    val isSelected = date == selectedDate

    Column(
        modifier = Modifier
            .weight(1f)
            .height(cellHeight)
            .clickable { onSelectDate(date) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isSelected) Accent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            BaseText(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) OnAccent else TextPrimary,
                textAlign = TextAlign.Center,
            )
        }
        dayContent(date)
    }
}

private val WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")
private const val DAYS_IN_WEEK = 7
private const val WEEK_ROWS = 6
private val DEFAULT_CELL_HEIGHT = 56.dp
private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월")

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun BodyPlanCalendarPreview() {
    BodyPlanTheme {
        BodyPlanCalendar(
            yearMonth = YearMonth.of(2026, 9),
            selectedDate = LocalDate.of(2026, 9, 8),
            onSelectDate = {},
            onChangeMonth = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
