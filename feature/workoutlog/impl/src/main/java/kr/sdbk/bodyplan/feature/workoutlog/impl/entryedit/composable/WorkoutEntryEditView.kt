package kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit.composable

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BaseTextField
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.ItemChip
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WheelPicker
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.SurfaceMuted
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutOptions
import kr.sdbk.bodyplan.core.domain.model.countsRepeats
import kr.sdbk.bodyplan.core.ui.components.BodyPartTabRow
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit.SetInput
import kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit.WorkoutEntryEditEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit.WorkoutEntryEditIntent
import kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit.WorkoutEntryEditState
import kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit.WorkoutEntryEditTarget
import kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit.WorkoutEntryEditViewModel

internal data class WorkoutEntryEditEvents(val goBack: () -> Unit)

internal data class WorkoutEntryEditUiEvents(
    val onBackPressed: () -> Unit,
    val onSelectBodyPart: (BodyPart) -> Unit,
    val onSelectExercise: (Long) -> Unit,
    val onClickAddSet: () -> Unit,
    val onClickRemoveSet: (Long) -> Unit,
    val onChangeSetRepeatCount: (Long, String) -> Unit,
    val onChangeSetIntensity: (Long, String) -> Unit,
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
        onChangeSetRepeatCount = { id, text ->
            viewModel.handleIntent(WorkoutEntryEditIntent.ChangeSetRepeatCount(id, text))
        },
        onChangeSetIntensity = { id, text ->
            viewModel.handleIntent(WorkoutEntryEditIntent.ChangeSetIntensity(id, text))
        },
        onClickSave = { viewModel.handleIntent(WorkoutEntryEditIntent.ClickSave) },
        onClickRetry = { viewModel.handleIntent(WorkoutEntryEditIntent.ClickRetry) },
    )
}

@Composable
internal fun WorkoutEntryEditViewImpl(state: WorkoutEntryEditState, uiEvents: WorkoutEntryEditUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(
            title = if (state.editingEntryId == null) "운동 추가" else "운동 수정",
            onBack = uiEvents.onBackPressed,
        )

        if (!state.isBodyPartLocked) {
            BodyPartTabRow(
                selected = state.selectedBodyPart,
                onSelect = uiEvents.onSelectBodyPart,
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(space = 12.dp)
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.errorMessage != null -> ErrorContent(state.errorMessage, uiEvents.onClickRetry)
                state.isLoading -> LoadingContent()
                state.selectedBodyPart == null -> CenterText("부위를 먼저 선택하세요")
                state.exercises.isEmpty() -> CenterText("이 부위에 등록된 운동이 없습니다")
                else -> ExerciseAndSets(state, uiEvents)
            }
        }

        PrimaryButton(
            text = "저장",
            onClick = uiEvents.onClickSave,
            modifier = Modifier.padding(16.dp),
            enabled = state.canSave,
        )
    }
}

@Composable
private fun ExerciseAndSets(state: WorkoutEntryEditState, uiEvents: WorkoutEntryEditUiEvents) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(items = state.selectableExercises, key = { it.id }) { exercise ->
                    ItemChip(
                        text = exercise.name,
                        selected = exercise.id == state.selectedExercise?.id,
                        onClick = { uiEvents.onSelectExercise(exercise.id) },
                    )
                }
            }
        }

        if (state.selectedExercise == null) {
            item { CenterText("운동을 선택하세요") }
            return@LazyColumn
        }

        setItems(state, uiEvents)

        item {
            BaseText(
                text = "+ 세트 추가",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(enabled = state.canAddSet, onClick = uiEvents.onClickAddSet)
                    .padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.canAddSet) Accent else TextTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.setItems(
    state: WorkoutEntryEditState,
    uiEvents: WorkoutEntryEditUiEvents,
) {
    val intensityType = state.selectedExercise?.intensityType ?: IntensityType.WEIGHT
    val intensityOptions = when (intensityType) {
        IntensityType.WEIGHT -> null
        IntensityType.ANGLE -> WorkoutOptions.angleDegrees
        IntensityType.DURATION -> WorkoutOptions.durationMinutes
    }
    val intensityLabel = when (intensityType) {
        IntensityType.WEIGHT -> "무게"
        IntensityType.ANGLE -> "각도"
        IntensityType.DURATION -> "시간"
    }
    val intensitySuffix = when (intensityType) {
        IntensityType.WEIGHT -> "kg"
        IntensityType.ANGLE -> "도"
        IntensityType.DURATION -> "분"
    }

    itemsIndexed(items = state.sets, key = { _, setInput -> setInput.id }) { index, setInput ->
        SetCard(
            setNumber = index + 1,
            setInput = setInput,
            intensityOptions = intensityOptions,
            intensityLabel = intensityLabel,
            intensitySuffix = intensitySuffix,
            showRepeatCount = intensityType.countsRepeats,
            canRemove = state.canRemoveSet,
            onClickRemove = { uiEvents.onClickRemoveSet(setInput.id) },
            onChangeRepeatCount = { uiEvents.onChangeSetRepeatCount(setInput.id, it) },
            onChangeIntensity = { uiEvents.onChangeSetIntensity(setInput.id, it) },
        )
    }
}

@Composable
private fun SetCard(
    setNumber: Int,
    setInput: SetInput,
    intensityOptions: List<Int>?,
    intensityLabel: String,
    intensitySuffix: String,
    showRepeatCount: Boolean,
    canRemove: Boolean,
    onClickRemove: () -> Unit,
    onChangeRepeatCount: (String) -> Unit,
    onChangeIntensity: (String) -> Unit,
) {
    BodyPlanCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BaseText(
                text = "${setNumber}세트",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
            WeightSpacer()
            if (canRemove) {
                BaseText(
                    text = "세트 삭제",
                    modifier = Modifier.clickable(onClick = onClickRemove),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
        }
        VerticalSpacer(space = 16.dp)
        HorizontalDivider(color = Border)
        VerticalSpacer(space = 16.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (intensityOptions == null) {
                NumberField(
                    label = intensityLabel,
                    value = setInput.intensityValue,
                    onChange = onChangeIntensity,
                    suffix = intensitySuffix,
                    modifier = Modifier.weight(1f),
                )
            } else {
                WheelPicker(
                    label = intensityLabel,
                    options = intensityOptions,
                    selected = setInput.intensityValue ?: intensityOptions.first(),
                    onSelect = { onChangeIntensity(it.toString()) },
                    modifier = Modifier.weight(1f),
                    suffix = intensitySuffix,
                )
            }
            // 시간으로 재는 종목은 한 세트가 한 회차라 횟수를 고를 것이 없다.
            if (showRepeatCount) {
                NumberField(
                    label = "횟수",
                    value = setInput.repeatCount,
                    onChange = onChangeRepeatCount,
                    suffix = "회",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 숫자 한 칸. 걸러 내는 일은 ViewModel이 한다 — 무엇이 유효한 입력인지가 저장 규칙과 같은 곳에 있어야 한다. */
@Composable
private fun NumberField(
    label: String,
    value: Int?,
    onChange: (String) -> Unit,
    suffix: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BaseText(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary,
        )
        VerticalSpacer(space = 8.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceMuted)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BaseTextField(
                value = value?.toString().orEmpty(),
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            BaseText(text = suffix, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        BaseText(
            text = text,
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

private val previewExercises = listOf(
    Exercise(1L, BodyPart.CHEST, "인클라인 벤치프레스 머신", IntensityType.WEIGHT),
    Exercise(2L, BodyPart.CHEST, "플랫 벤치프레스 머신", IntensityType.WEIGHT),
    Exercise(3L, BodyPart.CHEST, "푸쉬업", IntensityType.ANGLE),
)

private val previewUiEvents = WorkoutEntryEditUiEvents(
    onBackPressed = {},
    onSelectBodyPart = {},
    onSelectExercise = {},
    onClickAddSet = {},
    onClickRemoveSet = {},
    onChangeSetRepeatCount = { _, _ -> },
    onChangeSetIntensity = { _, _ -> },
    onClickSave = {},
    onClickRetry = {},
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WorkoutEntryEditViewImplPreview() {
    BodyPlanTheme {
        WorkoutEntryEditViewImpl(
            state = WorkoutEntryEditState(
                target = WorkoutEntryEditTarget.Log(LocalDate.of(2026, 9, 7)),
                selectedBodyPart = BodyPart.CHEST,
                exercises = previewExercises,
                selectedExercise = previewExercises.first(),
                sets = listOf(SetInput(id = 0L, repeatCount = 4, intensityValue = 10)),
                nextSetInputId = 1L,
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WorkoutEntryEditViewImplNoBodyPartPreview() {
    BodyPlanTheme {
        WorkoutEntryEditViewImpl(
            state = WorkoutEntryEditState(target = WorkoutEntryEditTarget.Log(LocalDate.of(2026, 9, 7))),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WorkoutEntryEditViewImplNoExercisePreview() {
    BodyPlanTheme {
        WorkoutEntryEditViewImpl(
            state = WorkoutEntryEditState(
                target = WorkoutEntryEditTarget.Log(LocalDate.of(2026, 9, 7)),
                selectedBodyPart = BodyPart.CHEST,
                exercises = previewExercises,
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WorkoutEntryEditViewImplRoutinePreview() {
    BodyPlanTheme {
        WorkoutEntryEditViewImpl(
            state = WorkoutEntryEditState(
                target = WorkoutEntryEditTarget.Routine(routineId = 1L),
                selectedBodyPart = BodyPart.CHEST,
                exercises = previewExercises,
                selectedExercise = previewExercises.first(),
                sets = listOf(SetInput(id = 0L, repeatCount = 4, intensityValue = 10)),
                nextSetInputId = 1L,
            ),
            uiEvents = previewUiEvents,
        )
    }
}
