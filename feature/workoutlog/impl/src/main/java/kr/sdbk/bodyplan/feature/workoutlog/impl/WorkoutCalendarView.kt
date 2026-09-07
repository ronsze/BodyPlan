package kr.sdbk.bodyplan.feature.workoutlog.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
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
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCalendar
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.DayStatus
import kr.sdbk.bodyplan.core.ui.components.DayStatusIndicator
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect

internal data class WorkoutCalendarEvents(val goToLog: (LocalDate) -> Unit, val goToExerciseManage: () -> Unit)

internal data class WorkoutCalendarUiEvents(
    val onSelectDate: (LocalDate) -> Unit,
    val onChangeMonth: (YearMonth) -> Unit,
    val onClickManageExercise: () -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun WorkoutCalendarView(events: WorkoutCalendarEvents, viewModel: WorkoutCalendarViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(viewModel)

    WorkoutCalendarViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is WorkoutCalendarEffect.NavigateToLog -> events.goToLog(effect.date)
            is WorkoutCalendarEffect.NavigateToExerciseManage -> events.goToExerciseManage()
        }
    }
}

@Composable
private fun rememberUiEvents(viewModel: WorkoutCalendarViewModel): WorkoutCalendarUiEvents = remember {
    WorkoutCalendarUiEvents(
        onSelectDate = { viewModel.handleIntent(WorkoutCalendarIntent.ClickDate(it)) },
        onClickManageExercise = { viewModel.handleIntent(WorkoutCalendarIntent.ClickManageExercise) },
        onChangeMonth = { viewModel.handleIntent(WorkoutCalendarIntent.ChangeMonth(it)) },
        onClickRetry = { viewModel.handleIntent(WorkoutCalendarIntent.ClickRetry) },
    )
}

@Composable
internal fun WorkoutCalendarViewImpl(state: WorkoutCalendarState, uiEvents: WorkoutCalendarUiEvents) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state.errorMessage != null) {
            ErrorContent(state.errorMessage, uiEvents.onClickRetry)
            return@Column
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = uiEvents.onClickManageExercise) {
                BaseText(text = "운동 종목 관리")
            }
        }

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        BodyPlanCalendar(
            yearMonth = state.yearMonth,
            selectedDate = state.today,
            onSelectDate = uiEvents.onSelectDate,
            onChangeMonth = uiEvents.onChangeMonth,
            dayContent = { date ->
                DayStatusIndicator(status = state.dayStatuses[date] ?: DayStatus.Pending)
            },
        )
    }
}

@Composable
private fun ErrorContent(message: String, onClickRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BaseText(text = message)
            TextButton(onClick = onClickRetry) { BaseText(text = "다시 시도") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutCalendarViewImplPreview() {
    BodyPlanTheme {
        WorkoutCalendarViewImpl(
            state = WorkoutCalendarState(
                yearMonth = YearMonth.of(2026, 9),
                today = LocalDate.of(2026, 9, 7),
                dayStatuses = mapOf(
                    LocalDate.of(2026, 9, 1) to DayStatus.Recorded(setOf(BodyPart.CHEST, BodyPart.TRICEPS)),
                    LocalDate.of(2026, 9, 2) to DayStatus.Rest,
                    LocalDate.of(2026, 9, 3) to DayStatus.Recorded(setOf(BodyPart.BACK)),
                    LocalDate.of(2026, 9, 7) to DayStatus.Pending,
                    LocalDate.of(2026, 9, 20) to DayStatus.Upcoming,
                ),
            ),
            uiEvents = WorkoutCalendarUiEvents(
                onSelectDate = {},
                onChangeMonth = {},
                onClickManageExercise = {},
                onClickRetry = {},
            ),
        )
    }
}
