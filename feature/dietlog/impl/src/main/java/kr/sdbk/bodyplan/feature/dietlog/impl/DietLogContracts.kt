package kr.sdbk.bodyplan.feature.dietlog.impl

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.DietEntry
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class DietLogState(
    val date: LocalDate,
    val entries: List<DietEntry> = emptyList(),
    val isEditable: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : State

internal sealed interface DietLogIntent : Intent {
    data object ClickAddEntry : DietLogIntent

    data class ClickEntry(val id: Long) : DietLogIntent

    data class ClickDeleteEntry(val id: Long) : DietLogIntent

    /** 사진을 길게 눌러 갤러리로 내보낸다. */
    data class LongClickEntry(val id: Long) : DietLogIntent

    data object ClickBack : DietLogIntent

    data object ClickRetry : DietLogIntent
}

internal sealed interface DietLogEffect : Effect {
    data class NavigateToEntryEdit(val date: LocalDate, val entryId: Long?) : DietLogEffect

    data object GoBack : DietLogEffect

    data class ShowMessage(val message: String) : DietLogEffect
}
