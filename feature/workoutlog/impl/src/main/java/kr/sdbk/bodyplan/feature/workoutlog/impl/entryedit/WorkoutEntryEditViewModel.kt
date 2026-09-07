package kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutOptions
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.ExerciseRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutEntryEditNavKey

@HiltViewModel(assistedFactory = WorkoutEntryEditViewModel.Factory::class)
internal class WorkoutEntryEditViewModel
@AssistedInject
constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    @Assisted navKey: WorkoutEntryEditNavKey,
) : BaseViewModel<WorkoutEntryEditState, WorkoutEntryEditIntent, WorkoutEntryEditEffect>(
    initialState = WorkoutEntryEditState(
        date = LocalDate.ofEpochDay(navKey.dateEpochDay),
        editingEntryId = navKey.entryId,
    ),
) {
    private var exercisesJob: Job? = null

    override suspend fun initializeData() {
        val entryId = state.value.editingEntryId ?: return
        restoreEntry(entryId)
    }

    override fun handleIntent(intent: WorkoutEntryEditIntent) {
        when (intent) {
            is WorkoutEntryEditIntent.SelectBodyPart -> selectBodyPart(intent.bodyPart)

            is WorkoutEntryEditIntent.SelectExercise -> selectExercise(intent.id)

            is WorkoutEntryEditIntent.ClickAddSet -> addSet()

            is WorkoutEntryEditIntent.ClickRemoveSet -> removeSet(intent.setInputId)

            is WorkoutEntryEditIntent.SelectSetRepeatCount ->
                updateSet(intent.setInputId) { it.copy(repeatCount = intent.value) }

            is WorkoutEntryEditIntent.SelectSetIntensity ->
                updateSet(intent.setInputId) { it.copy(intensityValue = intent.value) }

            is WorkoutEntryEditIntent.ClickSave -> save()

            is WorkoutEntryEditIntent.ClickBack -> updateEffect(WorkoutEntryEditEffect.GoBack)

            is WorkoutEntryEditIntent.ClickRetry -> retry()
        }
    }

    private fun retry() {
        val entryId = state.value.editingEntryId
        if (entryId != null) {
            viewModelScope.launch { restoreEntry(entryId) }
            return
        }
        state.value.selectedBodyPart?.let(::observeExercises)
    }

    private suspend fun restoreEntry(entryId: Long) {
        updateState { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            val entry = requireNotNull(workoutLogRepository.getEntry(entryId)) { "기록이 없습니다" }
            entry to resolveExercise(entry)
        }.onSuccess { (entry, exercise) ->
            updateState { current ->
                current.copy(
                    isLoading = false,
                    errorMessage = null,
                    selectedBodyPart = entry.bodyPart,
                    selectedExercise = exercise,
                    sets = entry.sets.mapIndexed { index, set ->
                        SetInput(
                            id = index.toLong(),
                            repeatCount = set.repeatCount,
                            intensityValue = set.intensity.value,
                        )
                    },
                    nextSetInputId = entry.sets.size.toLong(),
                )
            }
            observeExercises(entry.bodyPart)
        }.onFailure {
            updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) }
        }
    }

    /** 종목이 지워졌으면 기록의 스냅샷으로 되살린다. 지운 종목의 기록도 세트는 고칠 수 있어야 한다. */
    private suspend fun resolveExercise(entry: WorkoutEntry): Exercise =
        exerciseRepository.getExercise(entry.exerciseId) ?: Exercise(
            id = entry.exerciseId,
            bodyPart = entry.bodyPart,
            name = entry.exerciseName,
            intensityType = entry.intensityType,
            isDeleted = true,
        )

    private fun selectBodyPart(bodyPart: BodyPart) {
        updateState {
            it.copy(
                selectedBodyPart = bodyPart,
                selectedExercise = null,
                sets = emptyList(),
                exercises = emptyList(),
            )
        }
        observeExercises(bodyPart)
    }

    private fun observeExercises(bodyPart: BodyPart) {
        exercisesJob?.cancel()
        updateState { it.copy(isLoading = true, errorMessage = null) }
        exercisesJob = viewModelScope.launch {
            exerciseRepository.observeExercises(bodyPart)
                .catch { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
                .collect { list ->
                    updateState { it.copy(isLoading = false, errorMessage = null, exercises = list) }
                }
        }
    }

    private fun selectExercise(id: Long) {
        val exercise = state.value.exercises.firstOrNull { it.id == id } ?: return
        updateState { current ->
            current.copy(
                selectedExercise = exercise,
                sets = listOf(newSetInput(current.nextSetInputId, exercise.intensityType)),
                nextSetInputId = current.nextSetInputId + 1,
            )
        }
    }

    private fun addSet() {
        val intensityType = state.value.selectedExercise?.intensityType ?: return
        updateState { current ->
            if (!current.canAddSet) return@updateState current
            current.copy(
                sets = current.sets + newSetInput(current.nextSetInputId, intensityType),
                nextSetInputId = current.nextSetInputId + 1,
            )
        }
    }

    private fun newSetInput(id: Long, intensityType: IntensityType) = SetInput(
        id = id,
        repeatCount = WorkoutOptions.repeatCounts.first(),
        intensityValue = WorkoutOptions.defaultIntensity(intensityType).value,
    )

    private fun removeSet(setInputId: Long) {
        updateState { current ->
            if (!current.canRemoveSet) return@updateState current
            current.copy(sets = current.sets.filterNot { it.id == setInputId })
        }
    }

    private fun updateSet(setInputId: Long, transform: (SetInput) -> SetInput) {
        updateState { current ->
            current.copy(
                sets = current.sets.map { if (it.id == setInputId) transform(it) else it },
            )
        }
    }

    private fun save() {
        val current = state.value
        val exercise = current.selectedExercise
        if (exercise == null || !current.canSave) return

        updateState { it.copy(isSaving = true) }
        viewModelScope.launch {
            val sets = current.sets.map { input ->
                WorkoutSet(
                    repeatCount = input.repeatCount,
                    intensity = Intensity.of(exercise.intensityType, input.intensityValue),
                )
            }
            runCatching {
                val entryId = current.editingEntryId
                if (entryId == null) {
                    workoutLogRepository.addEntry(current.date, exercise, sets)
                } else {
                    workoutLogRepository.updateEntry(entryId, exercise, sets)
                }
            }.onSuccess {
                updateState { it.copy(isSaving = false) }
                updateEffect(WorkoutEntryEditEffect.GoBack)
            }.onFailure {
                updateState { it.copy(isSaving = false) }
                updateEffect(WorkoutEntryEditEffect.ShowMessage(SAVE_ERROR))
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: WorkoutEntryEditNavKey): WorkoutEntryEditViewModel
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
private const val SAVE_ERROR = "저장하지 못했습니다"
