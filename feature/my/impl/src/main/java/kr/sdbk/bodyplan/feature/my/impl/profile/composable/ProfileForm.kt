package kr.sdbk.bodyplan.feature.my.impl.profile.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BaseTextField
import kr.sdbk.bodyplan.core.designsystem.component.PillChip
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.domain.model.Gender
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.feature.my.impl.profile.ProfileInput

internal val Gender.label: String
    get() = when (this) {
        Gender.MALE -> "남성"
        Gender.FEMALE -> "여성"
    }

internal val Goal.label: String
    get() = when (this) {
        Goal.DIET -> "다이어트"
        Goal.MUSCLE_GAIN -> "근성장"
        Goal.TARGET_WEIGHT -> "목표 체중"
        Goal.TARGET_STRENGTH -> "목표 근력"
    }

/**
 * 온보딩과 프로필 수정이 함께 쓰는 입력 묶음.
 *
 * 모든 칸을 비워 둘 수 있다. 고른 것을 다시 누르면 선택이 풀린다.
 */
@Composable
internal fun ProfileForm(input: ProfileInput, onChange: (ProfileInput) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        NumberField("나이", input.ageYears, "세") { onChange(input.copy(ageYears = it)) }
        NumberField("키", input.heightCm, "cm") { onChange(input.copy(heightCm = it)) }
        NumberField("몸무게", input.weightKg, "kg") { onChange(input.copy(weightKg = it)) }

        FieldLabel("성별")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Gender.entries.forEach { gender ->
                PillChip(
                    text = gender.label,
                    selected = input.gender == gender,
                    // 다시 누르면 고르지 않은 상태로 돌아간다.
                    onClick = {
                        onChange(input.copy(gender = if (input.gender == gender) null else gender))
                    },
                )
            }
        }

        FieldLabel("목적")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Goal.entries.forEach { goal ->
                PillChip(
                    text = goal.label,
                    selected = goal in input.goals,
                    onClick = {
                        val goals = if (goal in input.goals) input.goals - goal else input.goals + goal
                        onChange(input.copy(goals = goals))
                    },
                )
            }
        }

        NumberField("목표 체중", input.targetWeightKg, "kg") {
            onChange(input.copy(targetWeightKg = it))
        }

        FieldLabel("그 밖의 목표")
        InputBox {
            BaseTextField(
                value = input.targetNote,
                onValueChange = { onChange(input.copy(targetNote = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "목표 근력처럼 숫자로 적기 어려운 것",
                textStyle = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, suffix: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        InputBox {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BaseTextField(
                    value = value,
                    // 숫자만 남긴다. 붙여 넣기로 들어온 글자도 여기서 걸린다.
                    onValueChange = { onChange(it.filter(Char::isDigit)) },
                    modifier = Modifier.weight(1f),
                    placeholder = "비워 둘 수 있어요",
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                BaseText(
                    text = suffix,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    BaseText(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
    )
}

@Composable
private fun InputBox(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface)
            .border(BorderStroke(1.dp, Border), shape)
            .padding(16.dp),
    ) {
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6, heightDp = 900)
@Composable
private fun ProfileFormPreview() {
    BodyPlanTheme {
        ProfileForm(
            input = ProfileInput(
                ageYears = "30",
                heightCm = "175",
                gender = Gender.MALE,
                goals = setOf(Goal.DIET, Goal.MUSCLE_GAIN),
            ),
            onChange = {},
        )
    }
}
