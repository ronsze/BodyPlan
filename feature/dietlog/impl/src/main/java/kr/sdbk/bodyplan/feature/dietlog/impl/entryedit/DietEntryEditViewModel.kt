package kr.sdbk.bodyplan.feature.dietlog.impl.entryedit

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.repository.DietLogRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel
import kr.sdbk.bodyplan.feature.dietlog.api.DietEntryEditNavKey

@HiltViewModel(assistedFactory = DietEntryEditViewModel.Factory::class)
internal class DietEntryEditViewModel
@AssistedInject
constructor(
    private val dietLogRepository: DietLogRepository,
    @Assisted navKey: DietEntryEditNavKey,
) : BaseViewModel<DietEntryEditState, DietEntryEditIntent, DietEntryEditEffect>(
    initialState = DietEntryEditState(
        date = LocalDate.ofEpochDay(navKey.dateEpochDay),
        editingEntryId = navKey.entryId,
    ),
) {
    override suspend fun initializeData() {
        val entryId = state.value.editingEntryId ?: return
        restoreEntry(entryId)
    }

    override fun handleIntent(intent: DietEntryEditIntent) {
        when (intent) {
            is DietEntryEditIntent.PickImage ->
                updateState { it.copy(pickedImageUri = intent.uri) }

            is DietEntryEditIntent.ChangeMemo -> updateState { it.copy(memo = intent.value) }

            is DietEntryEditIntent.ClickSave -> save()

            is DietEntryEditIntent.ClickBack -> updateEffect(DietEntryEditEffect.GoBack)

            is DietEntryEditIntent.ClickRetry -> retry()
        }
    }

    private fun retry() {
        val entryId = state.value.editingEntryId ?: return
        viewModelScope.launch { restoreEntry(entryId) }
    }

    private suspend fun restoreEntry(entryId: Long) {
        updateState { it.copy(isLoading = true, errorMessage = null) }
        runCatching { requireNotNull(dietLogRepository.getEntry(entryId)) { "기록이 없습니다" } }
            .onSuccess { entry ->
                updateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        storedImagePath = entry.imagePath,
                        memo = entry.memo.orEmpty(),
                    )
                }
            }
            .onFailure { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
    }

    private fun save() {
        val current = state.value
        if (!current.canSave) return

        updateState { it.copy(isSaving = true) }
        viewModelScope.launch {
            val memo = current.memo.trim().ifBlank { null }
            runCatching {
                val entryId = current.editingEntryId
                if (entryId == null) {
                    // canSave가 통과했고 신규에는 저장된 사진이 없으므로 고른 사진이 반드시 있다.
                    dietLogRepository.addEntry(current.date, requireNotNull(current.pickedImageUri), memo)
                } else {
                    // 고른 사진이 없으면 저장된 사진을 그대로 둔다는 뜻이다.
                    dietLogRepository.updateEntry(entryId, current.pickedImageUri, memo)
                }
            }.onSuccess {
                updateState { it.copy(isSaving = false) }
                updateEffect(DietEntryEditEffect.GoBack)
            }.onFailure {
                updateState { it.copy(isSaving = false) }
                updateEffect(DietEntryEditEffect.ShowMessage(SAVE_ERROR))
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: DietEntryEditNavKey): DietEntryEditViewModel
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
private const val SAVE_ERROR = "저장하지 못했습니다"
