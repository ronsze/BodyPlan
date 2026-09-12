package kr.sdbk.bodyplan.feature.workoutlog.impl.routinedetail.composable

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.Badge
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.ui.components.WorkoutEntryCard
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.routinedetail.RoutineDetailEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.routinedetail.RoutineDetailIntent
import kr.sdbk.bodyplan.feature.workoutlog.impl.routinedetail.RoutineDetailState
import kr.sdbk.bodyplan.feature.workoutlog.impl.routinedetail.RoutineDetailViewModel

internal data class RoutineDetailEvents(val goBack: () -> Unit, val goToEntryEdit: (Long, Long?) -> Unit)

internal data class RoutineDetailUiEvents(
    val onBackPressed: () -> Unit,
    val onClickAddEntry: () -> Unit,
    val onClickEntry: (Long) -> Unit,
    val onClickDeleteEntry: (Long) -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun RoutineDetailView(events: RoutineDetailEvents, viewModel: RoutineDetailViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)
    val context = LocalContext.current

    RoutineDetailViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is RoutineDetailEffect.NavigateToEntryEdit -> events.goToEntryEdit(effect.routineId, effect.entryId)

            is RoutineDetailEffect.GoBack -> events.goBack()

            is RoutineDetailEffect.ShowMessage ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun rememberUiEvents(events: RoutineDetailEvents, viewModel: RoutineDetailViewModel): RoutineDetailUiEvents =
    remember {
        RoutineDetailUiEvents(
            onBackPressed = events.goBack,
            onClickAddEntry = { viewModel.handleIntent(RoutineDetailIntent.ClickAddEntry) },
            onClickEntry = { viewModel.handleIntent(RoutineDetailIntent.ClickEntry(it)) },
            onClickDeleteEntry = { viewModel.handleIntent(RoutineDetailIntent.ClickDeleteEntry(it)) },
            onClickRetry = { viewModel.handleIntent(RoutineDetailIntent.ClickRetry) },
        )
    }

@Composable
internal fun RoutineDetailViewImpl(state: RoutineDetailState, uiEvents: RoutineDetailUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(
            title = state.routine?.name.orEmpty(),
            onBack = uiEvents.onBackPressed,
            actionText = if (state.routine != null) "운동 추가" else null,
            onClickAction = uiEvents.onClickAddEntry,
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.errorMessage != null -> ErrorContent(state.errorMessage, uiEvents.onClickRetry)
                state.routine == null -> LoadingContent()
                else -> EntryList(state.routine, uiEvents)
            }
        }
    }
}

@Composable
private fun EntryList(routine: Routine, uiEvents: RoutineDetailUiEvents) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Badge(text = routine.bodyPart.label) }
        if (routine.entries.isEmpty()) {
            item { EmptyEntriesText() }
        }
        items(items = routine.entries, key = { it.id }) { entry ->
            WorkoutEntryCard(
                entry = entry,
                isEditable = true,
                onClick = { uiEvents.onClickEntry(entry.id) },
                onClickDelete = { uiEvents.onClickDeleteEntry(entry.id) },
            )
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
private fun EmptyEntriesText() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        BaseText(
            text = "종목이 없습니다",
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

private val previewRoutine = Routine(
    id = 1L,
    name = "가슴 A",
    bodyPart = BodyPart.CHEST,
    entries = listOf(
        WorkoutEntry(
            id = 1L,
            exerciseId = 1L,
            exerciseName = "인클라인 벤치프레스 머신",
            bodyPart = BodyPart.CHEST,
            intensityType = IntensityType.WEIGHT,
            sets = listOf(
                WorkoutSet(repeatCount = 4, intensity = Intensity.Weight(10)),
                WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(15)),
            ),
        ),
        WorkoutEntry(
            id = 2L,
            exerciseId = 3L,
            exerciseName = "푸쉬업",
            bodyPart = BodyPart.CHEST,
            intensityType = IntensityType.ANGLE,
            sets = listOf(WorkoutSet(repeatCount = 12, intensity = Intensity.Angle(0))),
        ),
    ),
)

private val previewUiEvents = RoutineDetailUiEvents(
    onBackPressed = {},
    onClickAddEntry = {},
    onClickEntry = {},
    onClickDeleteEntry = {},
    onClickRetry = {},
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun RoutineDetailViewImplPreview() {
    BodyPlanTheme {
        RoutineDetailViewImpl(
            state = RoutineDetailState(routineId = 1L, routine = previewRoutine),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun RoutineDetailViewImplEmptyPreview() {
    BodyPlanTheme {
        RoutineDetailViewImpl(
            state = RoutineDetailState(routineId = 1L, routine = previewRoutine.copy(entries = emptyList())),
            uiEvents = previewUiEvents,
        )
    }
}
