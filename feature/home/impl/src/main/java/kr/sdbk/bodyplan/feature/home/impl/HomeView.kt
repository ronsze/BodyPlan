package kr.sdbk.bodyplan.feature.home.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect

internal data class HomeEvents(val goBack: () -> Unit)

internal data class HomeUiEvents(val onBackPressed: () -> Unit)

@Composable
internal fun HomeView(events: HomeEvents, viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)

    HomeViewImpl(
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
private fun rememberUiEvents(events: HomeEvents, viewModel: HomeViewModel): HomeUiEvents = remember {
    HomeUiEvents(
        onBackPressed = events.goBack,
    )
}

@Composable
internal fun HomeViewImpl(state: HomeState, uiEvents: HomeUiEvents) {
    BaseText(text = "BodyPlan")
}

@Preview(showBackground = true)
@Composable
private fun HomeViewImplPreview() {
    BodyPlanTheme {
        HomeViewImpl(
            state = HomeState(),
            uiEvents = HomeUiEvents(onBackPressed = {}),
        )
    }
}
