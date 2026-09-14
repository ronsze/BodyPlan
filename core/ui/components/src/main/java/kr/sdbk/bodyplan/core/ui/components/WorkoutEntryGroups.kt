package kr.sdbk.bodyplan.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.Badge
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanIcon
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanIcons
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
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

/**
 * 부위·종목 그룹의 펼침 상태. 화면 표시 상태라 ViewModel에 두지 않고 스크롤 위치처럼 화면이 든다.
 *
 * 펼친 키만 저장한다 — 기본이 전부 접힘이라, 항목이 새로 생겨도 따로 등록할 것이 없다.
 */
@Stable
class WorkoutEntryGroupExpansion internal constructor(initialExpanded: Set<String>) {
    private val expanded = mutableStateOf(initialExpanded)

    internal val expandedKeys: Set<String> get() = expanded.value

    fun isExpanded(key: String): Boolean = key in expanded.value

    fun toggle(key: String) {
        expanded.value = if (key in expanded.value) expanded.value - key else expanded.value + key
    }
}

@Composable
fun rememberWorkoutEntryGroupExpansion(): WorkoutEntryGroupExpansion = rememberSaveable(saver = ExpansionSaver) {
    WorkoutEntryGroupExpansion(emptySet())
}

private val ExpansionSaver = Saver<WorkoutEntryGroupExpansion, List<String>>(
    save = { it.expandedKeys.toList() },
    restore = { WorkoutEntryGroupExpansion(it.toSet()) },
)

/**
 * 기록을 부위 → 종목 → 세트로 묶어 목록에 늘어놓는다. 일간 일지와 루틴 상세가 함께 쓴다.
 *
 * 같은 종목이 여러 건이면 종목 하나 아래에 건별로 나뉘어 보인다 — 건마다 세트가 다르고 따로 고칠 수 있어야 한다.
 * 부위 순서는 [BodyPart.entries], 그 안 순서는 [entries]의 순서(기록한 순)를 따른다.
 */
fun LazyListScope.workoutEntryGroups(
    entries: List<WorkoutEntry>,
    isEditable: Boolean,
    expansion: WorkoutEntryGroupExpansion,
    onClickEntry: (Long) -> Unit,
    onClickDeleteEntry: (Long) -> Unit,
    personalRecordExerciseIds: Set<Long> = emptySet(),
    // null이면 세션이 아니다 — 체크 칸을 그리지 않는다.
    completedSets: Set<WorkoutSetKey>? = null,
    onToggleSet: (WorkoutSetKey) -> Unit = {},
) {
    val byBodyPart = entries.groupBy { it.bodyPart }
    BodyPart.entries.forEach { bodyPart ->
        val bodyPartEntries = byBodyPart[bodyPart] ?: return@forEach
        val bodyPartKey = bodyPart.name
        val exerciseGroups = bodyPartEntries.groupBy { it.exerciseId }.values.toList()

        item(key = "bodypart-$bodyPartKey") {
            BodyPartGroupHeader(
                bodyPart = bodyPart,
                exerciseCount = exerciseGroups.size,
                expanded = expansion.isExpanded(bodyPartKey),
                onClick = { expansion.toggle(bodyPartKey) },
            )
        }
        if (!expansion.isExpanded(bodyPartKey)) return@forEach

        items(items = exerciseGroups, key = { "exercise-$bodyPartKey-${it.first().exerciseId}" }) { group ->
            val exerciseKey = exerciseKey(bodyPart, group.first().exerciseId)
            ExerciseGroupCard(
                entries = group,
                isEditable = isEditable,
                isPersonalRecord = group.first().exerciseId in personalRecordExerciseIds,
                expanded = expansion.isExpanded(exerciseKey),
                onClickHeader = { expansion.toggle(exerciseKey) },
                onClickEntry = onClickEntry,
                onClickDeleteEntry = onClickDeleteEntry,
                completedSets = completedSets,
                onToggleSet = onToggleSet,
            )
        }
    }
}

private fun exerciseKey(bodyPart: BodyPart, exerciseId: Long): String = "${bodyPart.name}/$exerciseId"

@Composable
private fun BodyPartGroupHeader(bodyPart: BodyPart, exerciseCount: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BaseText(
            text = bodyPart.label,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
        )
        BaseText(
            text = "종목 ${exerciseCount}개",
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        WeightSpacer()
        ExpandChevron(expanded = expanded)
    }
}

@Composable
private fun ExerciseGroupCard(
    entries: List<WorkoutEntry>,
    isEditable: Boolean,
    isPersonalRecord: Boolean,
    expanded: Boolean,
    onClickHeader: () -> Unit,
    onClickEntry: (Long) -> Unit,
    onClickDeleteEntry: (Long) -> Unit,
    completedSets: Set<WorkoutSetKey>?,
    onToggleSet: (WorkoutSetKey) -> Unit,
) {
    val first = entries.first()
    val setCount = entries.sumOf { it.sets.size }
    BodyPlanCard(contentPadding = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClickHeader)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BaseText(
                text = first.exerciseName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            if (isPersonalRecord) {
                Badge(text = "PR", modifier = Modifier.padding(start = 8.dp))
            }
            BaseText(
                text = "세트 ${setCount}개",
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            ExpandChevron(expanded = expanded)
        }
        if (!expanded) return@BodyPlanCard

        HorizontalDivider(color = Border)
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    VerticalSpacer(space = 12.dp)
                    HorizontalDivider(color = Border)
                    VerticalSpacer(space = 12.dp)
                }
                EntrySets(
                    entry = entry,
                    isEditable = isEditable,
                    onClickEdit = { onClickEntry(entry.id) },
                    onClickDelete = { onClickDeleteEntry(entry.id) },
                    completedSets = completedSets,
                    onToggleSet = onToggleSet,
                )
            }
        }
    }
}

@Composable
private fun EntrySets(
    entry: WorkoutEntry,
    isEditable: Boolean,
    onClickEdit: () -> Unit,
    onClickDelete: () -> Unit,
    completedSets: Set<WorkoutSetKey>?,
    onToggleSet: (WorkoutSetKey) -> Unit,
) {
    Column {
        entry.sets.forEachIndexed { index, set ->
            if (index > 0) VerticalSpacer(space = 8.dp)
            val key = WorkoutSetKey(entryId = entry.id, setIndex = index)
            SetRow(
                setNumber = index + 1,
                set = set,
                checked = completedSets?.let { key in it },
                onToggle = { onToggleSet(key) },
            )
        }
        if (isEditable) {
            VerticalSpacer(space = 12.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
            ) {
                BaseText(
                    text = "수정",
                    modifier = Modifier.clickable(onClick = onClickEdit),
                    style = MaterialTheme.typography.bodySmall,
                    color = Accent,
                )
                BaseText(
                    text = "삭제",
                    modifier = Modifier.clickable(onClick = onClickDelete),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
        }
    }
}

/** [checked]가 null이면 세션이 아니다. 세션 중에는 줄 어디를 눌러도 토글된다 — 체크 칸만 눌리면 손이 미끄러진다. */
@Composable
private fun SetRow(setNumber: Int, set: WorkoutSet, checked: Boolean?, onToggle: () -> Unit) {
    Row(
        modifier = if (checked == null) Modifier else Modifier.fillMaxWidth().clickable(onClick = onToggle),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (checked != null) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(24.dp),
                colors = CheckboxDefaults.colors(checkedColor = Accent),
            )
        }
        BaseText(
            text = "${setNumber}세트",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        BaseText(
            text = set.summaryText(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (checked == true) TextTertiary else TextPrimary,
        )
    }
}

/** 오른쪽 화살표 하나를 돌려 쓴다 — 펼치면 아래를 본다. */
@Composable
private fun ExpandChevron(expanded: Boolean) {
    BodyPlanIcon(
        painter = BodyPlanIcons.ChevronRight,
        contentDescription = if (expanded) "접기" else "펼치기",
        boxSize = 24.dp,
        iconSize = 16.dp,
        tint = TextTertiary,
        modifier = Modifier.rotate(if (expanded) 90f else 0f),
    )
}

/** 세트 한 줄의 표기. 시간으로 재는 종목은 한 세트가 한 회차라 횟수를 적지 않는다. */
private fun WorkoutSet.summaryText(): String = when (intensity) {
    is Intensity.Weight -> "${intensity.value}kg × ${repeatCount}회"
    is Intensity.Angle -> "${intensity.value}도 × ${repeatCount}회"
    is Intensity.Duration -> "${intensity.value}분"
}

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
        exerciseId = 1L,
        exerciseName = "인클라인 벤치프레스 머신",
        bodyPart = BodyPart.CHEST,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(20))),
    ),
    WorkoutEntry(
        id = 3L,
        exerciseId = 5L,
        exerciseName = "사이드 레터럴 레이즈 머신",
        bodyPart = BodyPart.SHOULDER,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(5))),
    ),
)

@Preview(showBackground = true)
@Composable
private fun WorkoutEntryGroupsPreview() {
    BodyPlanTheme {
        val expansion = rememberWorkoutEntryGroupExpansion()
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            workoutEntryGroups(
                entries = previewEntries,
                isEditable = true,
                expansion = expansion,
                onClickEntry = {},
                onClickDeleteEntry = {},
                personalRecordExerciseIds = setOf(1L),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutEntryGroupsExpandedPreview() {
    BodyPlanTheme {
        val expansion = rememberSaveable(saver = ExpansionSaver) {
            WorkoutEntryGroupExpansion(setOf(BodyPart.CHEST.name, exerciseKey(BodyPart.CHEST, 1L)))
        }
        val completedSets = setOf(WorkoutSetKey(entryId = 1L, setIndex = 0))
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            workoutEntryGroups(
                entries = previewEntries,
                isEditable = false,
                expansion = expansion,
                onClickEntry = {},
                onClickDeleteEntry = {},
                completedSets = completedSets,
            )
        }
    }
}
