package kr.sdbk.bodyplan.feature.workoutlog.impl.exercisemanage.composable

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BaseTextField
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.PillChip
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.ui.components.BodyPartTabRow
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisemanage.ExerciseManageEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisemanage.ExerciseManageIntent
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisemanage.ExerciseManageState
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisemanage.ExerciseManageViewModel

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(
            title = "운동 종목 관리",
            onBack = uiEvents.onBackPressed,
            actionText = "종목 추가",
            onClickAction = uiEvents.onClickAdd,
        )

        BodyPartTabRow(
            selected = state.selectedBodyPart,
            onSelect = uiEvents.onSelectBodyPart,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(space = 16.dp)

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
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = state.exercises, key = { it.id }) { exercise ->
            BodyPlanCard(contentPadding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        BaseText(
                            text = exercise.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        BaseText(
                            text = exercise.intensityType.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BaseText(
                            text = "수정",
                            modifier = Modifier.clickable { uiEvents.onClickEdit(exercise.id) },
                            style = MaterialTheme.typography.bodySmall,
                            color = Accent,
                        )
                        BaseText(
                            text = "삭제",
                            modifier = Modifier.clickable { uiEvents.onClickDelete(exercise.id) },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                        )
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
        containerColor = Surface,
        title = {
            BaseText(
                text = if (state.editingExercise == null) "종목 추가" else "종목 수정",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
        },
        text = {
            Column {
                BaseTextField(
                    value = state.dialogName,
                    onValueChange = uiEvents.onChangeDialogName,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "종목 이름",
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                )
                VerticalSpacer(space = 16.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntensityType.entries.forEach { type ->
                        PillChip(
                            text = type.label,
                            selected = type == state.dialogIntensityType,
                            onClick = { uiEvents.onSelectDialogIntensityType(type) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            BaseText(
                text = "확인",
                modifier = Modifier.clickable(onClick = uiEvents.onConfirmDialog),
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
            )
        },
        dismissButton = {
            BaseText(
                text = "취소",
                modifier = Modifier
                    .clickable(onClick = uiEvents.onDismissDialog)
                    .padding(end = 16.dp),
                style = MaterialTheme.typography.labelLarge,
                color = TextTertiary,
            )
        },
    )
}

private val IntensityType.label: String
    get() = when (this) {
        IntensityType.WEIGHT -> "무게로 기록"
        IntensityType.ANGLE -> "각도로 기록"
        IntensityType.DURATION -> "시간으로 기록"
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
            text = "등록된 운동이 없습니다",
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
    Exercise(3L, BodyPart.CHEST, "팩덱 플라이 머신", IntensityType.WEIGHT),
    Exercise(4L, BodyPart.CHEST, "푸쉬업", IntensityType.ANGLE),
)

private val previewUiEvents = ExerciseManageUiEvents(
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
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ExerciseManageViewImplPreview() {
    BodyPlanTheme {
        ExerciseManageViewImpl(
            state = ExerciseManageState(exercises = previewExercises),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ExerciseManageViewImplEmptyPreview() {
    BodyPlanTheme {
        ExerciseManageViewImpl(
            state = ExerciseManageState(),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ExerciseManageViewImplDialogPreview() {
    BodyPlanTheme {
        ExerciseManageViewImpl(
            state = ExerciseManageState(
                exercises = previewExercises,
                isDialogVisible = true,
                dialogName = "푸쉬업",
                dialogIntensityType = IntensityType.ANGLE,
            ),
            uiEvents = previewUiEvents,
        )
    }
}
