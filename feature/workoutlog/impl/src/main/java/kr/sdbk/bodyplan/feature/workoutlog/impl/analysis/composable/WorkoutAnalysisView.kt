package kr.sdbk.bodyplan.feature.workoutlog.impl.analysis.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.ui.components.AnalysisScreen
import kr.sdbk.bodyplan.core.ui.components.AnalysisScreenActions
import kr.sdbk.bodyplan.core.ui.components.AnalysisScreenState
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.analysis.WorkoutAnalysisEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.analysis.WorkoutAnalysisIntent
import kr.sdbk.bodyplan.feature.workoutlog.impl.analysis.WorkoutAnalysisState
import kr.sdbk.bodyplan.feature.workoutlog.impl.analysis.WorkoutAnalysisViewModel

internal data class WorkoutAnalysisEvents(val goBack: () -> Unit, val goToAiToken: () -> Unit)

internal data class WorkoutAnalysisUiEvents(
    val onBackPressed: () -> Unit,
    val onClickAnalyze: () -> Unit,
    val onConfirmTokenDialog: () -> Unit,
    val onDismissTokenDialog: () -> Unit,
)

@Composable
internal fun WorkoutAnalysisView(events: WorkoutAnalysisEvents, viewModel: WorkoutAnalysisViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)

    WorkoutAnalysisViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is WorkoutAnalysisEffect.GoBack -> events.goBack()
            is WorkoutAnalysisEffect.NavigateToAiToken -> events.goToAiToken()
        }
    }
}

@Composable
private fun rememberUiEvents(
    events: WorkoutAnalysisEvents,
    viewModel: WorkoutAnalysisViewModel,
): WorkoutAnalysisUiEvents = remember {
    WorkoutAnalysisUiEvents(
        onBackPressed = events.goBack,
        onClickAnalyze = { viewModel.handleIntent(WorkoutAnalysisIntent.ClickAnalyze) },
        onConfirmTokenDialog = { viewModel.handleIntent(WorkoutAnalysisIntent.ConfirmTokenDialog) },
        onDismissTokenDialog = { viewModel.handleIntent(WorkoutAnalysisIntent.DismissTokenDialog) },
    )
}

@Composable
internal fun WorkoutAnalysisViewImpl(state: WorkoutAnalysisState, uiEvents: WorkoutAnalysisUiEvents) {
    AnalysisScreen(
        state = AnalysisScreenState(
            title = state.title,
            subtitle = state.periodLabel,
            result = state.result,
            isLoading = state.isLoading,
            isAnalyzing = state.isAnalyzing,
            canAnalyze = state.canAnalyze,
            isTokenDialogVisible = state.isTokenDialogVisible,
            errorMessage = state.message,
            stageMessage = state.run?.stage?.label,
        ),
        actions = AnalysisScreenActions(
            onBack = uiEvents.onBackPressed,
            onClickAnalyze = uiEvents.onClickAnalyze,
            onConfirmTokenDialog = uiEvents.onConfirmTokenDialog,
            onDismissTokenDialog = uiEvents.onDismissTokenDialog,
        ),
    )
}

private val previewUiEvents = WorkoutAnalysisUiEvents(
    onBackPressed = {},
    onClickAnalyze = {},
    onConfirmTokenDialog = {},
    onDismissTokenDialog = {},
)

private val previewResult = AnalysisResult(
    id = 1L,
    kind = AnalysisKind.WORKOUT_DAILY,
    scopeKey = "20700",
    content = AnalysisContent(
        summary = "가슴과 삼두를 함께 자극한 날입니다. 볼륨은 목표에 조금 못 미칩니다.",
        sections = listOf(
            AnalysisSection("수행한 운동", "가슴 · 벤치프레스 3세트, 삼두 · 딥스 3세트"),
            AnalysisSection("볼륨", "무게 종목 총 2,340kg입니다. 근성장 목표에는 조금 부족합니다."),
            AnalysisSection("부위 균형", "가슴·삼두에 몰렸고 등과 하체는 빠졌습니다."),
            AnalysisSection("개선 방향", "다음 날 등 운동을 넣어 밀고 당기는 균형을 맞추세요."),
        ),
    ),
    createdAtMillis = 1_757_300_000_000L,
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WorkoutAnalysisViewImplPreview() {
    BodyPlanTheme {
        WorkoutAnalysisViewImpl(
            state = WorkoutAnalysisState(
                periodLabel = "2026년 9월 8일",
                result = previewResult,
                hasCredential = true,
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WorkoutAnalysisViewImplEmptyPreview() {
    BodyPlanTheme {
        WorkoutAnalysisViewImpl(
            state = WorkoutAnalysisState(periodLabel = "2026년 9월", hasCredential = true),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WorkoutAnalysisViewImplErrorPreview() {
    BodyPlanTheme {
        WorkoutAnalysisViewImpl(
            state = WorkoutAnalysisState(
                periodLabel = "2026년 9월 7일 ~ 9월 13일",
                hasCredential = true,
                errorMessage = "분석할 기록이 없습니다",
            ),
            uiEvents = previewUiEvents,
        )
    }
}
