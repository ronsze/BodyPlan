package kr.sdbk.bodyplan.feature.dietlog.impl.log.composable

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kr.sdbk.bodyplan.core.designsystem.component.BaseImage
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.DietEntry
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.dietlog.api.DietAnalysisPeriod
import kr.sdbk.bodyplan.feature.dietlog.impl.log.DietLogEffect
import kr.sdbk.bodyplan.feature.dietlog.impl.log.DietLogIntent
import kr.sdbk.bodyplan.feature.dietlog.impl.log.DietLogState
import kr.sdbk.bodyplan.feature.dietlog.impl.log.DietLogViewModel
import kr.sdbk.bodyplan.feature.dietlog.impl.log.mealTitle

internal data class DietLogEvents(
    val goBack: () -> Unit,
    val goToEntryEdit: (LocalDate, Long?) -> Unit,
    val goToAnalysis: (DietAnalysisPeriod, LocalDate) -> Unit,
)

internal data class DietLogUiEvents(
    val onBackPressed: () -> Unit,
    val onClickAddEntry: () -> Unit,
    val onClickEntry: (Long) -> Unit,
    val onLongClickEntry: (Long) -> Unit,
    val onClickDeleteEntry: (Long) -> Unit,
    val onClickAnalyze: () -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun DietLogView(events: DietLogEvents, viewModel: DietLogViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel, state.date)
    val context = LocalContext.current

    DietLogViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is DietLogEffect.NavigateToEntryEdit -> events.goToEntryEdit(effect.date, effect.entryId)

            is DietLogEffect.GoBack -> events.goBack()

            is DietLogEffect.ShowMessage ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun rememberUiEvents(events: DietLogEvents, viewModel: DietLogViewModel, date: LocalDate): DietLogUiEvents {
    val context = LocalContext.current
    // 안드로이드 9만 공용 저장소 쓰기 권한이 필요하다. 10부터는 MediaStore가 권한 없이 쓴다.
    val pendingExportId = remember { mutableStateOf<Long?>(null) }
    val requestWritePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val id = pendingExportId.value
        pendingExportId.value = null
        when {
            id == null -> Unit
            granted -> viewModel.handleIntent(DietLogIntent.LongClickEntry(id))
            else -> Toast.makeText(context, PERMISSION_REQUIRED, Toast.LENGTH_SHORT).show()
        }
    }

    return remember(viewModel, requestWritePermission, date) {
        DietLogUiEvents(
            onBackPressed = events.goBack,
            onClickAddEntry = { viewModel.handleIntent(DietLogIntent.ClickAddEntry) },
            onClickEntry = { viewModel.handleIntent(DietLogIntent.ClickEntry(it)) },
            onLongClickEntry = { id ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    viewModel.handleIntent(DietLogIntent.LongClickEntry(id))
                } else {
                    pendingExportId.value = id
                    requestWritePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            },
            onClickDeleteEntry = { viewModel.handleIntent(DietLogIntent.ClickDeleteEntry(it)) },
            onClickAnalyze = { events.goToAnalysis(DietAnalysisPeriod.DAILY, date) },
            onClickRetry = { viewModel.handleIntent(DietLogIntent.ClickRetry) },
        )
    }
}

@Composable
internal fun DietLogViewImpl(state: DietLogState, uiEvents: DietLogUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(
            title = state.date.format(DATE_FORMAT),
            onBack = uiEvents.onBackPressed,
            actionText = if (state.isEditable) "식단 추가" else null,
            onClickAction = uiEvents.onClickAddEntry,
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.errorMessage != null -> ErrorContent(state.errorMessage, uiEvents.onClickRetry)
                state.isLoading && state.entries.isEmpty() -> LoadingContent()
                state.entries.isEmpty() -> EmptyContent()
                else -> EntryList(state, uiEvents)
            }
        }

        // 조회만 되는 날짜에서도 분석은 할 수 있다. 지난 기록을 돌아보는 것이 분석의 쓸모다.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            OutlinedActionButton(text = "식단 분석", onClick = uiEvents.onClickAnalyze)
        }
    }
}

@Composable
private fun EntryList(state: DietLogState, uiEvents: DietLogUiEvents) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(items = state.entries, key = { _, entry -> entry.id }) { index, entry ->
            EntryCard(
                title = mealTitle(index + 1),
                entry = entry,
                isEditable = state.isEditable,
                onClick = { uiEvents.onClickEntry(entry.id) },
                onLongClick = { uiEvents.onLongClickEntry(entry.id) },
                onClickDelete = { uiEvents.onClickDeleteEntry(entry.id) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryCard(
    title: String,
    entry: DietEntry,
    isEditable: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickDelete: () -> Unit,
) {
    BodyPlanCard(contentPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BaseText(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            WeightSpacer()
            if (isEditable) {
                BaseText(
                    text = "삭제",
                    modifier = Modifier.clickable(onClick = onClickDelete),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
        }
        VerticalSpacer(space = 16.dp)
        HorizontalDivider(color = Border)
        VerticalSpacer(space = 16.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BaseImage(
                file = File(entry.imagePath),
                contentDescription = title,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    // 사진을 길게 누르면 갤러리로 내보낸다. 조회만 되는 날짜에서도 저장은 할 수 있다.
                    .combinedClickable(
                        onClick = { if (isEditable) onClick() },
                        onLongClick = onLongClick,
                    ),
                placeholder = ColorPainter(Color.LightGray),
            )
            val memo = entry.memo
            if (!memo.isNullOrBlank()) {
                BaseText(
                    text = memo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BaseText(
            text = "기록이 없습니다",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
        )
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

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
private const val PERMISSION_REQUIRED = "저장하려면 권한이 필요합니다"

private val previewEntries = listOf(
    DietEntry(id = 1L, imagePath = "/files/diet_images/a.jpg", memo = "닭가슴살 샐러드와 고구마"),
    DietEntry(id = 2L, imagePath = "/files/diet_images/b.jpg", memo = null),
)

private val previewUiEvents = DietLogUiEvents(
    onBackPressed = {},
    onClickAnalyze = {},
    onClickAddEntry = {},
    onClickEntry = {},
    onLongClickEntry = {},
    onClickDeleteEntry = {},
    onClickRetry = {},
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun DietLogViewImplPreview() {
    BodyPlanTheme {
        DietLogViewImpl(
            state = DietLogState(
                date = LocalDate.of(2026, 9, 8),
                isEditable = true,
                entries = previewEntries,
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun DietLogViewImplEmptyPreview() {
    BodyPlanTheme {
        DietLogViewImpl(
            state = DietLogState(date = LocalDate.of(2026, 9, 8), isEditable = true),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun DietLogViewImplReadOnlyPreview() {
    BodyPlanTheme {
        DietLogViewImpl(
            state = DietLogState(
                date = LocalDate.of(2026, 8, 30),
                isEditable = false,
                entries = previewEntries,
            ),
            uiEvents = previewUiEvents,
        )
    }
}
