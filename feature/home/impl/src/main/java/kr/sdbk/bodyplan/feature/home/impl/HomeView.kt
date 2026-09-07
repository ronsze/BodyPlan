package kr.sdbk.bodyplan.feature.home.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect

internal data class HomeEvents(val goBack: () -> Unit, val goToWorkoutCalendar: () -> Unit)

internal data class HomeUiEvents(val onBackPressed: () -> Unit, val onClickWorkoutLog: () -> Unit)

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
        onClickWorkoutLog = events.goToWorkoutCalendar,
    )
}

@Composable
internal fun HomeViewImpl(state: HomeState, uiEvents: HomeUiEvents) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        BaseText(text = "BodyPlan", style = MaterialTheme.typography.headlineSmall)
        VerticalSpacer(space = 16.dp)
        Button(onClick = uiEvents.onClickWorkoutLog) {
            BaseText(text = "운동 일지")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeViewImplPreview() {
    BodyPlanTheme {
        HomeViewImpl(
            state = HomeState(),
            uiEvents = HomeUiEvents(onBackPressed = {}, onClickWorkoutLog = {}),
        )
    }
}
