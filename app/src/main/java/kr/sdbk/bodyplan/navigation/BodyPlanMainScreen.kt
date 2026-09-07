package kr.sdbk.bodyplan.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
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
private fun MainBottomBar(currentTab: MainTab, onSelectTab: (MainTab) -> Unit) {
    NavigationBar {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == currentTab,
                onClick = { onSelectTab(tab) },
                icon = { BaseText(text = tab.label) },
            )
        }
    }
}

/** 하단 탭. 각 탭은 그 일지의 캘린더를 백스택의 뿌리로 세운다. */
private enum class MainTab(val label: String, val navKey: NavKey) {
    WORKOUT("운동 일지", WorkoutCalendarNavKey),
    DIET("식단 일지", DietCalendarNavKey),
}
