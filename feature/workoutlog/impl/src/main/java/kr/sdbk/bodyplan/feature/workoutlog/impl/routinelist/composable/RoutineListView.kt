package kr.sdbk.bodyplan.feature.workoutlog.impl.routinelist.composable

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
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.ui.components.BodyPartTabRow
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.routinelist.RoutineListEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.routinelist.RoutineListIntent
import kr.sdbk.bodyplan.feature.workoutlog.impl.routinelist.RoutineListState
import kr.sdbk.bodyplan.feature.workoutlog.impl.routinelist.RoutineListViewModel

internal data class RoutineListEvents(val goBack: () -> Unit, val goToRoutineDetail: (Long) -> Unit)

internal data class RoutineListUiEvents(
    val onBackPressed: () -> Unit,
    val onClickAddRoutine: () -> Unit,
    val onChangeDialogName: (String) -> Unit,
    val onSelectDialogBodyPart: (BodyPart) -> Unit,
    val onConfirmDialog: () -> Unit,
    val onDismissDialog: () -> Unit,
    val onClickRoutine: (Long) -> Unit,
    val onClickDeleteRoutine: (Long) -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun RoutineListView(events: RoutineListEvents, viewModel: RoutineListViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)
    val context = LocalContext.current

    RoutineListViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is RoutineListEffect.GoBack -> events.goBack()

            is RoutineListEffect.NavigateToRoutineDetail -> events.goToRoutineDetail(effect.routineId)

            is RoutineListEffect.ShowMessage ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun rememberUiEvents(events: RoutineListEvents, viewModel: RoutineListViewModel): RoutineListUiEvents =
    remember {
        RoutineListUiEvents(
            onBackPressed = events.goBack,
            onClickAddRoutine = { viewModel.handleIntent(RoutineListIntent.ClickAddRoutine) },
            onChangeDialogName = { viewModel.handleIntent(RoutineListIntent.ChangeDialogName(it)) },
            onSelectDialogBodyPart = { viewModel.handleIntent(RoutineListIntent.SelectDialogBodyPart(it)) },
            onConfirmDialog = { viewModel.handleIntent(RoutineListIntent.ConfirmDialog) },
            onDismissDialog = { viewModel.handleIntent(RoutineListIntent.DismissDialog) },
            onClickRoutine = { viewModel.handleIntent(RoutineListIntent.ClickRoutine(it)) },
            onClickDeleteRoutine = { viewModel.handleIntent(RoutineListIntent.ClickDeleteRoutine(it)) },
            onClickRetry = { viewModel.handleIntent(RoutineListIntent.ClickRetry) },
        )
    }

@Composable
internal fun RoutineListViewImpl(state: RoutineListState, uiEvents: RoutineListUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(
            title = "루틴 관리",
            onBack = uiEvents.onBackPressed,
            actionText = "루틴 추가",
            onClickAction = uiEvents.onClickAddRoutine,
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.errorMessage != null -> ErrorContent(state.errorMessage, uiEvents.onClickRetry)
                state.isLoading && state.routines.isEmpty() -> LoadingContent()
                state.routines.isEmpty() -> EmptyContent()
                else -> RoutineSections(state, uiEvents)
            }
        }
    }

    if (state.isDialogVisible) {
        RoutineCreateDialog(state, uiEvents)
    }
}

@Composable
private fun RoutineSections(state: RoutineListState, uiEvents: RoutineListUiEvents) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.sections.forEach { (bodyPart, routines) ->
            item(key = "section-${bodyPart.name}") {
                BaseText(
                    text = bodyPart.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                )
            }
            items(items = routines, key = { it.id }) { routine ->
                RoutineCard(
                    routine = routine,
                    onClick = { uiEvents.onClickRoutine(routine.id) },
                    onClickDelete = { uiEvents.onClickDeleteRoutine(routine.id) },
                )
            }
        }
    }
}

@Composable
private fun RoutineCard(routine: Routine, onClick: () -> Unit, onClickDelete: () -> Unit) {
    BodyPlanCard(modifier = Modifier.clickable(onClick = onClick), contentPadding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BaseText(
                    text = routine.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BaseText(
                    text = "종목 ${routine.entries.size}개",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            BaseText(
                text = "삭제",
                modifier = Modifier.clickable(onClick = onClickDelete),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
    }
}

@Composable
private fun RoutineCreateDialog(state: RoutineListState, uiEvents: RoutineListUiEvents) {
    AlertDialog(
        onDismissRequest = uiEvents.onDismissDialog,
        containerColor = Surface,
        title = {
            BaseText(
                text = "루틴 추가",
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
                    placeholder = "루틴 이름",
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                )
                VerticalSpacer(space = 12.dp)
                BodyPartTabRow(
                    selected = state.dialogBodyPart,
                    onSelect = uiEvents.onSelectDialogBodyPart,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            BaseText(
                text = if (state.isCreating) "만드는 중..." else "만들기",
                modifier = Modifier.clickable(enabled = !state.isCreating, onClick = uiEvents.onConfirmDialog),
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
            text = "만든 루틴이 없습니다",
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

private val previewRoutines = listOf(
    Routine(id = 1L, name = "가슴 A", bodyPart = BodyPart.CHEST, entries = emptyList()),
    Routine(id = 2L, name = "가슴 B", bodyPart = BodyPart.CHEST, entries = emptyList()),
    Routine(id = 3L, name = "등 기본", bodyPart = BodyPart.BACK, entries = emptyList()),
)

private val previewUiEvents = RoutineListUiEvents(
    onBackPressed = {},
    onClickAddRoutine = {},
    onChangeDialogName = {},
    onSelectDialogBodyPart = {},
    onConfirmDialog = {},
    onDismissDialog = {},
    onClickRoutine = {},
    onClickDeleteRoutine = {},
    onClickRetry = {},
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun RoutineListViewImplPreview() {
    BodyPlanTheme {
        RoutineListViewImpl(
            state = RoutineListState(routines = previewRoutines),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun RoutineListViewImplEmptyPreview() {
    BodyPlanTheme {
        RoutineListViewImpl(
            state = RoutineListState(),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun RoutineListViewImplDialogPreview() {
    BodyPlanTheme {
        RoutineListViewImpl(
            state = RoutineListState(routines = previewRoutines, isDialogVisible = true, dialogName = "가슴 C"),
            uiEvents = previewUiEvents,
        )
    }
}
