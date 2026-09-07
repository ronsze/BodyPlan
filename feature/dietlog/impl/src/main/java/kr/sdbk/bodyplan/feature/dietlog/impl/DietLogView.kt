package kr.sdbk.bodyplan.feature.dietlog.impl

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.domain.model.DietEntry
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect

internal data class DietLogEvents(val goBack: () -> Unit, val goToEntryEdit: (LocalDate, Long?) -> Unit)

internal data class DietLogUiEvents(
    val onBackPressed: () -> Unit,
    val onClickAddEntry: () -> Unit,
    val onClickEntry: (Long) -> Unit,
    val onLongClickEntry: (Long) -> Unit,
    val onClickDeleteEntry: (Long) -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun DietLogView(events: DietLogEvents, viewModel: DietLogViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)
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
private fun rememberUiEvents(events: DietLogEvents, viewModel: DietLogViewModel): DietLogUiEvents {
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

    return remember(viewModel, requestWritePermission) {
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
            onClickRetry = { viewModel.handleIntent(DietLogIntent.ClickRetry) },
        )
    }
}

@Composable
internal fun DietLogViewImpl(state: DietLogState, uiEvents: DietLogUiEvents) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = uiEvents.onBackPressed) { BaseText(text = "뒤로") }
            BaseText(
                text = state.date.format(DATE_FORMAT),
                style = MaterialTheme.typography.titleMedium,
            )
            WeightSpacer()
            if (state.isEditable) {
                TextButton(onClick = uiEvents.onClickAddEntry) { BaseText(text = "식단 추가") }
            }
        }
        VerticalSpacer(space = 8.dp)

        when {
            state.errorMessage != null -> ErrorContent(state.errorMessage, uiEvents.onClickRetry)
            state.isLoading && state.entries.isEmpty() -> LoadingContent()
            state.entries.isEmpty() -> EmptyContent()
            else -> EntryList(state, uiEvents)
        }
    }
}

@Composable
private fun EntryList(state: DietLogState, uiEvents: DietLogUiEvents) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun EntryCard(
    title: String,
    entry: DietEntry,
    isEditable: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            BaseImage(
                file = File(entry.imagePath),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    // 길게 누르면 갤러리로 내보낸다. 조회만 되는 날짜에서도 저장은 할 수 있다.
                    .combinedClickable(
                        onClick = { if (isEditable) onClick() },
                        onLongClick = onLongClick,
                    ),
                placeholder = ColorPainter(Color.LightGray),
            )
            Row(
                modifier = Modifier.padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val memo = entry.memo
                Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                    BaseText(text = title, style = MaterialTheme.typography.titleSmall)
                    if (!memo.isNullOrBlank()) {
                        BaseText(text = memo, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (isEditable) {
                    TextButton(onClick = onClickDelete) { BaseText(text = "삭제") }
                }
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
        BaseText(text = "기록이 없습니다")
    }
}

@Composable
private fun ErrorContent(message: String, onClickRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BaseText(text = message)
            TextButton(onClick = onClickRetry) { BaseText(text = "다시 시도") }
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
private const val PERMISSION_REQUIRED = "저장하려면 권한이 필요합니다"

@Preview(showBackground = true)
@Composable
private fun DietLogViewImplPreview() {
    BodyPlanTheme {
        DietLogViewImpl(
            state = DietLogState(
                date = LocalDate.of(2026, 9, 7),
                isEditable = true,
                entries = listOf(
                    DietEntry(id = 1L, imagePath = "/files/diet_images/a.jpg", memo = "닭가슴살과 고구마"),
                    DietEntry(id = 2L, imagePath = "/files/diet_images/b.jpg", memo = null),
                ),
            ),
            uiEvents = DietLogUiEvents(
                onBackPressed = {},
                onClickAddEntry = {},
                onClickEntry = {},
                onLongClickEntry = {},
                onClickDeleteEntry = {},
                onClickRetry = {},
            ),
        )
    }
}
