package kr.sdbk.bodyplan.feature.dietlog.impl

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kr.sdbk.bodyplan.core.navigation.BodyPlanEntryProviderScope
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
import kr.sdbk.bodyplan.feature.dietlog.api.DietLogNavKey

fun BodyPlanEntryProviderScope.dietLogNavGraph(navigator: BodyPlanNavigator) {
    entry<DietLogNavKey> {
        val events = remember { DietLogEvents(goBack = navigator::goBack) }
        DietLogView(events = events, viewModel = hiltViewModel())
    }
}
