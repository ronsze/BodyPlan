package kr.sdbk.bodyplan.feature.workoutlog.impl

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.OptionChipRow
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutOptions
import kr.sdbk.bodyplan.core.ui.components.BodyPartTabRow
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect

internal data class WorkoutEntryEditEvents(val goBack: () -> Unit)

internal data class WorkoutEntryEditUiEvents(
    val onBackPressed: () -> Unit,
    val onSelectBodyPart: (BodyPart) -> Unit,
    val onSelectExercise: (Long) -> Unit,
    val onClickAddSet: () -> Unit,
    val onClickRemoveSet: (Long) -> Unit,
    val onSelectSetRepeatCount: (Long, Int) -> Unit,
    val onSelectSetIntensity: (Long, Int) -> Unit,
    val onClickSave: () -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun WorkoutEntryEditView(events: WorkoutEntryEditEvents, viewModel: WorkoutEntryEditViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)
    val context = LocalContext.current

    WorkoutEntryEditViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is WorkoutEntryEditEffect.GoBack -> events.goBack()

            is WorkoutEntryEditEffect.ShowMessage ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun rememberUiEvents(
    events: WorkoutEntryEditEvents,
    viewModel: WorkoutEntryEditViewModel,
): WorkoutEntryEditUiEvents = remember {
    WorkoutEntryEditUiEvents(
        onBackPressed = events.goBack,
        onSelectBodyPart = { viewModel.handleIntent(WorkoutEntryEditIntent.SelectBodyPart(it)) },
        onSelectExercise = { viewModel.handleIntent(WorkoutEntryEditIntent.SelectExercise(it)) },
        onClickAddSet = { viewModel.handleIntent(WorkoutEntryEditIntent.ClickAddSet) },
        onClickRemoveSet = { viewModel.handleIntent(WorkoutEntryEditIntent.ClickRemoveSet(it)) },
        onSelectSetRepeatCount = { id, value ->
            viewModel.handleIntent(WorkoutEntryEditIntent.SelectSetRepeatCount(id, value))
        },
        onSelectSetIntensity = { id, value ->
            viewModel.handleIntent(WorkoutEntryEditIntent.SelectSetIntensity(id, value))
        },
        onClickSave = { viewModel.handleIntent(WorkoutEntryEditIntent.ClickSave) },
        onClickRetry = { viewModel.handleIntent(WorkoutEntryEditIntent.ClickRetry) },
    )
}

@Composable
internal fun WorkoutEntryEditViewImpl(state: WorkoutEntryEditState, uiEvents: WorkoutEntryEditUiEvents) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = uiEvents.onBackPressed) { BaseText(text = "뒤로") }
            BaseText(
                text = if (state.editingEntryId == null) "운동 추가" else "운동 수정",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        VerticalSpacer(space = 8.dp)

        BodyPartTabRow(
            selected = state.selectedBodyPart,
            onSelect = uiEvents.onSelectBodyPart,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(space = 12.dp)

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.errorMessage != null -> ErrorContent(state.errorMessage, uiEvents.onClickRetry)
                state.isLoading -> LoadingContent()
                state.selectedBodyPart == null -> CenterText(text = "부위를 먼저 선택하세요")
                state.exercises.isEmpty() -> CenterText(text = "이 부위에 등록된 운동이 없습니다")
                else -> ExerciseAndSets(state, uiEvents)
            }
        }

        VerticalSpacer(space = 8.dp)
        Button(
            onClick = uiEvents.onClickSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            BaseText(text = "저장")
        }
    }
}

@Composable
private fun ExerciseAndSets(state: WorkoutEntryEditState, uiEvents: WorkoutEntryEditUiEvents) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ExerciseChipRow(
                exercises = state.selectableExercises,
                selectedId = state.selectedExercise?.id,
                onSelect = uiEvents.onSelectExercise,
            )
        }

        if (state.selectedExercise == null) {
            item { CenterText(text = "운동을 선택하세요") }
            return@LazyColumn
        }

        setItems(state, uiEvents)

        item {
            TextButton(onClick = uiEvents.onClickAddSet, enabled = state.canAddSet) {
                BaseText(text = "세트 추가")
            }
        }
    }
}

private fun LazyListScope.setItems(state: WorkoutEntryEditState, uiEvents: WorkoutEntryEditUiEvents) {
    val intensityType = state.selectedExercise?.intensityType ?: IntensityType.WEIGHT
    val intensityOptions = when (intensityType) {
        IntensityType.WEIGHT -> WorkoutOptions.weightKilograms
        IntensityType.ANGLE -> WorkoutOptions.angleDegrees
    }
    val intensityLabel = if (intensityType == IntensityType.WEIGHT) "무게" else "각도"
    val intensitySuffix = if (intensityType == IntensityType.WEIGHT) "kg" else "도"

    itemsIndexed(items = state.sets, key = { _, setInput -> setInput.id }) { index, setInput ->
        SetCard(
            setNumber = index + 1,
            setInput = setInput,
            intensityOptions = intensityOptions,
            intensityLabel = intensityLabel,
            intensitySuffix = intensitySuffix,
            canRemove = state.canRemoveSet,
            onClickRemove = { uiEvents.onClickRemoveSet(setInput.id) },
            onSelectRepeatCount = { uiEvents.onSelectSetRepeatCount(setInput.id, it) },
            onSelectIntensity = { uiEvents.onSelectSetIntensity(setInput.id, it) },
        )
    }
}

@Composable
private fun ExerciseChipRow(exercises: List<Exercise>, selectedId: Long?, onSelect: (Long) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(items = exercises, key = { it.id }) { exercise ->
            FilterChip(
                selected = exercise.id == selectedId,
                onClick = { onSelect(exercise.id) },
                label = { BaseText(text = exercise.name) },
            )
        }
    }
}

@Composable
private fun SetCard(
    setNumber: Int,
    setInput: SetInput,
    intensityOptions: List<Int>,
    intensityLabel: String,
    intensitySuffix: String,
    canRemove: Boolean,
    onClickRemove: () -> Unit,
    onSelectRepeatCount: (Int) -> Unit,
    onSelectIntensity: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BaseText(
                    text = "${setNumber}세트",
                    style = MaterialTheme.typography.titleSmall,
                )
                WeightSpacer()
                TextButton(onClick = onClickRemove, enabled = canRemove) {
                    BaseText(text = "세트 삭제")
                }
            }
            BaseText(text = intensityLabel, style = MaterialTheme.typography.labelMedium)
            OptionChipRow(
                options = intensityOptions,
                selected = setInput.intensityValue,
                onSelect = onSelectIntensity,
                suffix = intensitySuffix,
            )
            VerticalSpacer(space = 6.dp)
            BaseText(text = "횟수", style = MaterialTheme.typography.labelMedium)
            OptionChipRow(
                options = WorkoutOptions.repeatCounts,
                selected = setInput.repeatCount,
                onSelect = onSelectRepeatCount,
                suffix = "회",
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
private fun CenterText(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BaseText(text = text)
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

@Preview(showBackground = true)
@Composable
private fun WorkoutEntryEditViewImplPreview() {
    BodyPlanTheme {
        WorkoutEntryEditViewImpl(
            state = WorkoutEntryEditState(
                date = LocalDate.of(2026, 9, 7),
                selectedBodyPart = BodyPart.CHEST,
                exercises = listOf(
                    Exercise(1L, BodyPart.CHEST, "플랫 벤치프레스 머신", IntensityType.WEIGHT),
                    Exercise(2L, BodyPart.CHEST, "푸쉬업", IntensityType.ANGLE),
                ),
                selectedExercise = Exercise(1L, BodyPart.CHEST, "플랫 벤치프레스 머신", IntensityType.WEIGHT),
                sets = listOf(
                    SetInput(id = 0L, repeatCount = 12, intensityValue = 40),
                    SetInput(id = 1L, repeatCount = 8, intensityValue = 45),
                ),
                nextSetInputId = 2L,
            ),
            uiEvents = WorkoutEntryEditUiEvents(
                onBackPressed = {},
                onSelectBodyPart = {},
                onSelectExercise = {},
                onClickAddSet = {},
                onClickRemoveSet = {},
                onSelectSetRepeatCount = { _, _ -> },
                onSelectSetIntensity = { _, _ -> },
                onClickSave = {},
                onClickRetry = {},
            ),
        )
    }
}
