package kr.sdbk.bodyplan.feature.workoutlog.impl

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kr.sdbk.bodyplan.core.navigation.BodyPlanEntryProviderScope
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutEntryEditNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutLogNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.navigateToWorkoutEntryEdit

fun BodyPlanEntryProviderScope.workoutLogNavGraph(navigator: BodyPlanNavigator) {
    entry<WorkoutLogNavKey> { navKey ->
        val events = remember {
            WorkoutLogEvents(
                goBack = navigator::goBack,
                goToEntryEdit = navigator::navigateToWorkoutEntryEdit,
            )
        }
        WorkoutLogView(
            events = events,
            viewModel = hiltViewModel<WorkoutLogViewModel, WorkoutLogViewModel.Factory> {
                it.create(navKey)
            },
        )
    }

    entry<WorkoutEntryEditNavKey> { navKey ->
        val events = remember { WorkoutEntryEditEvents(goBack = navigator::goBack) }
        WorkoutEntryEditView(
            events = events,
            viewModel = hiltViewModel<WorkoutEntryEditViewModel, WorkoutEntryEditViewModel.Factory> {
                it.create(navKey)
            },
        )
    }
}
