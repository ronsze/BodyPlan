package kr.sdbk.bodyplan.feature.my.impl.home.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale
import kr.sdbk.bodyplan.core.designsystem.component.Badge
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.AccentSurface
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.SurfaceMuted
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.Gender
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.feature.my.impl.profile.composable.label

/**
 * 마이 탭 맨 위의 프로필 카드.
 *
 * 수정으로 들어가는 자리는 [onClickEdit]을 건 버튼뿐이다 — 카드 전체를 누르게 하면 스크롤하다 잘못 닿아 화면이 넘어간다.
 * 체지방률·골격근량은 프로필이 아니라 최근 [inbody]에서 온 값이다.
 */
@Composable
internal fun ProfileSummaryCard(
    profile: UserProfile,
    fromInbody: Boolean,
    inbody: InbodyMeasurement?,
    onClickEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyPlanCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(gender = profile.gender)
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                BaseText(
                    text = "내 정보",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                BaseText(
                    text = profile.bodyLine() ?: "아직 비어 있어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
            EditButton(onClick = onClickEdit)
        }
        VerticalSpacer(space = 16.dp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(label = "키", value = profile.heightCm?.let { "${it}cm" })
            StatTile(label = "체중", value = profile.weightKg?.let { "${it}kg" })
        }
        VerticalSpacer(space = 8.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(label = "체지방률", value = inbody?.bodyFatPercent?.let { format("%.1f%%", it) })
            StatTile(label = "골격근량", value = inbody?.skeletalMuscleKg?.let { format("%.1fkg", it) })
        }

        // 자동으로 바뀐 값이 어디서 왔는지 알려 준다. 말없이 바뀌면 손수 넣은 값이 사라진 것처럼 보인다.
        if (fromInbody) {
            VerticalSpacer(space = 10.dp)
            BaseText(
                text = "키·체중은 최근 인바디로 갱신됐어요",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
        if (profile.goals.isNotEmpty()) {
            VerticalSpacer(space = 14.dp)
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

@Composable
private fun Avatar(gender: Gender?) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(AccentSurface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BaseText(
            text = gender?.label?.take(1) ?: "나",
            style = MaterialTheme.typography.titleMedium,
            color = Accent,
        )
    }
}

@Composable
private fun EditButton(onClick: () -> Unit) {
    BaseText(
        text = "수정",
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AccentSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        style = MaterialTheme.typography.labelLarge,
        color = Accent,
    )
}

@Composable
private fun RowScope.StatTile(label: String, value: String?) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceMuted)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BaseText(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary,
        )
        VerticalSpacer(space = 2.dp)
        BaseText(
            text = value ?: "-",
            style = MaterialTheme.typography.titleSmall,
            color = if (value == null) TextTertiary else TextPrimary,
        )
    }
}

/** 이름이 아직 없어 성별·나이로 소개 줄을 만든다. 채워진 것만 잇는다. */
private fun UserProfile.bodyLine(): String? = listOfNotNull(
    gender?.label,
    ageYears?.let { "${it}세" },
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

private fun UserProfile.targetLine(): String? = listOfNotNull(
    targetWeightKg?.let { "목표 ${it}kg" },
    targetNote?.takeIf { it.isNotBlank() },
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

private fun format(pattern: String, value: Double): String = String.format(Locale.US, pattern, value)

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
            inbody = InbodyMeasurement(weightKg = 72.0, skeletalMuscleKg = 33.4, bodyFatKg = 14.2),
            onClickEdit = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun ProfileSummaryCardEmptyPreview() {
    BodyPlanTheme {
        ProfileSummaryCard(
            profile = UserProfile(),
            fromInbody = false,
            inbody = null,
            onClickEdit = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
