package kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import java.time.LocalDate
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.ItemChip
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrend
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrendMetric
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrendPoint
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.RecordedExercise
import kr.sdbk.bodyplan.core.ui.components.BodyPartTabRow
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend.ExerciseTrendEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend.ExerciseTrendIntent
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend.ExerciseTrendState
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend.ExerciseTrendViewModel

internal data class ExerciseTrendEvents(val goBack: () -> Unit)

internal data class ExerciseTrendUiEvents(
    val onBackPressed: () -> Unit,
    val onSelectBodyPart: (BodyPart) -> Unit,
    val onSelectExercise: (Long) -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun ExerciseTrendView(events: ExerciseTrendEvents, viewModel: ExerciseTrendViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)

    ExerciseTrendViewImpl(state = state, uiEvents = uiEvents)

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is ExerciseTrendEffect.GoBack -> events.goBack()
        }
    }
}

@Composable
private fun rememberUiEvents(events: ExerciseTrendEvents, viewModel: ExerciseTrendViewModel): ExerciseTrendUiEvents =
    remember(viewModel) {
        ExerciseTrendUiEvents(
            onBackPressed = events.goBack,
            onSelectBodyPart = { viewModel.handleIntent(ExerciseTrendIntent.SelectBodyPart(it)) },
            onSelectExercise = { viewModel.handleIntent(ExerciseTrendIntent.SelectExercise(it)) },
            onClickRetry = { viewModel.handleIntent(ExerciseTrendIntent.ClickRetry) },
        )
    }

@Composable
internal fun ExerciseTrendViewImpl(state: ExerciseTrendState, uiEvents: ExerciseTrendUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = "종목 추이", onBack = uiEvents.onBackPressed)
        BodyPartTabRow(
            selected = state.selectedBodyPart,
            onSelect = uiEvents.onSelectBodyPart,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.errorMessage != null -> ErrorContent(state.errorMessage, uiEvents.onClickRetry)
                state.isLoading && state.exercises.isEmpty() -> LoadingContent()
                state.selectedBodyPart == null -> CenterText("부위를 먼저 선택하세요")
                state.selectableExercises.isEmpty() -> CenterText("이 부위에는 최근 기록이 없습니다")
                else -> TrendContent(state, uiEvents)
            }
        }
    }
}

@Composable
private fun TrendContent(state: ExerciseTrendState, uiEvents: ExerciseTrendUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        LazyRow(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(items = state.selectableExercises, key = { it.id }) { exercise ->
                ItemChip(
                    text = exercise.name,
                    selected = exercise.id == state.selectedExerciseId,
                    onClick = { uiEvents.onSelectExercise(exercise.id) },
                )
            }
        }

        val trend = state.trend
        when {
            state.selectedExerciseId == null -> CenterText("종목을 선택하세요")

            // 고른 종목의 값이 아직 오지 않았으면 이전 차트를 그대로 둔다.
            trend != null -> ExerciseTrendCard(
                trend = trend,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        BaseText(text = text, color = TextTertiary)
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

private val previewUiEvents = ExerciseTrendUiEvents(
    onBackPressed = {},
    onSelectBodyPart = {},
    onSelectExercise = {},
    onClickRetry = {},
)

private val previewExercises = listOf(
    RecordedExercise(1L, "인클라인 벤치프레스 머신", BodyPart.CHEST, IntensityType.WEIGHT),
    RecordedExercise(2L, "체스트 플라이", BodyPart.CHEST, IntensityType.WEIGHT),
)

private val previewTrend = ExerciseTrend(
    exercise = previewExercises.first(),
    points = listOf(60 to 1200, 65 to 1400, 65 to 1310, 70 to 1580).mapIndexed { index, (max, volume) ->
        ExerciseTrendPoint(
            weekStart = LocalDate.of(2026, 8, 17).plusWeeks(index.toLong()),
            values = mapOf(
                ExerciseTrendMetric.MAX_WEIGHT to max,
                ExerciseTrendMetric.TOTAL_VOLUME to volume,
            ),
        )
    },
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ExerciseTrendViewImplPreview() {
    BodyPlanTheme {
        ExerciseTrendViewImpl(
            state = ExerciseTrendState(
                selectedBodyPart = BodyPart.CHEST,
                exercises = previewExercises,
                selectedExerciseId = 1L,
                trend = previewTrend,
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ExerciseTrendViewImplUnselectedPreview() {
    BodyPlanTheme {
        ExerciseTrendViewImpl(state = ExerciseTrendState(exercises = previewExercises), uiEvents = previewUiEvents)
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ExerciseTrendViewImplNoExercisePreview() {
    BodyPlanTheme {
        ExerciseTrendViewImpl(
            state = ExerciseTrendState(selectedBodyPart = BodyPart.BACK, exercises = previewExercises),
            uiEvents = previewUiEvents,
        )
    }
}
