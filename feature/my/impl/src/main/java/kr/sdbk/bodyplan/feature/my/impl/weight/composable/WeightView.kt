package kr.sdbk.bodyplan.feature.my.impl.weight.composable

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.core.domain.model.WeightTrend
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.my.impl.weight.WeightEffect
import kr.sdbk.bodyplan.feature.my.impl.weight.WeightIntent
import kr.sdbk.bodyplan.feature.my.impl.weight.WeightState
import kr.sdbk.bodyplan.feature.my.impl.weight.WeightViewModel

internal data class WeightEvents(val goBack: () -> Unit)

internal data class WeightUiEvents(
    val onBackPressed: () -> Unit,
    val onSelectDate: (LocalDate) -> Unit,
    val onChangeInput: (String) -> Unit,
    val onClickSave: () -> Unit,
)

@Composable
internal fun WeightView(events: WeightEvents, viewModel: WeightViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(viewModel)
    val context = LocalContext.current

    WeightViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is WeightEffect.GoBack -> events.goBack()

            is WeightEffect.ShowMessage ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun rememberUiEvents(viewModel: WeightViewModel): WeightUiEvents = remember {
    WeightUiEvents(
        onBackPressed = { viewModel.handleIntent(WeightIntent.ClickBack) },
        onSelectDate = { viewModel.handleIntent(WeightIntent.SelectDate(it)) },
        onChangeInput = { viewModel.handleIntent(WeightIntent.ChangeInput(it)) },
        onClickSave = { viewModel.handleIntent(WeightIntent.ClickSave) },
    )
}

@Composable
internal fun WeightViewImpl(state: WeightState, uiEvents: WeightUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = "체중 기록", onBack = uiEvents.onBackPressed)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WeightInputCard(
                dates = state.editableDates,
                today = state.today,
                selectedDate = state.selectedDate,
                input = state.input,
                canSave = state.canSave,
                isSaving = state.isSaving,
                onSelectDate = uiEvents.onSelectDate,
                onChangeInput = uiEvents.onChangeInput,
                onClickSave = uiEvents.onClickSave,
            )
            WeightTrendCard(trend = state.trend)
            WeightHistoryCard(records = state.recentRecords, isLoading = state.isLoading)
        }
    }
}

private val previewUiEvents = WeightUiEvents(
    onBackPressed = {},
    onSelectDate = {},
    onChangeInput = {},
    onClickSave = {},
)

private val previewToday = LocalDate.of(2026, 9, 10)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WeightViewImplPreview() {
    BodyPlanTheme {
        WeightViewImpl(
            state = WeightState(
                today = previewToday,
                selectedDate = previewToday,
                input = "72.4",
                records = listOf(
                    WeightRecord(previewToday.minusDays(3), 73.2),
                    WeightRecord(previewToday.minusDays(1), 72.9),
                    WeightRecord(previewToday, 72.4),
                ),
                trend = WeightTrend(
                    dailyChangeKg = -0.5,
                    weeklyAverageChangeKg = -1.2,
                    monthlyAverageChangeKg = 0.3,
                ),
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WeightViewImplEmptyPreview() {
    BodyPlanTheme {
        WeightViewImpl(
            state = WeightState(today = previewToday, selectedDate = previewToday),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WeightViewImplLoadingPreview() {
    BodyPlanTheme {
        WeightViewImpl(
            state = WeightState(today = previewToday, selectedDate = previewToday, isLoading = true),
            uiEvents = previewUiEvents,
        )
    }
}
