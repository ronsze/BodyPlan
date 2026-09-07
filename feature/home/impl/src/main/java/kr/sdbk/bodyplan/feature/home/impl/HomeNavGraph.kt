package kr.sdbk.bodyplan.feature.home.impl

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kr.sdbk.bodyplan.core.navigation.BodyPlanEntryProviderScope
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
import kr.sdbk.bodyplan.feature.dietlog.api.navigateToDietCalendar
import kr.sdbk.bodyplan.feature.home.api.HomeNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.navigateToWorkoutCalendar

fun BodyPlanEntryProviderScope.homeNavGraph(navigator: BodyPlanNavigator) {
    entry<HomeNavKey> {
        val events = remember {
            HomeEvents(
                goBack = navigator::goBack,
                goToWorkoutCalendar = navigator::navigateToWorkoutCalendar,
                goToDietCalendar = navigator::navigateToDietCalendar,
            )
        }
        HomeView(events = events, viewModel = hiltViewModel())
    }
}
