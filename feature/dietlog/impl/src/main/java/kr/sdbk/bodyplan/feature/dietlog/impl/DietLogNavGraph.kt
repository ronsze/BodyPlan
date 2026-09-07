package kr.sdbk.bodyplan.feature.dietlog.impl

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kr.sdbk.bodyplan.core.navigation.BodyPlanEntryProviderScope
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
import kr.sdbk.bodyplan.feature.dietlog.api.DietCalendarNavKey
import kr.sdbk.bodyplan.feature.dietlog.api.DietEntryEditNavKey
import kr.sdbk.bodyplan.feature.dietlog.api.DietLogNavKey
import kr.sdbk.bodyplan.feature.dietlog.api.navigateToDietEntryEdit
import kr.sdbk.bodyplan.feature.dietlog.api.navigateToDietLog
import kr.sdbk.bodyplan.feature.dietlog.impl.calendar.composable.DietCalendarEvents
import kr.sdbk.bodyplan.feature.dietlog.impl.calendar.composable.DietCalendarView
import kr.sdbk.bodyplan.feature.dietlog.impl.entryedit.DietEntryEditViewModel
import kr.sdbk.bodyplan.feature.dietlog.impl.entryedit.composable.DietEntryEditEvents
import kr.sdbk.bodyplan.feature.dietlog.impl.entryedit.composable.DietEntryEditView
import kr.sdbk.bodyplan.feature.dietlog.impl.log.DietLogViewModel
import kr.sdbk.bodyplan.feature.dietlog.impl.log.composable.DietLogEvents
import kr.sdbk.bodyplan.feature.dietlog.impl.log.composable.DietLogView

fun BodyPlanEntryProviderScope.dietLogNavGraph(navigator: BodyPlanNavigator) {
    entry<DietCalendarNavKey> {
        val events = remember { DietCalendarEvents(goToLog = navigator::navigateToDietLog) }
        DietCalendarView(events = events, viewModel = hiltViewModel())
    }

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
