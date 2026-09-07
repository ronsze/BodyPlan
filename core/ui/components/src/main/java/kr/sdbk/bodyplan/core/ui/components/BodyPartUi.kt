package kr.sdbk.bodyplan.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.PartBack
import kr.sdbk.bodyplan.core.designsystem.theme.PartBiceps
import kr.sdbk.bodyplan.core.designsystem.theme.PartChest
import kr.sdbk.bodyplan.core.designsystem.theme.PartLeg
import kr.sdbk.bodyplan.core.designsystem.theme.PartShoulder
import kr.sdbk.bodyplan.core.designsystem.theme.PartTriceps
import kr.sdbk.bodyplan.core.domain.model.BodyPart

val BodyPart.label: String
    get() = when (this) {
        BodyPart.CHEST -> "가슴"
        BodyPart.BACK -> "등"
        BodyPart.SHOULDER -> "어깨"
        BodyPart.LEG -> "하체"
        BodyPart.BICEPS -> "이두"
        BodyPart.TRICEPS -> "삼두"
    }

val BodyPart.color: Color
    get() = when (this) {
        BodyPart.CHEST -> PartChest
        BodyPart.BACK -> PartBack
        BodyPart.SHOULDER -> PartShoulder
        BodyPart.LEG -> PartLeg
        BodyPart.BICEPS -> PartBiceps
        BodyPart.TRICEPS -> PartTriceps
    }

/** 부위 여섯 개를 늘어놓고 하나를 고르게 한다. 고른 것이 없는 상태를 허용한다. */
@Composable
fun BodyPartTabRow(selected: BodyPart?, onSelect: (BodyPart) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items = BodyPart.entries, key = { it.name }) { bodyPart ->
            FilterChip(
                selected = bodyPart == selected,
                onClick = { onSelect(bodyPart) },
                label = { BaseText(text = bodyPart.label) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BodyPartTabRowPreview() {
    BodyPlanTheme {
        BodyPartTabRow(selected = BodyPart.CHEST, onSelect = {})
    }
}
