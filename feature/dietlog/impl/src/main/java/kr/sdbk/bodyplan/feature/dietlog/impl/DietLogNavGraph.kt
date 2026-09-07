package kr.sdbk.bodyplan.feature.dietlog.impl

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kr.sdbk.bodyplan.core.navigation.BodyPlanEntryProviderScope
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
import kr.sdbk.bodyplan.feature.dietlog.api.DietEntryEditNavKey
import kr.sdbk.bodyplan.feature.dietlog.api.DietLogNavKey
import kr.sdbk.bodyplan.feature.dietlog.api.navigateToDietEntryEdit

fun BodyPlanEntryProviderScope.dietLogNavGraph(navigator: BodyPlanNavigator) {
    entry<DietLogNavKey> { navKey ->
        val events = remember {
            DietLogEvents(
                goBack = navigator::goBack,
                goToEntryEdit = navigator::navigateToDietEntryEdit,
            )
        }
        DietLogView(
            events = events,
            viewModel = hiltViewModel<DietLogViewModel, DietLogViewModel.Factory> {
                it.create(navKey)
            },
        )
    }

    entry<DietEntryEditNavKey> { navKey ->
        val events = remember { DietEntryEditEvents(goBack = navigator::goBack) }
        DietEntryEditView(
            events = events,
            viewModel = hiltViewModel<DietEntryEditViewModel, DietEntryEditViewModel.Factory> {
                it.create(navKey)
            },
        )
    }
}
