package kr.sdbk.bodyplan.feature.dietlog.impl.analysis.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Danger
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.ui.components.AiTokenRequiredDialog
import kr.sdbk.bodyplan.core.ui.components.AnalysisResultContent
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = "식단 분석", onBack = uiEvents.onBackPressed)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BaseText(
                text = state.periodLabel,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )

            when {
                state.isLoading && state.result == null -> LoadingContent()
                state.result != null -> AnalysisResultContent(state.result)
                else -> EmptyContent()
            }

            state.errorMessage?.let { message ->
                BaseText(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Danger,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            PrimaryButton(
                text = analyzeButtonText(state),
                onClick = uiEvents.onClickAnalyze,
                enabled = state.canAnalyze,
            )
        }
    }

    if (state.isTokenDialogVisible) {
        AiTokenRequiredDialog(
            onConfirm = uiEvents.onConfirmTokenDialog,
            onDismiss = uiEvents.onDismissTokenDialog,
        )
    }
}

private fun analyzeButtonText(state: DietAnalysisState): String = when {
    state.isAnalyzing -> "분석 중"
    state.result != null -> "새로 분석하기"
    else -> "분석하기"
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        BaseText(
            text = "아직 분석하지 않았어요",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
        )
    }
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
                kind = AnalysisKind.DIET_DAILY,
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
                kind = AnalysisKind.DIET_MONTHLY,
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
                kind = AnalysisKind.DIET_WEEKLY,
                hasCredential = true,
                errorMessage = "먼저 날짜별로 분석해 주세요",
            ),
            uiEvents = previewUiEvents,
        )
    }
}
