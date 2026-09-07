package kr.sdbk.bodyplan.feature.workoutlog.impl.exercisemanage

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.repository.ExerciseRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class ExerciseManageViewModel
@Inject
constructor(private val exerciseRepository: ExerciseRepository) :
    BaseViewModel<ExerciseManageState, ExerciseManageIntent, ExerciseManageEffect>(
        initialState = ExerciseManageState(),
    ) {
    private var loadJob: Job? = null

    override suspend fun initializeData() {
        observeExercises(state.value.selectedBodyPart)
    }

    override fun handleIntent(intent: ExerciseManageIntent) {
        when (intent) {
            is ExerciseManageIntent.SelectBodyPart -> selectBodyPart(intent.bodyPart)

            is ExerciseManageIntent.ClickAdd -> openDialog(null)

            is ExerciseManageIntent.ClickEdit ->
                openDialog(state.value.exercises.firstOrNull { it.id == intent.id })

            is ExerciseManageIntent.ClickDelete -> deleteExercise(intent.id)

            is ExerciseManageIntent.ChangeDialogName ->
                updateState { it.copy(dialogName = intent.value) }

            is ExerciseManageIntent.SelectDialogIntensityType ->
                updateState { it.copy(dialogIntensityType = intent.type) }

            is ExerciseManageIntent.ConfirmDialog -> confirmDialog()

            is ExerciseManageIntent.DismissDialog -> closeDialog()

            is ExerciseManageIntent.ClickBack -> updateEffect(ExerciseManageEffect.GoBack)

            is ExerciseManageIntent.ClickRetry -> observeExercises(state.value.selectedBodyPart)
        }
    }

    private fun selectBodyPart(bodyPart: BodyPart) {
        if (bodyPart == state.value.selectedBodyPart) return
        updateState { it.copy(selectedBodyPart = bodyPart, exercises = emptyList()) }
        observeExercises(bodyPart)
    }

    private fun observeExercises(bodyPart: BodyPart) {
        loadJob?.cancel()
        updateState { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            exerciseRepository.observeExercises(bodyPart)
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { list ->
                    updateState { it.copy(isLoading = false, errorMessage = null, exercises = list) }
                }
        }
    }

    private fun openDialog(target: kr.sdbk.bodyplan.core.domain.model.Exercise?) {
        updateState {
            it.copy(
                editingExercise = target,
                isDialogVisible = true,
                dialogName = target?.name.orEmpty(),
                dialogIntensityType = target?.intensityType ?: IntensityType.WEIGHT,
            )
        }
    }

    private fun closeDialog() {
        updateState { it.copy(isDialogVisible = false, editingExercise = null, dialogName = "") }
    }

    private fun confirmDialog() {
        val current = state.value
        val name = current.dialogName.trim()
        if (name.isBlank()) {
            updateEffect(ExerciseManageEffect.ShowMessage(NAME_REQUIRED))
            return
        }

        val target = current.editingExercise
        closeDialog()
        viewModelScope.launch {
            runCatching {
                if (target == null) {
                    exerciseRepository.addExercise(
                        bodyPart = current.selectedBodyPart,
                        name = name,
                        intensityType = current.dialogIntensityType,
                    )
                } else {
                    exerciseRepository.updateExercise(
                        target.copy(name = name, intensityType = current.dialogIntensityType),
                    )
                }
            }.onFailure { updateEffect(ExerciseManageEffect.ShowMessage(SAVE_ERROR)) }
        }
    }

    private fun deleteExercise(id: Long) {
        viewModelScope.launch {
            runCatching { exerciseRepository.deleteExercise(id) }
                .onFailure { updateEffect(ExerciseManageEffect.ShowMessage(DELETE_ERROR)) }
        }
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
private const val SAVE_ERROR = "저장하지 못했습니다"
private const val DELETE_ERROR = "삭제하지 못했습니다"
private const val NAME_REQUIRED = "종목 이름을 입력하세요"
