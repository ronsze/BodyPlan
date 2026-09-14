package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
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
 *
 * [collapsedWeekOf]를 주면 접었다 펼 수 있다 — 접힌 동안은 그 날짜가 든 한 주만 그리고 달 이동을 막는다.
 * 접힘 상태를 호출부가 드는 것은 접을 때 보던 달을 되돌려야 해서다. 그 판단은 이 컴포넌트가 할 수 없다.
 */
@Composable
fun BodyPlanCalendar(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    cellHeight: Dp = DEFAULT_CELL_HEIGHT,
    collapsedWeekOf: LocalDate? = null,
    isExpanded: Boolean = true,
    onToggleExpanded: () -> Unit = {},
    dayContent: @Composable (LocalDate) -> Unit = {},
) {
    val isCollapsible = collapsedWeekOf != null
    val isCollapsed = isCollapsible && !isExpanded
    Column(modifier = modifier.fillMaxWidth()) {
        MonthHeader(yearMonth = yearMonth, canChangeMonth = !isCollapsed, onChangeMonth = onChangeMonth)
        BodyPlanCard(cornerRadius = 20.dp) {
            WeekdayHeader()
            MonthGrid(
                yearMonth = yearMonth,
                selectedDate = selectedDate,
                onSelectDate = onSelectDate,
                cellHeight = cellHeight,
                onlyWeekOf = collapsedWeekOf.takeIf { isCollapsed },
                dayContent = dayContent,
            )
            if (isCollapsible) {
                ExpandToggle(isExpanded = isExpanded, onClick = onToggleExpanded)
            }
        }
    }
}

/** [canChangeMonth]가 아니면 화살표를 그리지 않는다. 비활성으로 남기면 눌러도 안 되는 이유가 보이지 않는다. */
@Composable
private fun MonthHeader(yearMonth: YearMonth, canChangeMonth: Boolean, onChangeMonth: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        AnimatedVisibility(visible = canChangeMonth, enter = fadeIn(), exit = fadeOut()) {
            BodyPlanIcon(
                painter = BodyPlanIcons.ChevronLeft,
                contentDescription = "이전 달",
                boxSize = 20.dp,
                iconSize = 16.dp,
                tint = TextSecondary,
                modifier = Modifier.clickable { onChangeMonth(yearMonth.minusMonths(1)) },
            )
        }
        BaseText(
            text = yearMonth.atDay(1).format(MONTH_FORMAT),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        AnimatedVisibility(visible = canChangeMonth, enter = fadeIn(), exit = fadeOut()) {
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
}

@Composable
private fun ExpandToggle(isExpanded: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) -90f else 90f, label = "expandToggleRotation")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BaseText(
            text = if (isExpanded) "접기" else "펼치기",
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary,
        )
        BodyPlanIcon(
            painter = BodyPlanIcons.ChevronRight,
            contentDescription = null,
            boxSize = 20.dp,
            iconSize = 14.dp,
            tint = TextTertiary,
            modifier = Modifier.rotate(rotation),
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
                // 주말만 색으로 가른다 — 평일은 구분할 이유가 없다.
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

/**
 * [onlyWeekOf]가 있으면 그 날짜가 든 줄 하나만 보인다. 다른 달의 날짜면 아무 줄도 보이지 않는다 —
 * 그 주는 이 달의 격자에 없다.
 *
 * 줄을 빼고 그리는 대신 전부 그려 두고 가리는 것은 접고 펼 때 줄이 밀려 나오고 들어가게 하기 위해서다.
 */
@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    cellHeight: Dp,
    onlyWeekOf: LocalDate?,
    dayContent: @Composable (LocalDate) -> Unit,
) {
    val firstDay = yearMonth.atDay(1)
    // 일요일을 한 주의 첫 칸으로 둔다. DayOfWeek는 월요일이 1이라 7로 나눈 나머지가 곧 칸 번호다.
    val leadingBlanks = firstDay.dayOfWeek.value % DAYS_IN_WEEK
    val lengthOfMonth = yearMonth.lengthOfMonth()
    // 달마다 필요한 주 수가 다르다. 6줄로 고정하면 5주로 끝나는 달에 빈 줄만큼 여백이 남는다.
    val weekRows = ceil((leadingBlanks + lengthOfMonth) / DAYS_IN_WEEK.toFloat()).toInt()
    val keptRow = onlyWeekOf?.takeIf { YearMonth.from(it) == yearMonth }
        ?.let { (leadingBlanks + it.dayOfMonth - 1) / DAYS_IN_WEEK }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(weekRows) { row ->
            AnimatedVisibility(
                visible = onlyWeekOf == null || row == keptRow,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                WeekRow(
                    row = row,
                    firstDay = firstDay,
                    leadingBlanks = leadingBlanks,
                    lengthOfMonth = lengthOfMonth,
                    selectedDate = selectedDate,
                    onSelectDate = onSelectDate,
                    cellHeight = cellHeight,
                    dayContent = dayContent,
                )
            }
        }
    }
}

@Composable
private fun WeekRow(
    row: Int,
    firstDay: LocalDate,
    leadingBlanks: Int,
    lengthOfMonth: Int,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    cellHeight: Dp,
    dayContent: @Composable (LocalDate) -> Unit,
) {
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

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun BodyPlanCalendarCollapsedPreview() {
    BodyPlanTheme {
        BodyPlanCalendar(
            yearMonth = YearMonth.of(2026, 9),
            selectedDate = LocalDate.of(2026, 9, 8),
            onSelectDate = {},
            onChangeMonth = {},
            modifier = Modifier.padding(16.dp),
            collapsedWeekOf = LocalDate.of(2026, 9, 8),
            isExpanded = false,
        )
    }
}
