package kr.sdbk.bodyplan.feature.my.impl.home.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.Badge
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.HorizontalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.Gender
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.feature.my.impl.profile.composable.label

/**
 * 마이 탭 맨 위의 내 정보 요약.
 *
 * 카드 전체가 수정으로 들어가는 자리다. 수정 글자만 눌리면 누르기 어렵다.
 */
@Composable
internal fun ProfileSummaryCard(
    profile: UserProfile,
    fromInbody: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyPlanCard(modifier = modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BaseText(
                text = "내 정보",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            WeightSpacer()
            // 자동으로 바뀐 값이 어디서 왔는지 알려 준다. 말없이 바뀌면 손수 넣은 값이
            // 사라진 것처럼 보인다.
            if (fromInbody) {
                BaseText(
                    text = "최근 인바디로 갱신됨",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
                HorizontalSpacer(space = 8.dp)
            }
            BaseText(
                text = "수정",
                style = MaterialTheme.typography.bodySmall,
                color = Accent,
            )
        }
        VerticalSpacer(space = 12.dp)

        if (profile.isEmpty) {
            BaseText(
                text = "아직 비어 있어요. 채우면 분석이 더 정확해집니다.",
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary,
            )
            return@BodyPlanCard
        }

        val body = profile.bodyLine()
        if (body != null) {
            BaseText(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        if (profile.goals.isNotEmpty()) {
            VerticalSpacer(space = 10.dp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                profile.goals.forEach { Badge(text = it.label) }
            }
        }
        val target = profile.targetLine()
        if (target != null) {
            VerticalSpacer(space = 10.dp)
            BaseText(
                text = target,
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary,
            )
        }
    }
}

/** 채워진 것만 골라 한 줄로 잇는다. 비어 있는 칸은 자리를 차지하지 않는다. */
private fun UserProfile.bodyLine(): String? = listOfNotNull(
    gender?.label,
    ageYears?.let { "${it}세" },
    heightCm?.let { "${it}cm" },
    weightKg?.let { "${it}kg" },
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

private fun UserProfile.targetLine(): String? = listOfNotNull(
    targetWeightKg?.let { "목표 ${it}kg" },
    targetNote?.takeIf { it.isNotBlank() },
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun ProfileSummaryCardPreview() {
    BodyPlanTheme {
        ProfileSummaryCard(
            profile = UserProfile(
                ageYears = 30,
                heightCm = 175,
                weightKg = 72,
                gender = Gender.MALE,
                goals = setOf(Goal.DIET, Goal.MUSCLE_GAIN),
                targetWeightKg = 68,
            ),
            fromInbody = true,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun ProfileSummaryCardEmptyPreview() {
    BodyPlanTheme {
        ProfileSummaryCard(profile = UserProfile(), fromInbody = false, onClick = {})
    }
}
