package kr.sdbk.bodyplan.feature.workoutlog.impl.log.composable

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kr.sdbk.bodyplan.core.designsystem.component.Badge
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
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutAnalysisPeriod
import kr.sdbk.bodyplan.feature.workoutlog.impl.log.WorkoutLogEffect
import kr.sdbk.bodyplan.feature.workoutlog.impl.log.WorkoutLogIntent
import kr.sdbk.bodyplan.feature.workoutlog.impl.log.WorkoutLogState
import kr.sdbk.bodyplan.feature.workoutlog.impl.log.WorkoutLogViewModel

internal data class WorkoutLogEvents(
    val goBack: () -> Unit,
    val goToEntryEdit: (LocalDate, Long?) -> Unit,
    val goToAnalysis: (WorkoutAnalysisPeriod, LocalDate) -> Unit,
)

internal data class WorkoutLogUiEvents(
    val onBackPressed: () -> Unit,
    val onClickAddEntry: () -> Unit,
    val onClickEntry: (Long) -> Unit,
    val onClickDeleteEntry: (Long) -> Unit,
    val onClickAnalyze: () -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun WorkoutLogView(events: WorkoutLogEvents, viewModel: WorkoutLogViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel, state.date)
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
private fun rememberUiEvents(
    events: WorkoutLogEvents,
    viewModel: WorkoutLogViewModel,
    date: LocalDate,
): WorkoutLogUiEvents = remember(viewModel, date) {
    WorkoutLogUiEvents(
        onBackPressed = events.goBack,
        onClickAddEntry = { viewModel.handleIntent(WorkoutLogIntent.ClickAddEntry) },
        onClickEntry = { viewModel.handleIntent(WorkoutLogIntent.ClickEntry(it)) },
        onClickDeleteEntry = { viewModel.handleIntent(WorkoutLogIntent.ClickDeleteEntry(it)) },
        onClickAnalyze = { events.goToAnalysis(WorkoutAnalysisPeriod.DAILY, date) },
        onClickRetry = { viewModel.handleIntent(WorkoutLogIntent.ClickRetry) },
    )
}

@Composable
internal fun WorkoutLogViewImpl(state: WorkoutLogState, uiEvents: WorkoutLogUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(
            title = state.date.format(DATE_FORMAT),
            onBack = uiEvents.onBackPressed,
            actionText = if (state.isEditable) "운동 추가" else null,
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
            OutlinedActionButton(text = "운동 분석", onClick = uiEvents.onClickAnalyze)
        }
    }
}

@Composable
private fun EntryList(state: WorkoutLogState, uiEvents: WorkoutLogUiEvents) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
    BodyPlanCard(
        modifier = if (isEditable) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BaseText(
                text = entry.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            Badge(text = entry.bodyPart.label, modifier = Modifier.padding(start = 8.dp))
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
        VerticalSpacer(space = 14.dp)
        HorizontalDivider(color = Border)
        VerticalSpacer(space = 14.dp)
        entry.sets.forEachIndexed { index, set ->
            if (index > 0) VerticalSpacer(space = 8.dp)
            SetRow(setNumber = index + 1, set = set)
        }
    }
}

@Composable
private fun SetRow(setNumber: Int, set: WorkoutSet) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BaseText(
            text = "${setNumber}세트",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        BaseText(
            text = intensityText(set),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            ),
            color = TextPrimary,
        )
    }
}

private fun intensityText(set: WorkoutSet): String = when (set.intensity) {
    is Intensity.Weight -> "${set.intensity.value}kg × ${set.repeatCount}회"

    is Intensity.Angle -> "${set.intensity.value}도 × ${set.repeatCount}회"

    // 시간으로 재는 종목은 한 세트가 한 회차라 횟수를 적지 않는다.
    is Intensity.Duration -> "${set.intensity.value}분"
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

private val previewEntries = listOf(
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
        exerciseId = 2L,
        exerciseName = "사이드 레터럴 레이즈 머신",
        bodyPart = BodyPart.SHOULDER,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(5))),
    ),
)

private val previewUiEvents = WorkoutLogUiEvents(
    onBackPressed = {},
    onClickAnalyze = {},
    onClickAddEntry = {},
    onClickEntry = {},
    onClickDeleteEntry = {},
    onClickRetry = {},
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WorkoutLogViewImplPreview() {
    BodyPlanTheme {
        WorkoutLogViewImpl(
            state = WorkoutLogState(
                date = LocalDate.of(2026, 9, 7),
                isEditable = true,
                entries = previewEntries,
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WorkoutLogViewImplReadOnlyPreview() {
    BodyPlanTheme {
        WorkoutLogViewImpl(
            state = WorkoutLogState(
                date = LocalDate.of(2026, 8, 30),
                isEditable = false,
                entries = previewEntries,
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WorkoutLogViewImplEmptyPreview() {
    BodyPlanTheme {
        WorkoutLogViewImpl(
            state = WorkoutLogState(date = LocalDate.of(2026, 9, 7), isEditable = true),
            uiEvents = previewUiEvents,
        )
    }
}
