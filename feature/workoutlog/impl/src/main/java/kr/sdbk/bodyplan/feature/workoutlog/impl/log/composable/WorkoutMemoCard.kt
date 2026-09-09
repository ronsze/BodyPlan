package kr.sdbk.bodyplan.feature.workoutlog.impl.log.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BaseTextField
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary

/**
 * 그날 하나만 남기는 메모. 보기와 편집을 같은 카드 안에서 오간다.
 *
 * 편집은 [isEditable]인 날짜(오늘·어제)에서만 열 수 있고, 저장을 눌러야 확정된다 —
 * 자동 저장으로 두면 실패를 알릴 자리가 없다.
 */
@Composable
internal fun WorkoutMemoCard(
    memo: String?,
    input: String,
    isEditable: Boolean,
    isEditing: Boolean,
    isSaving: Boolean,
    onClickEdit: () -> Unit,
    onChangeInput: (String) -> Unit,
    onClickSave: () -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyPlanCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BaseText(text = "메모", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            WeightSpacer()
            if (isEditable && !isEditing) {
                BaseText(
                    text = if (memo == null) "추가" else "편집",
                    modifier = Modifier.clickable(onClick = onClickEdit),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
        }
        VerticalSpacer(space = 14.dp)
        if (isEditing) {
            MemoEditContent(
                input = input,
                isSaving = isSaving,
                onChangeInput = onChangeInput,
                onClickSave = onClickSave,
                onClickCancel = onClickCancel,
            )
        } else {
            MemoText(memo = memo, isEditable = isEditable)
        }
    }
}

@Composable
private fun MemoText(memo: String?, isEditable: Boolean) {
    BaseText(
        text = memo ?: if (isEditable) "메모를 남겨보세요" else "메모가 없습니다",
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

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun WorkoutMemoCardPreview() {
    BodyPlanTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WorkoutMemoCard(
                memo = "어깨가 뻐근해서 무게를 내렸다",
                input = "",
                isEditable = true,
                isEditing = false,
                isSaving = false,
                onClickEdit = {},
                onChangeInput = {},
                onClickSave = {},
                onClickCancel = {},
            )
            WorkoutMemoCard(
                memo = null,
                input = "",
                isEditable = true,
                isEditing = false,
                isSaving = false,
                onClickEdit = {},
                onChangeInput = {},
                onClickSave = {},
                onClickCancel = {},
            )
            WorkoutMemoCard(
                memo = null,
                input = "컨디션이 좋았다",
                isEditable = true,
                isEditing = true,
                isSaving = false,
                onClickEdit = {},
                onChangeInput = {},
                onClickSave = {},
                onClickCancel = {},
            )
            WorkoutMemoCard(
                memo = "지난주에 남긴 메모",
                input = "",
                isEditable = false,
                isEditing = false,
                isSaving = false,
                onClickEdit = {},
                onChangeInput = {},
                onClickSave = {},
                onClickCancel = {},
            )
        }
    }
}
