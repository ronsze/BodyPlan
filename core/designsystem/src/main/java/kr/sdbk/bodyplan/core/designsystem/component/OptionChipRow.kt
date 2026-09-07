package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme

/**
 * 정해진 숫자 중 하나를 고르는 가로 칩 줄.
 * 값이 정해진 눈금 위에만 놓이게 해, 호출부가 임의 숫자를 만들지 않도록 한다.
 */
@Composable
fun OptionChipRow(
    options: List<Int>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String = "",
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(items = options, key = { it }) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { BaseText(text = "$option$suffix") },
                shape = FilterChipDefaults.shape,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OptionChipRowPreview() {
    BodyPlanTheme {
        OptionChipRow(
            options = listOf(5, 10, 15, 20, 25),
            selected = 15,
            onSelect = {},
            suffix = "kg",
        )
    }
}
