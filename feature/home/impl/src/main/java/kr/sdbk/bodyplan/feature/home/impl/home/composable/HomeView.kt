package kr.sdbk.bodyplan.feature.home.impl.home.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.domain.model.ProgressDirection
import kr.sdbk.bodyplan.core.domain.model.ProgressHeadline
import kr.sdbk.bodyplan.core.domain.model.ProgressMetric
import kr.sdbk.bodyplan.core.domain.model.ProgressMetricKey
import kr.sdbk.bodyplan.core.domain.model.ProgressSummary
import kr.sdbk.bodyplan.feature.home.impl.home.HomeIntent
import kr.sdbk.bodyplan.feature.home.impl.home.HomeState
import kr.sdbk.bodyplan.feature.home.impl.home.HomeViewModel

internal data class HomeUiEvents(val onClickRetry: () -> Unit)

@Composable
internal fun HomeView(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(viewModel)

    HomeViewImpl(state = state, uiEvents = uiEvents)
}

@Composable
private fun rememberUiEvents(viewModel: HomeViewModel): HomeUiEvents = remember(viewModel) {
    HomeUiEvents(onClickRetry = { viewModel.handleIntent(HomeIntent.ClickRetry) })
}

@Composable
internal fun HomeViewImpl(state: HomeState, uiEvents: HomeUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = "홈")

        Box(modifier = Modifier.weight(1f)) {
            val summary = state.summary
            when {
                state.errorMessage != null -> ErrorContent(state.errorMessage, uiEvents.onClickRetry)
                summary == null -> LoadingContent()
                else -> SummaryContent(summary)
            }
        }
    }
}

@Composable
private fun SummaryContent(summary: ProgressSummary) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProgressCard(headline = summary.headline, metrics = summary.recentMetrics)
        BodyCompositionCard(metrics = summary.bodyCompositionMetrics)
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
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

private val previewUiEvents = HomeUiEvents(onClickRetry = {})

private val previewSummary = ProgressSummary(
    headline = ProgressHeadline.IMPROVING,
    recentMetrics = listOf(
        ProgressMetric(ProgressMetricKey.WEIGHT, -1.2, ProgressDirection.IMPROVING),
        ProgressMetric(ProgressMetricKey.WORKOUT_VOLUME, 1240.0, ProgressDirection.IMPROVING),
        ProgressMetric(ProgressMetricKey.WORKOUT_DAYS, -1.0, ProgressDirection.WORSENING),
    ),
    bodyCompositionMetrics = listOf(
        ProgressMetric(ProgressMetricKey.SKELETAL_MUSCLE, 0.8, ProgressDirection.IMPROVING),
        ProgressMetric(ProgressMetricKey.BODY_FAT, -0.4, ProgressDirection.IMPROVING),
    ),
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun HomeViewImplPreview() {
    BodyPlanTheme {
        HomeViewImpl(state = HomeState(summary = previewSummary), uiEvents = previewUiEvents)
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun HomeViewImplEmptyPreview() {
    BodyPlanTheme {
        HomeViewImpl(
            state = HomeState(
                summary = ProgressSummary(
                    headline = ProgressHeadline.NOT_ENOUGH_DATA,
                    recentMetrics = listOf(
                        ProgressMetric(ProgressMetricKey.WEIGHT, null, ProgressDirection.UNKNOWN),
                        ProgressMetric(ProgressMetricKey.WORKOUT_VOLUME, null, ProgressDirection.UNKNOWN),
                        ProgressMetric(ProgressMetricKey.WORKOUT_DAYS, null, ProgressDirection.UNKNOWN),
                    ),
                    bodyCompositionMetrics = listOf(
                        ProgressMetric(ProgressMetricKey.SKELETAL_MUSCLE, null, ProgressDirection.UNKNOWN),
                        ProgressMetric(ProgressMetricKey.BODY_FAT, null, ProgressDirection.UNKNOWN),
                    ),
                ),
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun HomeViewImplErrorPreview() {
    BodyPlanTheme {
        HomeViewImpl(state = HomeState(errorMessage = "불러오지 못했습니다"), uiEvents = previewUiEvents)
    }
}
