package kr.sdbk.bodyplan.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanIcon
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanIcons
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
import kr.sdbk.bodyplan.feature.dietlog.api.DietCalendarNavKey
import kr.sdbk.bodyplan.feature.dietlog.impl.dietLogNavGraph
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutCalendarNavKey
import kr.sdbk.bodyplan.feature.workoutlog.impl.workoutLogNavGraph

/**
 * 앱의 뼈대. 하단 탭으로 일지를 오가고, 그 아래로 들어간 화면에서는 탭을 감춘다.
 *
 * 탭 목록이 여기 있는 이유는 두 feature를 동시에 아는 곳이 `:app`뿐이기 때문이다.
 * feature는 서로의 `api`만 알고 화면은 각자의 navGraph가 등록한다.
 */
@Composable
fun BodyPlanMainScreen(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(MainTab.entries.first().navKey)
    val navigator = remember(backStack) { BodyPlanNavigator(backStack) }
    val currentTab = MainTab.entries.firstOrNull { it.navKey == backStack.lastOrNull() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Background,
        // 화면은 시스템 바 아래까지 그린다. 여백은 여기서 한 번만 잡아 화면마다 되풀이하지 않는다.
        // safeDrawing은 키보드까지 포함하므로 입력이 열리면 내용 영역이 그만큼 줄어든다.
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // 깊은 화면에서는 탭을 감춘다. 작성하다 다른 일지로 빠져나가는 일을 막는다.
            if (currentTab != null) {
                MainBottomBar(
                    currentTab = currentTab,
                    onSelectTab = { tab ->
                        if (tab != currentTab) {
                            backStack.clear()
                            backStack.add(tab.navKey)
                        }
                    },
                    // 탭 바는 Scaffold 바깥 끝에 놓이므로 아래 여백을 스스로 잡는다.
                    // 키보드는 피하지 않는다 — 탭 화면에는 입력이 없고, 피하면 키보드 위로 떠오른다.
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                    ),
                )
            }
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            onBack = { navigator.goBack() },
            // 기본값에는 ViewModel 저장소 데코레이터가 없다. 없으면 모든 화면의 ViewModel이
            // 액티비티에 붙어, 화면을 떠나도 살아남고 같은 화면을 다시 열면 이전 상태가 그대로 돌아온다.
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider =
                entryProvider {
                    workoutLogNavGraph(navigator)
                    dietLogNavGraph(navigator)
                },
        )
    }
}

@Composable
private fun MainBottomBar(currentTab: MainTab, onSelectTab: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(Surface)) {
        HorizontalDivider(color = Border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { tab ->
                MainTabItem(
                    tab = tab,
                    selected = tab == currentTab,
                    onClick = { onSelectTab(tab) },
                    // 탭이 바의 절반씩 차지한다. 아이콘과 글자만 눌리면 누르기 어렵다.
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MainTabItem(tab: MainTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint = if (selected) Accent else TextTertiary
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BodyPlanIcon(
            painter = tab.icon,
            contentDescription = null,
            boxSize = 24.dp,
            iconSize = 20.dp,
            tint = tint,
        )
        VerticalSpacer(space = 4.dp)
        BaseText(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = tint,
        )
    }
}

/** 하단 탭. 각 탭은 그 일지의 캘린더를 백스택의 뿌리로 세운다. */
private enum class MainTab(val label: String, val navKey: NavKey) {
    WORKOUT("운동 일지", WorkoutCalendarNavKey),
    DIET("식단 일지", DietCalendarNavKey),
    ;

    val icon: Painter
        @Composable get() = when (this) {
            WORKOUT -> BodyPlanIcons.Dumbbell
            DIET -> BodyPlanIcons.ForkKnifeCrossed
        }
}
