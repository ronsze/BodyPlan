package kr.sdbk.bodyplan.feature.dietlog.impl.analysis.composable

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
import kr.sdbk.bodyplan.feature.dietlog.impl.analysis.DietAnalysisEffect
import kr.sdbk.bodyplan.feature.dietlog.impl.analysis.DietAnalysisIntent
import kr.sdbk.bodyplan.feature.dietlog.impl.analysis.DietAnalysisState
import kr.sdbk.bodyplan.feature.dietlog.impl.analysis.DietAnalysisViewModel

internal data class DietAnalysisEvents(val goBack: () -> Unit, val goToAiToken: () -> Unit)

internal data class DietAnalysisUiEvents(
    val onBackPressed: () -> Unit,
    val onClickAnalyze: () -> Unit,
    val onConfirmTokenDialog: () -> Unit,
    val onDismissTokenDialog: () -> Unit,
)

@Composable
internal fun DietAnalysisView(events: DietAnalysisEvents, viewModel: DietAnalysisViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)

    DietAnalysisViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is DietAnalysisEffect.GoBack -> events.goBack()
            is DietAnalysisEffect.NavigateToAiToken -> events.goToAiToken()
        }
    }
}

@Composable
private fun rememberUiEvents(events: DietAnalysisEvents, viewModel: DietAnalysisViewModel): DietAnalysisUiEvents =
    remember {
        DietAnalysisUiEvents(
            onBackPressed = events.goBack,
            onClickAnalyze = { viewModel.handleIntent(DietAnalysisIntent.ClickAnalyze) },
            onConfirmTokenDialog = { viewModel.handleIntent(DietAnalysisIntent.ConfirmTokenDialog) },
            onDismissTokenDialog = { viewModel.handleIntent(DietAnalysisIntent.DismissTokenDialog) },
        )
    }

@Composable
internal fun DietAnalysisViewImpl(state: DietAnalysisState, uiEvents: DietAnalysisUiEvents) {
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

private val previewUiEvents = DietAnalysisUiEvents(
    onBackPressed = {},
    onClickAnalyze = {},
    onConfirmTokenDialog = {},
    onDismissTokenDialog = {},
)

private val previewResult = AnalysisResult(
    id = 1L,
    kind = AnalysisKind.DIET_DAILY,
    scopeKey = "20700",
    content = AnalysisContent(
        summary = "닭가슴살 위주로 단백질은 충분했지만 탄수화물이 목표보다 적었어요.",
        sections = listOf(
            AnalysisSection("먹은 음식", "닭가슴살 샐러드, 고구마 1개, 아메리카노"),
            AnalysisSection("칼로리", "약 1,450kcal로 추정됩니다. 목표 1,800kcal보다 350kcal 적습니다."),
            AnalysisSection("영양 성분", "탄수화물 120g, 단백질 110g, 지방 40g으로 추정됩니다."),
            AnalysisSection("개선 방향", "저녁에 현미밥 반 공기를 더해 탄수화물을 채우세요."),
        ),
    ),
    createdAtMillis = 1_757_300_000_000L,
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun DietAnalysisViewImplPreview() {
    BodyPlanTheme {
        DietAnalysisViewImpl(
            state = DietAnalysisState(
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
private fun DietAnalysisViewImplEmptyPreview() {
    BodyPlanTheme {
        DietAnalysisViewImpl(
            state = DietAnalysisState(
                periodLabel = "2026년 9월",
                hasCredential = true,
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun DietAnalysisViewImplErrorPreview() {
    BodyPlanTheme {
        DietAnalysisViewImpl(
            state = DietAnalysisState(
                periodLabel = "2026년 9월 7일 ~ 9월 13일",
                hasCredential = true,
                errorMessage = "먼저 날짜별로 분석해 주세요",
            ),
            uiEvents = previewUiEvents,
        )
    }
}
