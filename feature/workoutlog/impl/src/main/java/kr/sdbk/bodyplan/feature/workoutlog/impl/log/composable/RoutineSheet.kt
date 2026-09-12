package kr.sdbk.bodyplan.feature.workoutlog.impl.log.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.ui.components.label

/** 일지에 넣을 루틴을 고르는 시트. 부위별로 묶어 보인다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutineSheet(
    sections: List<Pair<BodyPart, List<Routine>>>,
    isApplying: Boolean,
    onSelectRoutine: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    // 넣는 중에는 스와이프로도 닫히지 않게 한다 — 닫힌 뒤에 오는 dismiss를 ViewModel이 무시하면 화면과 상태가 어긋난다.
    val currentIsApplying by rememberUpdatedState(isApplying)
    val sheetState = rememberModalBottomSheetState(confirmValueChange = { !currentIsApplying })
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Surface) {
        RoutineSheetContent(
            sections = sections,
            isApplying = isApplying,
            onSelectRoutine = onSelectRoutine,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
private fun RoutineSheetContent(
    sections: List<Pair<BodyPart, List<Routine>>>,
    isApplying: Boolean,
    onSelectRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BaseText(
            text = "루틴 불러오기",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        VerticalSpacer(space = 8.dp)
        if (sections.isEmpty()) {
            EmptyText()
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            sections.forEach { (bodyPart, routines) ->
                item(key = "section-${bodyPart.name}") {
                    BaseText(
                        text = bodyPart.label,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                    )
                }
                items(items = routines, key = { it.id }) { routine ->
                    RoutineRow(
                        routine = routine,
                        enabled = !isApplying,
                        onClick = { onSelectRoutine(routine.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineRow(routine: Routine, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BaseText(
            text = routine.name,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
        )
        WeightSpacer()
        BaseText(
            text = "종목 ${routine.entries.size}개",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun EmptyText() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        BaseText(
            text = "만든 루틴이 없습니다",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutineSheetContentPreview() {
    BodyPlanTheme {
        RoutineSheetContent(
            sections = listOf(
                BodyPart.CHEST to listOf(
                    Routine(id = 1L, name = "가슴 A", bodyPart = BodyPart.CHEST, entries = emptyList()),
                    Routine(id = 2L, name = "가슴 B", bodyPart = BodyPart.CHEST, entries = emptyList()),
                ),
                BodyPart.BACK to listOf(
                    Routine(id = 3L, name = "등 기본", bodyPart = BodyPart.BACK, entries = emptyList()),
                ),
            ),
            isApplying = false,
            onSelectRoutine = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutineSheetContentEmptyPreview() {
    BodyPlanTheme {
        RoutineSheetContent(sections = emptyList(), isApplying = false, onSelectRoutine = {})
    }
}
