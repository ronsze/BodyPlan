package kr.sdbk.bodyplan.feature.workoutlog.impl.calendar.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.YearMonth
import kr.sdbk.bodyplan.core.designsystem.component.BannerTone
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCalendar
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanIcons
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.InfoBanner
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.DayStatus
import kr.sdbk.bodyplan.core.ui.components.DayStatusIndicator
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutAnalysisPeriod
import kr.sdbk.bodyplan.feature.workoutlog.impl.calendar.WorkoutCalendarEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.calendar.WorkoutCalendarIntent
import kr.sdbk.bodyplan.feature.workoutlog.impl.calendar.WorkoutCalendarState
import kr.sdbk.bodyplan.feature.workoutlog.impl.calendar.WorkoutCalendarViewModel

internal data class WorkoutCalendarEvents(
    val goToLog: (LocalDate) -> Unit,
    val goToExerciseManage: () -> Unit,
    val goToExerciseTrend: () -> Unit,
    val goToAnalysis: (WorkoutAnalysisPeriod, LocalDate) -> Unit,
)

internal data class WorkoutCalendarUiEvents(
    val onSelectDate: (LocalDate) -> Unit,
    val onClickWeeklyAnalysis: () -> Unit,
    val onClickMonthlyAnalysis: () -> Unit,
    val onChangeMonth: (YearMonth) -> Unit,
    val onClickManageExercise: () -> Unit,
    val onClickExerciseTrend: () -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun WorkoutCalendarView(events: WorkoutCalendarEvents, viewModel: WorkoutCalendarViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel, state.yearMonth, state.today)

    WorkoutCalendarViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is WorkoutCalendarEffect.NavigateToLog -> events.goToLog(effect.date)
            is WorkoutCalendarEffect.NavigateToExerciseManage -> events.goToExerciseManage()
            is WorkoutCalendarEffect.NavigateToExerciseTrend -> events.goToExerciseTrend()
        }
    }
}

/**
 * 분석 진입은 보고 있는 달과 오늘을 캡처하므로 키를 준다.
 * 키가 없으면 달을 넘겨도 처음 들어온 달을 계속 분석하게 된다.
 *
 * 주간은 이번 달을 보고 있을 때만 낸다. 지난 달의 어느 주를 뜻하는지 버튼 하나로는 정할 수 없다.
 */
@Composable
private fun rememberUiEvents(
    events: WorkoutCalendarEvents,
    viewModel: WorkoutCalendarViewModel,
    yearMonth: YearMonth,
    today: LocalDate,
): WorkoutCalendarUiEvents = remember(yearMonth, today) {
    val isCurrentMonth = YearMonth.from(today) == yearMonth
    val dateInMonth = if (isCurrentMonth) today else yearMonth.atDay(1)
    WorkoutCalendarUiEvents(
        onSelectDate = { viewModel.handleIntent(WorkoutCalendarIntent.ClickDate(it)) },
        onClickWeeklyAnalysis = { events.goToAnalysis(WorkoutAnalysisPeriod.WEEKLY, today) },
        onClickMonthlyAnalysis = { events.goToAnalysis(WorkoutAnalysisPeriod.MONTHLY, dateInMonth) },
        onChangeMonth = { viewModel.handleIntent(WorkoutCalendarIntent.ChangeMonth(it)) },
        onClickManageExercise = { viewModel.handleIntent(WorkoutCalendarIntent.ClickManageExercise) },
        onClickExerciseTrend = { viewModel.handleIntent(WorkoutCalendarIntent.ClickExerciseTrend) },
        onClickRetry = { viewModel.handleIntent(WorkoutCalendarIntent.ClickRetry) },
    )
}

@Composable
internal fun WorkoutCalendarViewImpl(state: WorkoutCalendarState, uiEvents: WorkoutCalendarUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(
            title = "운동 기록",
            actionText = "운동 종목 관리",
            onClickAction = uiEvents.onClickManageExercise,
            actionIcon = BodyPlanIcons.ChartLine,
            actionIconDescription = "종목 추이",
            onClickActionIcon = uiEvents.onClickExerciseTrend,
        )

        if (state.errorMessage != null) {
            ErrorContent(state.errorMessage, uiEvents.onClickRetry)
            return@Column
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                // 스크롤 끝에서 마지막 요소가 화면 아래에 붙지 않게 띄운다.
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BodyPlanCalendar(
                yearMonth = state.yearMonth,
                selectedDate = state.today,
                onSelectDate = uiEvents.onSelectDate,
                onChangeMonth = uiEvents.onChangeMonth,
                dayContent = { date ->
                    DayStatusIndicator(status = state.dayStatuses[date] ?: DayStatus.Pending)
                },
            )
            MonthSummaryBanner(state)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 지난 달에서는 주간을 내지 않는다. 어느 주를 뜻하는지 버튼 하나로는 정할 수 없다.
                if (isCurrentMonth(state)) {
                    OutlinedActionButton(
                        text = "이번 주 분석",
                        onClick = uiEvents.onClickWeeklyAnalysis,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedActionButton(
                    text = if (isCurrentMonth(state)) "이번 달 분석" else "이 달 분석",
                    onClick = uiEvents.onClickMonthlyAnalysis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 그 달의 기록을 한 줄로 요약한다. 운동한 날이 없으면 쉬어 가는 이야기로 바꾼다. */
@Composable
private fun MonthSummaryBanner(state: WorkoutCalendarState) {
    val recordedDays = state.dayStatuses.values.count { it is DayStatus.Recorded }
    if (recordedDays > 0) {
        InfoBanner(
            icon = BodyPlanIcons.ZapOff,
            title = "이번 달 벌써 ${recordedDays}일이나 운동했어요!",
            description = "꾸준한 노력은 배신하지 않아요.",
        )
    } else {
        InfoBanner(
            icon = BodyPlanIcons.Bed,
            title = "이번 달은 휴식이 많았어요",
            description = "몸을 회복하고 다시 도전해보세요.",
            tone = BannerTone.Muted,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onClickRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BaseText(text = message, color = TextSecondary)
            OutlinedActionButton(text = "다시 시도", onClick = onClickRetry)
        }
    }
}

private val previewStatuses = mapOf(
    LocalDate.of(2026, 9, 1) to DayStatus.Recorded(setOf(BodyPart.CHEST, BodyPart.TRICEPS)),
    LocalDate.of(2026, 9, 2) to DayStatus.Rest,
    LocalDate.of(2026, 9, 3) to DayStatus.Recorded(setOf(BodyPart.BACK)),
    LocalDate.of(2026, 9, 4) to DayStatus.Rest,
    LocalDate.of(2026, 9, 5) to DayStatus.Recorded(setOf(BodyPart.LEG, BodyPart.BICEPS)),
    LocalDate.of(2026, 9, 8) to DayStatus.Pending,
)

/** 보고 있는 달이 오늘이 든 달인지. 버튼 문구가 갈린다. */
private fun isCurrentMonth(state: WorkoutCalendarState): Boolean = YearMonth.from(state.today) == state.yearMonth

private val previewUiEvents = WorkoutCalendarUiEvents(
    onSelectDate = {},
    onClickWeeklyAnalysis = {},
    onClickMonthlyAnalysis = {},
    onChangeMonth = {},
    onClickManageExercise = {},
    onClickExerciseTrend = {},
    onClickRetry = {},
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WorkoutCalendarViewImplPreview() {
    BodyPlanTheme {
        WorkoutCalendarViewImpl(
            state = WorkoutCalendarState(
                yearMonth = YearMonth.of(2026, 9),
                today = LocalDate.of(2026, 9, 8),
                dayStatuses = previewStatuses,
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WorkoutCalendarViewImplRestPreview() {
    BodyPlanTheme {
        WorkoutCalendarViewImpl(
            state = WorkoutCalendarState(
                yearMonth = YearMonth.of(2026, 9),
                today = LocalDate.of(2026, 9, 8),
                dayStatuses = mapOf(LocalDate.of(2026, 9, 2) to DayStatus.Rest),
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WorkoutCalendarViewImplErrorPreview() {
    BodyPlanTheme {
        WorkoutCalendarViewImpl(
            state = WorkoutCalendarState(
                yearMonth = YearMonth.of(2026, 9),
                today = LocalDate.of(2026, 9, 8),
                errorMessage = "불러오지 못했습니다",
            ),
            uiEvents = previewUiEvents,
        )
    }
}
