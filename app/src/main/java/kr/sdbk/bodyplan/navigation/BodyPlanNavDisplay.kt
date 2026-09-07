package kr.sdbk.bodyplan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
import kr.sdbk.bodyplan.feature.dietlog.impl.dietLogNavGraph
import kr.sdbk.bodyplan.feature.home.api.HomeNavKey
import kr.sdbk.bodyplan.feature.home.impl.homeNavGraph
import kr.sdbk.bodyplan.feature.workoutlog.impl.workoutLogNavGraph

@Composable
fun BodyPlanNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(HomeNavKey)
    val navigator = remember(backStack) { BodyPlanNavigator(backStack) }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { navigator.goBack() },
        // 기본값에는 ViewModel 저장소 데코레이터가 없다. 없으면 모든 화면의 ViewModel이
        // 액티비티에 붙어, 화면을 떠나도 살아남고 같은 화면을 다시 열면 이전 상태가 그대로 돌아온다.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider =
            entryProvider {
                homeNavGraph(navigator)
                workoutLogNavGraph(navigator)
                dietLogNavGraph(navigator)
            },
    )
}
