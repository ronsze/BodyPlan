package kr.sdbk.bodyplan.feature.dietlog.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.YearMonth
import kr.sdbk.bodyplan.core.designsystem.component.BaseImage
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCalendar
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state.errorMessage != null) {
            ErrorContent(state.errorMessage, uiEvents.onClickRetry)
            return@Column
        }

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        BodyPlanCalendar(
            yearMonth = state.yearMonth,
            selectedDate = state.today,
            onSelectDate = uiEvents.onSelectDate,
            onChangeMonth = uiEvents.onChangeMonth,
            dayContent = { date -> DayThumbnail(imagePath = state.imagesByDate[date]) },
        )
    }
}

@Composable
private fun DayThumbnail(imagePath: String?) {
    if (imagePath == null) return
    BaseImage(
        url = imagePath,
        contentDescription = null,
        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)),
        placeholder = ColorPainter(Color.LightGray),
    )
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
private fun DietCalendarViewImplPreview() {
    BodyPlanTheme {
        DietCalendarViewImpl(
            state = DietCalendarState(
                yearMonth = YearMonth.of(2026, 9),
                today = LocalDate.of(2026, 9, 7),
                imagesByDate = mapOf(
                    LocalDate.of(2026, 9, 3) to "/files/diet_images/a.jpg",
                    LocalDate.of(2026, 9, 5) to "/files/diet_images/b.jpg",
                ),
            ),
            uiEvents = DietCalendarUiEvents(
                onSelectDate = {},
                onChangeMonth = {},
                onClickRetry = {},
            ),
        )
    }
}
