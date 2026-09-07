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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BaseTextField
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.ui.components.BodyPartTabRow
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect

internal data class ExerciseManageEvents(val goBack: () -> Unit)

internal data class ExerciseManageUiEvents(
    val onBackPressed: () -> Unit,
    val onSelectBodyPart: (BodyPart) -> Unit,
    val onClickAdd: () -> Unit,
    val onClickEdit: (Long) -> Unit,
    val onClickDelete: (Long) -> Unit,
    val onChangeDialogName: (String) -> Unit,
    val onSelectDialogIntensityType: (IntensityType) -> Unit,
    val onConfirmDialog: () -> Unit,
    val onDismissDialog: () -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun ExerciseManageView(events: ExerciseManageEvents, viewModel: ExerciseManageViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)
    val context = LocalContext.current

    ExerciseManageViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is ExerciseManageEffect.GoBack -> events.goBack()

            is ExerciseManageEffect.ShowMessage ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun rememberUiEvents(
    events: ExerciseManageEvents,
    viewModel: ExerciseManageViewModel,
): ExerciseManageUiEvents = remember {
    ExerciseManageUiEvents(
        onBackPressed = events.goBack,
        onSelectBodyPart = { viewModel.handleIntent(ExerciseManageIntent.SelectBodyPart(it)) },
        onClickAdd = { viewModel.handleIntent(ExerciseManageIntent.ClickAdd) },
        onClickEdit = { viewModel.handleIntent(ExerciseManageIntent.ClickEdit(it)) },
        onClickDelete = { viewModel.handleIntent(ExerciseManageIntent.ClickDelete(it)) },
        onChangeDialogName = { viewModel.handleIntent(ExerciseManageIntent.ChangeDialogName(it)) },
        onSelectDialogIntensityType = {
            viewModel.handleIntent(ExerciseManageIntent.SelectDialogIntensityType(it))
        },
        onConfirmDialog = { viewModel.handleIntent(ExerciseManageIntent.ConfirmDialog) },
        onDismissDialog = { viewModel.handleIntent(ExerciseManageIntent.DismissDialog) },
        onClickRetry = { viewModel.handleIntent(ExerciseManageIntent.ClickRetry) },
    )
}

@Composable
internal fun ExerciseManageViewImpl(state: ExerciseManageState, uiEvents: ExerciseManageUiEvents) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = uiEvents.onBackPressed) { BaseText(text = "뒤로") }
            BaseText(text = "운동 종목 관리", style = MaterialTheme.typography.titleMedium)
            WeightSpacer()
            TextButton(onClick = uiEvents.onClickAdd) { BaseText(text = "종목 추가") }
        }
        VerticalSpacer(space = 8.dp)

        BodyPartTabRow(
            selected = state.selectedBodyPart,
            onSelect = uiEvents.onSelectBodyPart,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(space = 12.dp)

        when {
            state.errorMessage != null -> ErrorContent(state.errorMessage, uiEvents.onClickRetry)
            state.isLoading && state.exercises.isEmpty() -> LoadingContent()
            state.exercises.isEmpty() -> EmptyContent()
            else -> ExerciseList(state, uiEvents)
        }
    }

    if (state.isDialogVisible) {
        ExerciseDialog(state, uiEvents)
    }
}

@Composable
private fun ExerciseList(state: ExerciseManageState, uiEvents: ExerciseManageUiEvents) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items = state.exercises, key = { it.id }) { exercise ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BaseText(text = exercise.name, style = MaterialTheme.typography.titleSmall)
                        BaseText(
                            text = exercise.intensityType.label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    TextButton(onClick = { uiEvents.onClickEdit(exercise.id) }) {
                        BaseText(text = "수정")
                    }
                    TextButton(onClick = { uiEvents.onClickDelete(exercise.id) }) {
                        BaseText(text = "삭제")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseDialog(state: ExerciseManageState, uiEvents: ExerciseManageUiEvents) {
    AlertDialog(
        onDismissRequest = uiEvents.onDismissDialog,
        title = {
            BaseText(text = if (state.editingExercise == null) "종목 추가" else "종목 수정")
        },
        text = {
            Column {
                BaseTextField(
                    value = state.dialogName,
                    onValueChange = uiEvents.onChangeDialogName,
                    placeholder = "종목 이름",
                    singleLine = true,
                )
                VerticalSpacer(space = 12.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IntensityType.entries.forEach { type ->
                        FilterChip(
                            selected = type == state.dialogIntensityType,
                            onClick = { uiEvents.onSelectDialogIntensityType(type) },
                            label = { BaseText(text = type.label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = uiEvents.onConfirmDialog) { BaseText(text = "확인") }
        },
        dismissButton = {
            TextButton(onClick = uiEvents.onDismissDialog) { BaseText(text = "취소") }
        },
    )
}

private val IntensityType.label: String
    get() = when (this) {
        IntensityType.WEIGHT -> "무게로 기록"
        IntensityType.ANGLE -> "각도로 기록"
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
        BaseText(text = "등록된 운동이 없습니다")
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
private fun ExerciseManageViewImplPreview() {
    BodyPlanTheme {
        ExerciseManageViewImpl(
            state = ExerciseManageState(
                exercises = listOf(
                    Exercise(1L, BodyPart.CHEST, "플랫 벤치프레스 머신", IntensityType.WEIGHT),
                    Exercise(2L, BodyPart.CHEST, "푸쉬업", IntensityType.ANGLE),
                ),
            ),
            uiEvents = ExerciseManageUiEvents(
                onBackPressed = {},
                onSelectBodyPart = {},
                onClickAdd = {},
                onClickEdit = {},
                onClickDelete = {},
                onChangeDialogName = {},
                onSelectDialogIntensityType = {},
                onConfirmDialog = {},
                onDismissDialog = {},
                onClickRetry = {},
            ),
        )
    }
}
