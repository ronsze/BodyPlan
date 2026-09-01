package kr.sdbk.bodyplan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kr.sdbk.bodyplan.feature.home.HomeRoute

@Composable
fun BodyPlanNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Home)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Home> { HomeRoute() }
            },
    )
}
