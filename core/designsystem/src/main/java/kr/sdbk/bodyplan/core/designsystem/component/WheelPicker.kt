package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary

private val ITEM_HEIGHT = 36.dp
private const val VISIBLE_ITEMS = 5

/**
 * 정해진 값 중 하나를 굴려서 고르는 목록.
 *
 * 가운데 칸이 고른 값이고 위아래로 두 칸씩 미리 보인다. 손을 떼면 가장 가까운 칸에 붙는다.
 * 값이 눈금 위에만 놓이므로 호출부가 임의 숫자를 만들 수 없다.
 */
@Composable
fun WheelPicker(
    label: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String = "",
) {
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)

    // 첫 칸이 곧 가운데 칸이다. 위아래 여백이 두 칸씩 잡혀 있기 때문이다.
    val centeredIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    LaunchedEffect(listState, options) {
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .collect { (scrolling, index) ->
                val option = options.getOrNull(index) ?: return@collect
                if (!scrolling) currentOnSelect(option)
            }
    }

    LaunchedEffect(selected) {
        val target = options.indexOf(selected)
        if (target >= 0 && target != listState.firstVisibleItemIndex && !listState.isScrollInProgress) {
            listState.scrollToItem(target)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Background)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BaseText(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            ),
            color = TextSecondary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ITEM_HEIGHT * VISIBLE_ITEMS),
            contentAlignment = Alignment.Center,
        ) {
            SelectionMarker()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                flingBehavior = rememberSnapFlingBehavior(listState),
                contentPadding = PaddingValues(vertical = ITEM_HEIGHT * (VISIBLE_ITEMS / 2)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(items = options, key = { _, option -> option }) { index, option ->
                    val isCentered = index == centeredIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ITEM_HEIGHT),
                        contentAlignment = Alignment.Center,
                    ) {
                        BaseText(
                            text = if (suffix.isEmpty()) "$option" else "$option $suffix",
                            style = if (isCentered) {
                                MaterialTheme.typography.bodyLarge
                            } else {
                                MaterialTheme.typography.labelMedium
                            },
                            color = if (isCentered) Accent else TextTertiary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** 가운데 칸을 알려주는 위아래 선. 목록보다 뒤에 그려 글자를 가리지 않는다. */
@Composable
private fun SelectionMarker() {
    Column(
        modifier = Modifier.height(ITEM_HEIGHT),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Accent)
                    .fillMaxWidth(0.5f),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun WheelPickerPreview() {
    BodyPlanTheme {
        WheelPicker(
            label = "무게",
            options = (5..100 step 5).toList(),
            selected = 10,
            onSelect = {},
            modifier = Modifier.padding(16.dp),
            suffix = "kg",
        )
    }
}
