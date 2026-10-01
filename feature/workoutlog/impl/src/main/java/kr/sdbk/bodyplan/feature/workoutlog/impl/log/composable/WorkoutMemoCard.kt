package kr.sdbk.bodyplan.feature.workoutlog.impl.log.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.Badge
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BaseTextField
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.PillChip
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Condition
import kr.sdbk.bodyplan.core.domain.model.Effort
import kr.sdbk.bodyplan.core.domain.model.WorkoutMemo
import kr.sdbk.bodyplan.core.ui.components.label

/**
 * 그날 하나만 남기는 소감. 컨디션·체감 강도·통증 부위는 칩을 눌러 바로 남기고, 글은 덧붙이고 싶을 때만 쓴다.
 *
 * 칩은 [isEditable]인 날짜(오늘·어제)에서 누르는 즉시 저장된다. 글은 편집을 열어 저장을 눌러야 확정된다 —
 * 글은 길게 쓰다 실패하면 잃는 것이 커서 자동 저장하지 않는다. 조회 전용 날짜는 고른 값만 읽기로 보인다.
 */
@Composable
internal fun WorkoutMemoCard(
    memo: WorkoutMemo?,
    input: String,
    isEditable: Boolean,
    isEditing: Boolean,
    isSaving: Boolean,
    onSelectCondition: (Condition) -> Unit,
    onSelectEffort: (Effort) -> Unit,
    onTogglePainPart: (BodyPart) -> Unit,
    onClickEdit: () -> Unit,
    onChangeInput: (String) -> Unit,
    onClickSave: () -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyPlanCard(modifier = modifier) {
        BaseText(text = "오늘의 소감", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        VerticalSpacer(space = 14.dp)

        if (isEditable) {
            ChipGroup(label = "컨디션") {
                Condition.entries.forEach {
                    PillChip(text = it.label, selected = memo?.condition == it, onClick = { onSelectCondition(it) })
                }
            }
            VerticalSpacer(space = 12.dp)
            ChipGroup(label = "체감 강도") {
                Effort.entries.forEach {
                    PillChip(text = it.label, selected = memo?.effort == it, onClick = { onSelectEffort(it) })
                }
            }
            VerticalSpacer(space = 12.dp)
            ChipGroup(label = "아픈 부위") {
                PAIN_SELECTABLE_PARTS.forEach {
                    PillChip(
                        text = it.label,
                        selected = it in memo?.painParts.orEmpty(),
                        onClick = { onTogglePainPart(it) },
                    )
                }
            }
        } else {
            ReadOnlySummary(memo)
        }

        VerticalSpacer(space = 16.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BaseText(text = "메모", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            WeightSpacer()
            if (isEditable && !isEditing) {
                BaseText(
                    text = if (memo?.text == null) "추가" else "편집",
                    modifier = Modifier.clickable(onClick = onClickEdit),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
        }
        VerticalSpacer(space = 10.dp)
        if (isEditing) {
            MemoEditContent(
                input = input,
                isSaving = isSaving,
                onChangeInput = onChangeInput,
                onClickSave = onClickSave,
                onClickCancel = onClickCancel,
            )
        } else {
            MemoText(memo = memo?.text, isEditable = isEditable)
        }
    }
}

@Composable
private fun ChipGroup(label: String, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BaseText(text = label, style = MaterialTheme.typography.labelMedium, color = TextTertiary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { chips() }
    }
}

/** 조회 전용 날짜. 고른 것만 배지로 늘어놓고, 아무것도 없으면 한 줄로 알린다. */
@Composable
private fun ReadOnlySummary(memo: WorkoutMemo?) {
    val badges = listOfNotNull(
        memo?.condition?.let { "컨디션 ${it.label}" },
        memo?.effort?.let { "강도 ${it.label}" },
    ) + memo?.painParts.orEmpty().sortedBy { it.ordinal }.map { "${it.label} 통증" }
    if (badges.isEmpty()) {
        BaseText(text = "남긴 소감이 없습니다", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { badges.forEach { Badge(text = it) } }
}

@Composable
private fun MemoText(memo: String?, isEditable: Boolean) {
    BaseText(
        text = memo ?: if (isEditable) "더 남기고 싶은 말이 있다면 적어보세요" else "메모가 없습니다",
        style = MaterialTheme.typography.bodyMedium,
        color = if (memo == null) TextTertiary else TextSecondary,
    )
}

@Composable
private fun MemoEditContent(
    input: String,
    isSaving: Boolean,
    onChangeInput: (String) -> Unit,
    onClickSave: () -> Unit,
    onClickCancel: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    BaseTextField(
        value = input,
        onValueChange = onChangeInput,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface)
            .border(BorderStroke(1.dp, Border), shape)
            .defaultMinSize(minHeight = 96.dp)
            .padding(16.dp),
        enabled = !isSaving,
        placeholder = "오늘 운동은 어땠나요?",
        textStyle = MaterialTheme.typography.bodyMedium,
    )
    VerticalSpacer(space = 12.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedActionButton(text = "취소", onClick = onClickCancel, modifier = Modifier.weight(1f))
        PrimaryButton(
            text = if (isSaving) "저장 중..." else "저장",
            onClick = onClickSave,
            modifier = Modifier.weight(1f),
            enabled = !isSaving,
        )
    }
}

/** 통증은 몸의 부위에만 의미가 있어 유산소는 고르게 하지 않는다. */
private val PAIN_SELECTABLE_PARTS = BodyPart.entries.filter { it != BodyPart.CARDIO }

private val Condition.label: String
    get() = when (this) {
        Condition.GOOD -> "좋음"
        Condition.NORMAL -> "보통"
        Condition.BAD -> "나쁨"
    }

private val Effort.label: String
    get() = when (this) {
        Effort.LIGHT -> "가벼움"
        Effort.MODERATE -> "적당"
        Effort.HARD -> "힘듦"
    }

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun WorkoutMemoCardPreview() {
    BodyPlanTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WorkoutMemoCard(
                memo = WorkoutMemo(
                    condition = Condition.GOOD,
                    effort = Effort.HARD,
                    painParts = setOf(BodyPart.SHOULDER),
                    text = "어깨가 뻐근해서 무게를 내렸다",
                ),
                input = "",
                isEditable = true,
                isEditing = false,
                isSaving = false,
                onSelectCondition = {},
                onSelectEffort = {},
                onTogglePainPart = {},
                onClickEdit = {},
                onChangeInput = {},
                onClickSave = {},
                onClickCancel = {},
            )
            WorkoutMemoCard(
                memo = WorkoutMemo(condition = Condition.BAD, painParts = setOf(BodyPart.LEG)),
                input = "",
                isEditable = false,
                isEditing = false,
                isSaving = false,
                onSelectCondition = {},
                onSelectEffort = {},
                onTogglePainPart = {},
                onClickEdit = {},
                onChangeInput = {},
                onClickSave = {},
                onClickCancel = {},
            )
        }
    }
}
