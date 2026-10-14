package kr.sdbk.bodyplan.feature.my.impl.inbodydetail.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.Danger
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.InbodyDetail
import kr.sdbk.bodyplan.core.domain.model.InbodyEntryStatus
import kr.sdbk.bodyplan.core.ui.components.AiTokenRequiredDialog
import kr.sdbk.bodyplan.core.ui.components.AnalysisResultContent
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.core.ui.components.requestNotificationThen
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.NoticeBox
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.fullText
import kr.sdbk.bodyplan.feature.my.impl.inbodydetail.InbodyDetailEffect
import kr.sdbk.bodyplan.feature.my.impl.inbodydetail.InbodyDetailIntent
import kr.sdbk.bodyplan.feature.my.impl.inbodydetail.InbodyDetailState
import kr.sdbk.bodyplan.feature.my.impl.inbodydetail.InbodyDetailViewModel

internal data class InbodyDetailEvents(val goBack: () -> Unit, val goToAiToken: () -> Unit)

internal data class InbodyDetailUiEvents(
    val onBackPressed: () -> Unit,
    val onClickAnalyze: () -> Unit,
    val onClickImage: () -> Unit,
    val onDismissImageViewer: () -> Unit,
    val onConfirmTokenDialog: () -> Unit,
    val onDismissTokenDialog: () -> Unit,
)

@Composable
internal fun InbodyDetailView(events: InbodyDetailEvents, viewModel: InbodyDetailViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val uiEvents = remember(viewModel, events) {
        InbodyDetailUiEvents(
            onBackPressed = events.goBack,
            onClickAnalyze = { viewModel.handleIntent(InbodyDetailIntent.ClickAnalyze) },
            onClickImage = { viewModel.handleIntent(InbodyDetailIntent.ClickImage) },
            onDismissImageViewer = { viewModel.handleIntent(InbodyDetailIntent.DismissImageViewer) },
            onConfirmTokenDialog = { viewModel.handleIntent(InbodyDetailIntent.ConfirmTokenDialog) },
            onDismissTokenDialog = { viewModel.handleIntent(InbodyDetailIntent.DismissTokenDialog) },
        )
    }

    InbodyDetailViewImpl(state = state, uiEvents = uiEvents)

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is InbodyDetailEffect.NavigateToAiToken -> events.goToAiToken()
        }
    }
}

@Composable
internal fun InbodyDetailViewImpl(state: InbodyDetailState, uiEvents: InbodyDetailUiEvents) {
    // 알림 권한을 묻고 나서 분석을 요청한다. 거절해도 분석은 그대로 돈다.
    val analyze = requestNotificationThen(uiEvents.onClickAnalyze)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = "기록 상세", onBack = uiEvents.onBackPressed)

        val detail = state.detail
        when {
            detail != null -> Content(detail, state, uiEvents, analyze)

            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            else -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BaseText(
                    text = "기록을 찾을 수 없어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary,
                )
            }
        }
    }

    if (state.isImageViewerVisible) {
        state.detail?.let { ImageViewerDialog(it.entry.record.imagePath, uiEvents.onDismissImageViewer) }
    }
    if (state.isTokenDialogVisible) {
        AiTokenRequiredDialog(
            onConfirm = uiEvents.onConfirmTokenDialog,
            onDismiss = uiEvents.onDismissTokenDialog,
        )
    }
}

@Composable
private fun Content(
    detail: InbodyDetail,
    state: InbodyDetailState,
    uiEvents: InbodyDetailUiEvents,
    onClickAnalyze: () -> Unit,
) {
    val entry = detail.entry
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BaseText(
            text = entry.record.measuredOn.fullText(),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )
        ResultImageCard(imagePath = entry.record.imagePath, onClickEnlarge = uiEvents.onClickImage)

        BaseText(text = "상세 측정 값", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        InbodyMeasurementCard(detail)

        BaseText(text = "사진 분석", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        when (entry.status) {
            InbodyEntryStatus.RUNNING -> NoticeBox(
                title = entry.run?.stage?.label ?: "분석을 시작하는 중",
                body = "진행 중에는 다시 요청할 수 없어요.",
            )

            InbodyEntryStatus.FAILED -> BaseText(
                text = entry.record.failureReason.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = Danger,
            )

            else -> Unit
        }
        val result = entry.result
        if (result != null) {
            AnalysisResultContent(result, modifier = Modifier.fillMaxWidth())
        } else if (entry.status != InbodyEntryStatus.RUNNING) {
            NoticeBox(
                title = "아직 분석하지 않았어요",
                body = "아래 버튼으로 분석을 요청하면 측정값과 분석 내용이 채워져요.",
            )
        }

        PrimaryButton(
            text = if (entry.status == InbodyEntryStatus.RUNNING) "분석 중" else "새로 분석하기",
            onClick = onClickAnalyze,
            enabled = state.canAnalyze,
        )
    }
}
