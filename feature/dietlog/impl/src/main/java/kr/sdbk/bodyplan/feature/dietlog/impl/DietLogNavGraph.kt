package kr.sdbk.bodyplan.feature.dietlog.impl

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kr.sdbk.bodyplan.core.navigation.BodyPlanEntryProviderScope
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
import kr.sdbk.bodyplan.feature.dietlog.api.DietAnalysisNavKey
import kr.sdbk.bodyplan.feature.dietlog.api.DietCalendarNavKey
import kr.sdbk.bodyplan.feature.dietlog.api.DietEntryEditNavKey
import kr.sdbk.bodyplan.feature.dietlog.api.DietLogNavKey
import kr.sdbk.bodyplan.feature.dietlog.api.navigateToDietAnalysis
import kr.sdbk.bodyplan.feature.dietlog.api.navigateToDietEntryEdit
import kr.sdbk.bodyplan.feature.dietlog.api.navigateToDietLog
import kr.sdbk.bodyplan.feature.dietlog.impl.analysis.DietAnalysisViewModel
import kr.sdbk.bodyplan.feature.dietlog.impl.analysis.composable.DietAnalysisEvents
import kr.sdbk.bodyplan.feature.dietlog.impl.analysis.composable.DietAnalysisView
import kr.sdbk.bodyplan.feature.dietlog.impl.calendar.composable.DietCalendarEvents
import kr.sdbk.bodyplan.feature.dietlog.impl.calendar.composable.DietCalendarView
import kr.sdbk.bodyplan.feature.dietlog.impl.entryedit.DietEntryEditViewModel
import kr.sdbk.bodyplan.feature.dietlog.impl.entryedit.composable.DietEntryEditEvents
import kr.sdbk.bodyplan.feature.dietlog.impl.entryedit.composable.DietEntryEditView
import kr.sdbk.bodyplan.feature.dietlog.impl.log.DietLogViewModel
import kr.sdbk.bodyplan.feature.dietlog.impl.log.composable.DietLogEvents
import kr.sdbk.bodyplan.feature.dietlog.impl.log.composable.DietLogView
import kr.sdbk.bodyplan.feature.my.api.navigateToAiToken

fun BodyPlanEntryProviderScope.dietLogNavGraph(navigator: BodyPlanNavigator) {
    entry<DietCalendarNavKey> {
        val events = remember {
            DietCalendarEvents(
                goToLog = navigator::navigateToDietLog,
                goToAnalysis = navigator::navigateToDietAnalysis,
            )
        }
        DietCalendarView(events = events, viewModel = hiltViewModel())
    }

    entry<DietLogNavKey> { navKey ->
        val events = remember {
            DietLogEvents(
                goBack = navigator::goBack,
                goToEntryEdit = navigator::navigateToDietEntryEdit,
                goToAnalysis = navigator::navigateToDietAnalysis,
            )
        }
        DietLogView(
            events = events,
            viewModel = hiltViewModel<DietLogViewModel, DietLogViewModel.Factory> {
                it.create(navKey)
            },
        )
    }

    entry<DietAnalysisNavKey> { navKey ->
        val events = remember {
            DietAnalysisEvents(
                goBack = navigator::goBack,
                goToAiToken = navigator::navigateToAiToken,
            )
        }
        DietAnalysisView(
            events = events,
            viewModel = hiltViewModel<DietAnalysisViewModel, DietAnalysisViewModel.Factory> {
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
