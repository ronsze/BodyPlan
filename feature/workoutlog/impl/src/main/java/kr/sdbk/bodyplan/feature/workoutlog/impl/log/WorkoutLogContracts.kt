package kr.sdbk.bodyplan.feature.workoutlog.impl.log

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class WorkoutLogState(
    val date: LocalDate,
    val entries: List<WorkoutEntry> = emptyList(),
    val isEditable: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : State

internal sealed interface WorkoutLogIntent : Intent {
    data object ClickAddEntry : WorkoutLogIntent

    data class ClickEntry(val id: Long) : WorkoutLogIntent

    data class ClickDeleteEntry(val id: Long) : WorkoutLogIntent

    data object ClickBack : WorkoutLogIntent

    data object ClickRetry : WorkoutLogIntent
}

internal sealed interface WorkoutLogEffect : Effect {
    data class NavigateToEntryEdit(val date: LocalDate, val entryId: Long?) : WorkoutLogEffect

    data object GoBack : WorkoutLogEffect

    data class ShowMessage(val message: String) : WorkoutLogEffect
}
