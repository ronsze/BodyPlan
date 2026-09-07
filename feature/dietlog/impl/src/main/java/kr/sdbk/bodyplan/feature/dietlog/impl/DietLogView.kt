package kr.sdbk.bodyplan.feature.dietlog.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect

internal data class DietLogEvents(val goBack: () -> Unit)

internal data class DietLogUiEvents(val onBackPressed: () -> Unit)

@Composable
internal fun DietLogView(events: DietLogEvents, viewModel: DietLogViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)

    DietLogViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            else -> Unit
        }
    }
}

@Composable
private fun rememberUiEvents(events: DietLogEvents, viewModel: DietLogViewModel): DietLogUiEvents =
    remember {
        DietLogUiEvents(
            onBackPressed = events.goBack,
        )
    }

@Composable
internal fun DietLogViewImpl(state: DietLogState, uiEvents: DietLogUiEvents) {
    BaseText(text = "DietLog")
}

@Preview(showBackground = true)
@Composable
private fun DietLogViewImplPreview() {
    BodyPlanTheme {
        DietLogViewImpl(
            state = DietLogState(),
            uiEvents = DietLogUiEvents(onBackPressed = {}),
        )
    }
}
