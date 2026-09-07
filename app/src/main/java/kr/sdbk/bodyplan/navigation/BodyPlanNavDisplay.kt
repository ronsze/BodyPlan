package kr.sdbk.bodyplan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
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
        entryProvider =
            entryProvider {
                homeNavGraph(navigator)
                workoutLogNavGraph(navigator)
            },
    )
}
