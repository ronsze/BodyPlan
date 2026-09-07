package kr.sdbk.bodyplan.feature.my.impl

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kr.sdbk.bodyplan.core.navigation.BodyPlanEntryProviderScope
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
import kr.sdbk.bodyplan.feature.my.api.AiTokenNavKey
import kr.sdbk.bodyplan.feature.my.api.MyNavKey
import kr.sdbk.bodyplan.feature.my.api.navigateToAiToken
import kr.sdbk.bodyplan.feature.my.impl.aitoken.composable.AiTokenEvents
import kr.sdbk.bodyplan.feature.my.impl.aitoken.composable.AiTokenView
import kr.sdbk.bodyplan.feature.my.impl.home.composable.MyEvents
import kr.sdbk.bodyplan.feature.my.impl.home.composable.MyView

fun BodyPlanEntryProviderScope.myNavGraph(navigator: BodyPlanNavigator) {
    entry<MyNavKey> {
        val events = remember { MyEvents(goToAiToken = navigator::navigateToAiToken) }
        MyView(events = events, viewModel = hiltViewModel())
    }

    entry<AiTokenNavKey> {
        val events = remember { AiTokenEvents(goBack = navigator::goBack) }
        AiTokenView(events = events, viewModel = hiltViewModel())
    }
}
