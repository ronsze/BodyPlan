package kr.sdbk.bodyplan.feature.my.impl.inbody.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.PillChip
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.Danger
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.InbodyPeriod
import kr.sdbk.bodyplan.core.domain.model.InbodyTrend
import kr.sdbk.bodyplan.core.ui.components.AiTokenRequiredDialog
import kr.sdbk.bodyplan.core.ui.components.requestNotificationThen
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.my.impl.inbody.InbodyEffect
import kr.sdbk.bodyplan.feature.my.impl.inbody.InbodyIntent
import kr.sdbk.bodyplan.feature.my.impl.inbody.InbodyState
import kr.sdbk.bodyplan.feature.my.impl.inbody.InbodyViewModel
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.NoticeBox
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.rangeText
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.shortText

internal data class InbodyEvents(
    val goBack: () -> Unit,
    val goToAiToken: () -> Unit,
    val goToAdd: () -> Unit,
    val goToDetail: (Long) -> Unit,
)

internal data class InbodyUiEvents(
    val onBackPressed: () -> Unit,
    val onClickAdd: () -> Unit,
    val onClickDetail: (Long) -> Unit,
    val onSelectPeriod: (InbodyPeriod) -> Unit,
    val onClickAnalyze: (Long) -> Unit,
    val onConfirmTokenDialog: () -> Unit,
    val onDismissTokenDialog: () -> Unit,
)

@Composable
internal fun InbodyView(events: InbodyEvents, viewModel: InbodyViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val uiEvents = remember(viewModel, events) {
        InbodyUiEvents(
            onBackPressed = events.goBack,
            onClickAdd = events.goToAdd,
            onClickDetail = events.goToDetail,
            onSelectPeriod = { viewModel.handleIntent(InbodyIntent.SelectPeriod(it)) },
            onClickAnalyze = { viewModel.handleIntent(InbodyIntent.ClickAnalyze(it)) },
            onConfirmTokenDialog = { viewModel.handleIntent(InbodyIntent.ConfirmTokenDialog) },
            onDismissTokenDialog = { viewModel.handleIntent(InbodyIntent.DismissTokenDialog) },
        )
    }

    InbodyViewImpl(state = state, uiEvents = uiEvents)

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is InbodyEffect.NavigateToAiToken -> events.goToAiToken()
        }
    }
}

@Composable
internal fun InbodyViewImpl(state: InbodyState, uiEvents: InbodyUiEvents) {
    // 알림 권한을 묻고 나서 눌렀던 기록의 분석을 요청한다. 거절해도 분석은 그대로 돈다.
    var pendingRecordId by remember { mutableStateOf<Long?>(null) }
    val analyzeAfterPermission = requestNotificationThen { pendingRecordId?.let(uiEvents.onClickAnalyze) }

    // 최근 측정 판정은 기록이 바뀔 때만 다시 한다. 카드마다 읽을 때 전체를 다시 정렬하지 않게 한다.
    val latestMeasuredId = remember(state.entries) { state.latestMeasuredId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(
            title = "인바디 분석",
            onBack = uiEvents.onBackPressed,
            actionText = "사진 추가",
            onClickAction = uiEvents.onClickAdd,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PeriodChips(selected = state.period, onSelect = uiEvents.onSelectPeriod)

            val trend = state.trend
            when {
                trend == null && state.errorMessage != null -> Unit

                trend == null || (state.isLoading && state.entries.isEmpty()) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { CircularProgressIndicator() }

                else -> TrendSection(trend)
            }

            PrimaryButton(text = "+ 인바디 사진 추가", onClick = uiEvents.onClickAdd)

            RecordSection(
                state = state,
                latestMeasuredId = latestMeasuredId,
                onClickDetail = uiEvents.onClickDetail,
                onClickAnalyze = { recordId ->
                    pendingRecordId = recordId
                    analyzeAfterPermission()
                },
            )

            state.errorMessage?.let { message ->
                BaseText(text = message, style = MaterialTheme.typography.bodyMedium, color = Danger)
            }
        }
    }

    if (state.isTokenDialogVisible) {
        AiTokenRequiredDialog(
            onConfirm = uiEvents.onConfirmTokenDialog,
            onDismiss = uiEvents.onDismissTokenDialog,
        )
    }
}

@Composable
private fun PeriodChips(selected: InbodyPeriod, onSelect: (InbodyPeriod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InbodyPeriod.entries.forEach { period ->
            PillChip(text = period.label, selected = period == selected, onClick = { onSelect(period) })
        }
    }
}

@Composable
private fun TrendSection(trend: InbodyTrend) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BaseText(
                text = rangeText(trend.rangeStart, trend.rangeEnd),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
            WeightSpacer()
            trend.latestMeasuredOn?.let { latest ->
                BaseText(
                    text = "최근 측정 ${latest.shortText()} · 총 ${trend.measuredCount}회",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
        }

        when {
            !trend.hasRecords -> NoticeBox(
                title = "아직 기록이 없어요",
                body = "인바디 사진을 올려 분석해 보세요. 기록이 쌓이면 변화가 그려져요.",
            )

            trend.cards.isEmpty() -> NoticeBox(
                title = "이 기간에 측정이 없어요",
                body = "더 긴 기간을 고르거나 사진을 분석해 보세요.",
            )

            else -> trend.cards.forEach { card -> MetricTrendCard(card) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BaseText(
                text = "점은 기록 순서대로 놓이고, 지표별 세로축 범위는 달라요.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
            if (trend.pendingCount > 0) {
                BaseText(
                    text = "분석 결과가 없는 ${trend.pendingCount}장은 그래프에 포함되지 않아요.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun RecordSection(
    state: InbodyState,
    latestMeasuredId: Long?,
    onClickDetail: (Long) -> Unit,
    onClickAnalyze: (Long) -> Unit,
) {
    if (state.entries.isEmpty()) return

    VerticalSpacer(space = 4.dp)
    Row(verticalAlignment = Alignment.CenterVertically) {
        BaseText(text = "사진 기록", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        WeightSpacer()
        BaseText(
            text = "총 ${state.entries.size}장",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.entries.forEach { entry ->
            InbodyRecordCard(
                entry = entry,
                isLatest = entry.record.id == latestMeasuredId,
                onClickDetail = { onClickDetail(entry.record.id) },
                onClickAnalyze = { onClickAnalyze(entry.record.id) },
            )
        }
    }
}

private val InbodyPeriod.label: String
    get() = when (this) {
        InbodyPeriod.ONE_MONTH -> "1개월"
        InbodyPeriod.THREE_MONTHS -> "3개월"
        InbodyPeriod.SIX_MONTHS -> "6개월"
        InbodyPeriod.ALL -> "전체"
    }
