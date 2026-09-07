package kr.sdbk.bodyplan.feature.my.impl.inbody.composable

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kr.sdbk.bodyplan.core.designsystem.component.BaseImage
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection
import kr.sdbk.bodyplan.core.ui.components.AnalysisScreen
import kr.sdbk.bodyplan.core.ui.components.AnalysisScreenActions
import kr.sdbk.bodyplan.core.ui.components.AnalysisScreenState
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.my.impl.inbody.InbodyEffect
import kr.sdbk.bodyplan.feature.my.impl.inbody.InbodyIntent
import kr.sdbk.bodyplan.feature.my.impl.inbody.InbodyState
import kr.sdbk.bodyplan.feature.my.impl.inbody.InbodyViewModel

internal data class InbodyEvents(val goBack: () -> Unit, val goToAiToken: () -> Unit)

internal data class InbodyUiEvents(
    val onBackPressed: () -> Unit,
    val onClickPickImage: () -> Unit,
    val onClickAnalyze: () -> Unit,
    val onClickHistory: (Long) -> Unit,
    val onConfirmTokenDialog: () -> Unit,
    val onDismissTokenDialog: () -> Unit,
)

@Composable
internal fun InbodyView(events: InbodyEvents, viewModel: InbodyViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 사진 선택기는 저장소 권한을 요구하지 않는다. 취소하면 null이 오고 아무 것도 하지 않는다.
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.handleIntent(InbodyIntent.PickImage(uri.toString()))
    }

    val uiEvents = remember(viewModel, pickImage) {
        InbodyUiEvents(
            onBackPressed = events.goBack,
            onClickPickImage = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onClickAnalyze = { viewModel.handleIntent(InbodyIntent.ClickAnalyze) },
            onClickHistory = { viewModel.handleIntent(InbodyIntent.ClickHistory(it)) },
            onConfirmTokenDialog = { viewModel.handleIntent(InbodyIntent.ConfirmTokenDialog) },
            onDismissTokenDialog = { viewModel.handleIntent(InbodyIntent.DismissTokenDialog) },
        )
    }

    InbodyViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is InbodyEffect.NavigateToAiToken -> events.goToAiToken()
        }
    }
}

@Composable
internal fun InbodyViewImpl(state: InbodyState, uiEvents: InbodyUiEvents) {
    AnalysisScreen(
        state = AnalysisScreenState(
            title = "인바디 분석",
            result = state.result,
            isLoading = state.isLoading,
            isAnalyzing = state.isAnalyzing,
            canAnalyze = state.canAnalyze,
            isTokenDialogVisible = state.isTokenDialogVisible,
            errorMessage = state.message,
            stageMessage = state.run?.stage?.label,
            emptyMessage = "사진을 올리고 분석해 보세요",
        ),
        actions = AnalysisScreenActions(
            onBack = uiEvents.onBackPressed,
            onClickAnalyze = uiEvents.onClickAnalyze,
            onConfirmTokenDialog = uiEvents.onConfirmTokenDialog,
            onDismissTokenDialog = uiEvents.onDismissTokenDialog,
        ),
        beforeResult = {
            PhotoSlot(state.shownImage, uiEvents.onClickPickImage)
            // 사진 자리를 눌러도 되지만, 눌러 보기 전엔 그것이 추가라는 것을 알기 어렵다.
            // 지난 결과를 보고 있을 때만 낸다 — 새 사진을 고른 뒤에는 아래 분석 버튼이 그 자리다.
            if (state.pickedImageUri == null) {
                OutlinedActionButton(text = "새 인바디 추가", onClick = uiEvents.onClickPickImage)
            }
            // 그래프는 오래된 것부터 그린다. 이력 목록은 최신순이라 뒤집어 넘긴다.
            InbodyTrendChart(
                points = state.history.reversed().mapNotNull { result ->
                    result.measurement?.let { InbodyTrendPoint(result.createdAtMillis, it) }
                },
            )
        },
        afterResult = { HistoryList(state, uiEvents.onClickHistory) },
    )
}

/** 어느 쪽이든 누르면 사진 고르기가 열린다. 바꾸려고 다른 버튼을 찾지 않게 한다. */
@Composable
private fun PhotoSlot(imageUri: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PHOTO_HEIGHT)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUri == null) {
            BaseText(
                text = "인바디 사진을 올려 주세요",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
            )
        } else {
            BaseImage(
                url = imageUri,
                contentDescription = "인바디 사진",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun ColumnScope.HistoryList(state: InbodyState, onClickHistory: (Long) -> Unit) {
    if (state.history.isEmpty()) return

    BaseText(text = "지난 분석", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.history.forEach { item ->
            BodyPlanCard(
                modifier = Modifier.clickable { onClickHistory(item.id) },
                contentPadding = 16.dp,
            ) {
                BaseText(
                    text = ANALYZED_AT_FORMAT.format(
                        Instant.ofEpochMilli(item.createdAtMillis).atZone(ZoneId.systemDefault()),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
                VerticalSpacer(space = 4.dp)
                BaseText(
                    text = item.content.summary.lineSequence().first(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}

private val PHOTO_HEIGHT = 220.dp
private val ANALYZED_AT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm")

private val previewUiEvents = InbodyUiEvents(
    onBackPressed = {},
    onClickPickImage = {},
    onClickAnalyze = {},
    onClickHistory = {},
    onConfirmTokenDialog = {},
    onDismissTokenDialog = {},
)

private val previewResult = AnalysisResult(
    id = 1L,
    kind = AnalysisKind.INBODY,
    scopeKey = "",
    content = AnalysisContent(
        summary = "골격근량은 표준 범위지만 체지방률이 목표보다 높습니다.",
        sections = listOf(
            AnalysisSection("측정값", "체중 72.4kg, 골격근량 33.1kg, 체지방률 21.3%"),
            AnalysisSection("체성분 평가", "근육량은 또래 평균 이상이고 체지방이 조금 많습니다."),
            AnalysisSection("식단 개선", "하루 300kcal를 줄이되 단백질은 유지하세요."),
            AnalysisSection("운동 개선", "주 2회 유산소를 더해 체지방을 줄이세요."),
        ),
    ),
    createdAtMillis = 1_757_300_000_000L,
    imagePath = "/files/inbody_images/a.jpg",
)

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun InbodyViewImplPreview() {
    BodyPlanTheme {
        InbodyViewImpl(
            state = InbodyState(history = listOf(previewResult), hasCredential = true),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun InbodyViewImplEmptyPreview() {
    BodyPlanTheme {
        InbodyViewImpl(state = InbodyState(hasCredential = true), uiEvents = previewUiEvents)
    }
}
