package kr.sdbk.bodyplan.feature.workoutlog.impl

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kr.sdbk.bodyplan.core.navigation.BodyPlanEntryProviderScope
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
import kr.sdbk.bodyplan.feature.my.api.navigateToAiToken
import kr.sdbk.bodyplan.feature.workoutlog.api.ExerciseManageNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.ExerciseTrendNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutAnalysisNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutCalendarNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutEntryEditNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutLogNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.navigateToExerciseManage
import kr.sdbk.bodyplan.feature.workoutlog.api.navigateToExerciseTrend
import kr.sdbk.bodyplan.feature.workoutlog.api.navigateToWorkoutAnalysis
import kr.sdbk.bodyplan.feature.workoutlog.api.navigateToWorkoutEntryEdit
import kr.sdbk.bodyplan.feature.workoutlog.api.navigateToWorkoutLog
import kr.sdbk.bodyplan.feature.workoutlog.impl.analysis.WorkoutAnalysisViewModel
import kr.sdbk.bodyplan.feature.workoutlog.impl.analysis.composable.WorkoutAnalysisEvents
import kr.sdbk.bodyplan.feature.workoutlog.impl.analysis.composable.WorkoutAnalysisView
import kr.sdbk.bodyplan.feature.workoutlog.impl.calendar.composable.WorkoutCalendarEvents
import kr.sdbk.bodyplan.feature.workoutlog.impl.calendar.composable.WorkoutCalendarView
import kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit.WorkoutEntryEditViewModel
import kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit.composable.WorkoutEntryEditEvents
import kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit.composable.WorkoutEntryEditView
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisemanage.composable.ExerciseManageEvents
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisemanage.composable.ExerciseManageView
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend.composable.ExerciseTrendEvents
import kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend.composable.ExerciseTrendView
import kr.sdbk.bodyplan.feature.workoutlog.impl.log.WorkoutLogViewModel
import kr.sdbk.bodyplan.feature.workoutlog.impl.log.composable.WorkoutLogEvents
import kr.sdbk.bodyplan.feature.workoutlog.impl.log.composable.WorkoutLogView

fun BodyPlanEntryProviderScope.workoutLogNavGraph(navigator: BodyPlanNavigator) {
    entry<WorkoutCalendarNavKey> {
        val events = remember {
            WorkoutCalendarEvents(
                goToLog = navigator::navigateToWorkoutLog,
                goToExerciseManage = navigator::navigateToExerciseManage,
                goToExerciseTrend = navigator::navigateToExerciseTrend,
                goToAnalysis = navigator::navigateToWorkoutAnalysis,
            )
        }
        WorkoutCalendarView(events = events, viewModel = hiltViewModel())
    }

    entry<ExerciseTrendNavKey> {
        val events = remember { ExerciseTrendEvents(goBack = navigator::goBack) }
        ExerciseTrendView(events = events, viewModel = hiltViewModel())
    }

    entry<ExerciseManageNavKey> {
        val events = remember { ExerciseManageEvents(goBack = navigator::goBack) }
        ExerciseManageView(events = events, viewModel = hiltViewModel())
    }

    entry<WorkoutLogNavKey> { navKey ->
        val events = remember {
            WorkoutLogEvents(
                goBack = navigator::goBack,
                goToEntryEdit = navigator::navigateToWorkoutEntryEdit,
                goToAnalysis = navigator::navigateToWorkoutAnalysis,
            )
        }
        WorkoutLogView(
            events = events,
            viewModel = hiltViewModel<WorkoutLogViewModel, WorkoutLogViewModel.Factory> {
                it.create(navKey)
            },
        )
    }

    entry<WorkoutAnalysisNavKey> { navKey ->
        val events = remember {
            WorkoutAnalysisEvents(
                goBack = navigator::goBack,
                goToAiToken = navigator::navigateToAiToken,
            )
        }
        WorkoutAnalysisView(
            events = events,
            viewModel = hiltViewModel<WorkoutAnalysisViewModel, WorkoutAnalysisViewModel.Factory> {
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
