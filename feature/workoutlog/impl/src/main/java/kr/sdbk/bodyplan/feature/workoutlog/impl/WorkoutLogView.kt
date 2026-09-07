package kr.sdbk.bodyplan.feature.workoutlog.impl

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.ui.components.color
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect

internal data class WorkoutLogEvents(val goBack: () -> Unit, val goToEntryEdit: (LocalDate, Long?) -> Unit)

internal data class WorkoutLogUiEvents(
    val onBackPressed: () -> Unit,
    val onClickAddEntry: () -> Unit,
    val onClickEntry: (Long) -> Unit,
    val onClickDeleteEntry: (Long) -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun WorkoutLogView(events: WorkoutLogEvents, viewModel: WorkoutLogViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)
    val context = LocalContext.current

    WorkoutLogViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is WorkoutLogEffect.NavigateToEntryEdit -> events.goToEntryEdit(effect.date, effect.entryId)

            is WorkoutLogEffect.GoBack -> events.goBack()

            is WorkoutLogEffect.ShowMessage ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun rememberUiEvents(events: WorkoutLogEvents, viewModel: WorkoutLogViewModel): WorkoutLogUiEvents = remember {
    WorkoutLogUiEvents(
        onBackPressed = events.goBack,
        onClickAddEntry = { viewModel.handleIntent(WorkoutLogIntent.ClickAddEntry) },
        onClickEntry = { viewModel.handleIntent(WorkoutLogIntent.ClickEntry(it)) },
        onClickDeleteEntry = { viewModel.handleIntent(WorkoutLogIntent.ClickDeleteEntry(it)) },
        onClickRetry = { viewModel.handleIntent(WorkoutLogIntent.ClickRetry) },
    )
}

@Composable
internal fun WorkoutLogViewImpl(state: WorkoutLogState, uiEvents: WorkoutLogUiEvents) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = uiEvents.onBackPressed) { BaseText(text = "뒤로") }
            BaseText(
                text = state.date.format(DATE_FORMAT),
                style = MaterialTheme.typography.titleMedium,
            )
            WeightSpacer()
            if (state.isEditable) {
                TextButton(onClick = uiEvents.onClickAddEntry) { BaseText(text = "운동 추가") }
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
private fun EntryList(state: WorkoutLogState, uiEvents: WorkoutLogUiEvents) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items = state.entries, key = { it.id }) { entry ->
            EntryCard(
                entry = entry,
                isEditable = state.isEditable,
                onClick = { uiEvents.onClickEntry(entry.id) },
                onClickDelete = { uiEvents.onClickDeleteEntry(entry.id) },
            )
        }
    }
}

@Composable
private fun EntryCard(entry: WorkoutEntry, isEditable: Boolean, onClick: () -> Unit, onClickDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isEditable) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BaseText(
                    text = entry.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                )
                WeightSpacer()
                BaseText(
                    text = entry.bodyPart.label,
                    color = entry.bodyPart.color,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (isEditable) {
                    TextButton(onClick = onClickDelete) { BaseText(text = "삭제") }
                }
            }
            VerticalSpacer(space = 4.dp)
            entry.sets.forEachIndexed { index, set ->
                BaseText(
                    text = setLine(index + 1, set),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun setLine(setNumber: Int, set: WorkoutSet): String {
    val unit = when (set.intensity) {
        is Intensity.Weight -> "kg"
        is Intensity.Angle -> "도"
    }
    return "${setNumber}세트  ${set.intensity.value}$unit x ${set.repeatCount}회"
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

@Preview(showBackground = true)
@Composable
private fun WorkoutLogViewImplPreview() {
    BodyPlanTheme {
        WorkoutLogViewImpl(
            state = WorkoutLogState(
                date = LocalDate.of(2026, 9, 7),
                isEditable = true,
                entries = listOf(
                    WorkoutEntry(
                        id = 1L,
                        exerciseId = 1L,
                        exerciseName = "플랫 벤치프레스 머신",
                        bodyPart = BodyPart.CHEST,
                        intensityType = IntensityType.WEIGHT,
                        sets = listOf(
                            WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40)),
                            WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(45)),
                        ),
                    ),
                ),
            ),
            uiEvents = WorkoutLogUiEvents(
                onBackPressed = {},
                onClickAddEntry = {},
                onClickEntry = {},
                onClickDeleteEntry = {},
                onClickRetry = {},
            ),
        )
    }
}
