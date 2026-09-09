package kr.sdbk.bodyplan.feature.home.impl

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kr.sdbk.bodyplan.core.navigation.BodyPlanEntryProviderScope
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
import kr.sdbk.bodyplan.feature.home.api.HomeNavKey
import kr.sdbk.bodyplan.feature.home.impl.home.composable.HomeView

/**
 * 홈은 아무 화면으로도 들어가지 않아 [navigator]를 쓰지 않는다.
 * 다른 feature와 같은 모양을 지키려고 인자는 그대로 받는다.
 */
@Suppress("UnusedParameter")
fun BodyPlanEntryProviderScope.homeNavGraph(navigator: BodyPlanNavigator) {
    entry<HomeNavKey> {
        HomeView(viewModel = hiltViewModel())
    }
}
