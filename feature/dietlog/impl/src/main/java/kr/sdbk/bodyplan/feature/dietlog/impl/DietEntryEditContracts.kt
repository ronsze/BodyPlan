package kr.sdbk.bodyplan.feature.dietlog.impl

import java.time.LocalDate
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class DietEntryEditState(
    val date: LocalDate,
    val editingEntryId: Long? = null,
    /** 이미 저장된 사진의 경로. 수정 진입에서 채운다. */
    val storedImagePath: String? = null,
    /** 이번에 새로 고른 사진. 저장 전이라 아직 파일이 아니다. */
    val pickedImageUri: String? = null,
    val memo: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) : State {
    /** 새로 고른 것이 있으면 그것을, 없으면 저장된 것을 그린다. */
    val previewImage: String? get() = pickedImageUri ?: storedImagePath

    val canSave: Boolean get() = previewImage != null && !isSaving
}

internal sealed interface DietEntryEditIntent : Intent {
    data class PickImage(val uri: String) : DietEntryEditIntent

    data class ChangeMemo(val value: String) : DietEntryEditIntent

    data object ClickSave : DietEntryEditIntent

    data object ClickBack : DietEntryEditIntent

    data object ClickRetry : DietEntryEditIntent
}

internal sealed interface DietEntryEditEffect : Effect {
    data object GoBack : DietEntryEditEffect

    data class ShowMessage(val message: String) : DietEntryEditEffect
}
