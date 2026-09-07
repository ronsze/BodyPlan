package kr.sdbk.bodyplan.feature.dietlog.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import kr.sdbk.bodyplan.core.domain.model.DietDayStatus
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect

internal data class DietCalendarEvents(val goToLog: (LocalDate) -> Unit)

internal data class DietCalendarUiEvents(
    val onSelectDate: (LocalDate) -> Unit,
    val onChangeMonth: (YearMonth) -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun DietCalendarView(events: DietCalendarEvents, viewModel: DietCalendarViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(viewModel)

    DietCalendarViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is DietCalendarEffect.NavigateToLog -> events.goToLog(effect.date)
        }
    }
}

@Composable
private fun rememberUiEvents(viewModel: DietCalendarViewModel): DietCalendarUiEvents = remember {
    DietCalendarUiEvents(
        onSelectDate = { viewModel.handleIntent(DietCalendarIntent.ClickDate(it)) },
        onChangeMonth = { viewModel.handleIntent(DietCalendarIntent.ChangeMonth(it)) },
        onClickRetry = { viewModel.handleIntent(DietCalendarIntent.ClickRetry) },
    )
}

@Composable
internal fun DietCalendarViewImpl(state: DietCalendarState, uiEvents: DietCalendarUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = "식단 기록")

        if (state.errorMessage != null) {
            ErrorContent(state.errorMessage, uiEvents.onClickRetry)
            return@Column
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BodyPlanCalendar(
                yearMonth = state.yearMonth,
                selectedDate = state.today,
                onSelectDate = uiEvents.onSelectDate,
                onChangeMonth = uiEvents.onChangeMonth,
                cellHeight = CELL_HEIGHT,
                dayContent = { date ->
                    DietDayStatusIndicator(
                        status = state.dayStatuses[date] ?: DietDayStatus.Pending,
                    )
                },
            )
            MonthSummaryBanner(
                recordedDays = state.dayStatuses.values.count { it is DietDayStatus.Recorded },
            )
        }
    }
}

@Composable
private fun MonthSummaryBanner(recordedDays: Int) {
    if (recordedDays > 0) {
        InfoBanner(
            icon = BodyPlanIcons.ForkKnifeCrossed,
            title = "이번 달 ${recordedDays}일치 식단을 남겼어요",
            description = "먹은 것을 남기면 습관이 보여요.",
        )
    } else {
        InfoBanner(
            icon = BodyPlanIcons.CameraOff,
            title = "이번 달 식단 기록이 없어요",
            description = "오늘 먹은 것부터 한 장 남겨 보세요.",
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

// 사진이 무엇인지 알아볼 수 있어야 달력에 두는 뜻이 산다. 그만큼 셀도 높인다.
private val CELL_HEIGHT = 76.dp

private val previewUiEvents = DietCalendarUiEvents(
    onSelectDate = {},
    onChangeMonth = {},
    onClickRetry = {},
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun DietCalendarViewImplPreview() {
    BodyPlanTheme {
        DietCalendarViewImpl(
            state = DietCalendarState(
                yearMonth = YearMonth.of(2026, 9),
                today = LocalDate.of(2026, 9, 8),
                dayStatuses = mapOf(
                    LocalDate.of(2026, 9, 3) to DietDayStatus.Recorded("/files/diet_images/a.jpg"),
                    LocalDate.of(2026, 9, 4) to DietDayStatus.Missed,
                    LocalDate.of(2026, 9, 5) to DietDayStatus.Recorded("/files/diet_images/b.jpg"),
                    LocalDate.of(2026, 9, 6) to DietDayStatus.Missed,
                    LocalDate.of(2026, 9, 8) to DietDayStatus.Pending,
                ),
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun DietCalendarViewImplEmptyPreview() {
    BodyPlanTheme {
        DietCalendarViewImpl(
            state = DietCalendarState(
                yearMonth = YearMonth.of(2026, 9),
                today = LocalDate.of(2026, 9, 8),
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun DietCalendarViewImplErrorPreview() {
    BodyPlanTheme {
        DietCalendarViewImpl(
            state = DietCalendarState(
                yearMonth = YearMonth.of(2026, 9),
                today = LocalDate.of(2026, 9, 8),
                errorMessage = "불러오지 못했습니다",
            ),
            uiEvents = previewUiEvents,
        )
    }
}
