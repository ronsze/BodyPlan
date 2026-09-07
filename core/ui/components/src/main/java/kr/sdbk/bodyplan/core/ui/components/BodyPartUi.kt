package kr.sdbk.bodyplan.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.PillChip
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.PartBack
import kr.sdbk.bodyplan.core.designsystem.theme.PartBiceps
import kr.sdbk.bodyplan.core.designsystem.theme.PartCardio
import kr.sdbk.bodyplan.core.designsystem.theme.PartChest
import kr.sdbk.bodyplan.core.designsystem.theme.PartLeg
import kr.sdbk.bodyplan.core.designsystem.theme.PartShoulder
import kr.sdbk.bodyplan.core.designsystem.theme.PartTriceps
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.DayStatus

val BodyPart.label: String
    get() = when (this) {
        BodyPart.CHEST -> "가슴"
        BodyPart.BACK -> "등"
        BodyPart.SHOULDER -> "어깨"
        BodyPart.LEG -> "하체"
        BodyPart.BICEPS -> "이두"
        BodyPart.TRICEPS -> "삼두"
        BodyPart.CARDIO -> "유산소"
    }

val BodyPart.color: Color
    get() = when (this) {
        BodyPart.CHEST -> PartChest
        BodyPart.BACK -> PartBack
        BodyPart.SHOULDER -> PartShoulder
        BodyPart.LEG -> PartLeg
        BodyPart.BICEPS -> PartBiceps
        BodyPart.TRICEPS -> PartTriceps
        BodyPart.CARDIO -> PartCardio
    }

/**
 * 캘린더 셀의 날짜 아래에 붙는 표시. 높이를 고정해 표시가 있든 없든 칸이 흔들리지 않는다.
 *
 * 상태별 분기를 셀 호출부가 아니라 여기가 갖는다 — 기능마다 자기 표시를 셀 슬롯에 꽂는
 * 구조라, 분기가 밖으로 나가면 기능마다 복제된다.
 */
@Composable
fun DayStatusIndicator(status: DayStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(INDICATOR_HEIGHT),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (status) {
            is DayStatus.Recorded -> BodyPartDots(bodyParts = status.bodyParts)

            is DayStatus.Rest ->
                BaseText(
                    text = "휴식",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )

            is DayStatus.Pending, is DayStatus.Upcoming -> Unit
        }
    }
}

/** 그 날 수행한 부위를 색 점으로 늘어놓는다. 순서는 [BodyPart] 선언 순서로 고정한다. */
@Composable
private fun BodyPartDots(bodyParts: Set<BodyPart>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyPart.entries.filter { it in bodyParts }.forEach { bodyPart ->
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(bodyPart.color),
            )
        }
    }
}

/** 부위 여섯 개를 늘어놓고 하나를 고르게 한다. 고른 것이 없는 상태를 허용한다. */
@Composable
fun BodyPartTabRow(selected: BodyPart?, onSelect: (BodyPart) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(items = BodyPart.entries, key = { it.name }) { bodyPart ->
            PillChip(
                text = bodyPart.label,
                selected = bodyPart == selected,
                onClick = { onSelect(bodyPart) },
            )
        }
    }
}

private val INDICATOR_HEIGHT = 14.dp

@Preview(showBackground = true)
@Composable
private fun BodyPartTabRowPreview() {
    BodyPlanTheme {
        BodyPartTabRow(selected = BodyPart.CHEST, onSelect = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun DayStatusIndicatorPreview() {
    BodyPlanTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DayStatusIndicator(DayStatus.Recorded(setOf(BodyPart.CHEST, BodyPart.TRICEPS)))
            DayStatusIndicator(DayStatus.Rest)
            DayStatusIndicator(DayStatus.Pending)
        }
    }
}
