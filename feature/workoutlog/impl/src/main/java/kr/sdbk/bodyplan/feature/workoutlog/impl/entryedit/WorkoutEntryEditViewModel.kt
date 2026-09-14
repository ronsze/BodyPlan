package kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kr.sdbk.bodyplan.core.domain.model.countsRepeats
import kr.sdbk.bodyplan.core.domain.repository.ExerciseRepository
import kr.sdbk.bodyplan.core.domain.repository.RoutineRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import kr.sdbk.bodyplan.core.domain.usecase.IsPersonalRecordUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel(assistedFactory = WorkoutEntryEditViewModel.Factory::class)
internal class WorkoutEntryEditViewModel
@AssistedInject
constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val routineRepository: RoutineRepository,
    private val isPersonalRecord: IsPersonalRecordUseCase,
    @Assisted target: WorkoutEntryEditTarget,
    @Assisted editingEntryId: Long?,
) : BaseViewModel<WorkoutEntryEditState, WorkoutEntryEditIntent, WorkoutEntryEditEffect>(
    initialState = WorkoutEntryEditState(target = target, editingEntryId = editingEntryId),
) {
    private var exercisesJob: Job? = null
    private var prefillJob: Job? = null

    override suspend fun initializeData() {
        val entryId = state.value.editingEntryId
        when {
            entryId != null -> restoreEntry(entryId)

            // 루틴은 부위가 정해져 있어 고르게 하지 않고 바로 그 부위 종목을 연다.
            state.value.target is WorkoutEntryEditTarget.Routine -> loadRoutineBodyPart()
        }
    }

    override fun handleIntent(intent: WorkoutEntryEditIntent) {
        when (intent) {
            // 탭이 없지만 상태 쪽에서도 막는다 — 루틴 항목의 부위는 루틴이 정한다.
            is WorkoutEntryEditIntent.SelectBodyPart ->
                if (!state.value.isBodyPartLocked) selectBodyPart(intent.bodyPart)

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
        // 부위가 이미 정해졌으면 종목 구독만 실패한 것이다. 루틴을 다시 읽어 부위를 되돌리지 않는다.
        val bodyPart = state.value.selectedBodyPart
        if (bodyPart != null) {
            observeExercises(bodyPart)
            return
        }
        if (state.value.target is WorkoutEntryEditTarget.Routine) {
            viewModelScope.launch { loadRoutineBodyPart() }
        }
    }

    private suspend fun loadRoutineBodyPart() {
        val routineId = (state.value.target as? WorkoutEntryEditTarget.Routine)?.routineId ?: return
        updateState { it.copy(isLoading = true, errorMessage = null) }
        runCatching { requireNotNull(routineRepository.getRoutine(routineId)) { "루틴이 없습니다" } }
            .onSuccess { routine -> selectBodyPart(routine.bodyPart) }
            .onFailure { updateState { it.copy(isLoading = false, errorMessage = LOAD_ERROR) } }
    }

    private suspend fun restoreEntry(entryId: Long) {
        updateState { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            val entry = requireNotNull(getStoredEntry(entryId)) { "기록이 없습니다" }
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

    private suspend fun getStoredEntry(entryId: Long): WorkoutEntry? = when (state.value.target) {
        is WorkoutEntryEditTarget.Log -> workoutLogRepository.getEntry(entryId)
        is WorkoutEntryEditTarget.Routine -> routineRepository.getEntry(entryId)
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
        prefillFromLatest(exercise)
    }

    /**
     * 신규 작성·일지 대상에서만 지난 세트를 미리 채운다 — 수정은 이미 값이 있고,
     * 루틴 항목은 기준값이라 지난 기록에 끌려가면 안 된다.
     *
     * 조회를 기다리지 않는 것은 종목을 골랐는데 세트가 안 보이는 순간을 없애기 위해서다.
     * 조회 실패는 무시한다 — 채움은 편의이지 필수가 아니다.
     */
    private fun prefillFromLatest(exercise: Exercise) {
        prefillJob?.cancel()
        val target = state.value.target as? WorkoutEntryEditTarget.Log ?: return
        if (state.value.editingEntryId != null) return
        prefillJob = viewModelScope.launch {
            val latest = runCatching { workoutLogRepository.getLatestEntry(exercise.id, target.date) }
                .getOrNull() ?: return@launch
            // 축을 바꾼 종목의 옛 기록은 값의 뜻이 다르다 — 무게 80이 시간 80분으로 들어오면 안 된다.
            if (latest.sets.isEmpty() || latest.intensityType != exercise.intensityType) return@launch
            updateState { current ->
                // 그 사이 다른 종목을 골랐으면 그쪽 세트를 덮지 않는다.
                if (current.selectedExercise?.id != exercise.id) return@updateState current
                current.copy(
                    sets = latest.sets.mapIndexed { index, set ->
                        SetInput(
                            id = current.nextSetInputId + index,
                            repeatCount = set.repeatCount,
                            intensityValue = set.intensity.value,
                        )
                    },
                    nextSetInputId = current.nextSetInputId + latest.sets.size,
                )
            }
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

    /** 시간으로 재는 종목은 한 세트가 한 회차다. 화면에서 감춘 값이 4로 저장되지 않게 여기서 정한다. */
    private fun newSetInput(id: Long, intensityType: IntensityType) = SetInput(
        id = id,
        repeatCount = if (intensityType.countsRepeats) {
            WorkoutOptions.repeatCounts.first()
        } else {
            WorkoutOptions.SINGLE_REPEAT
        },
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
                saveTo(current.target, current.editingEntryId, exercise, sets)
            }.onSuccess {
                updateState { it.copy(isSaving = false) }
                notifyPersonalRecord(current.target, exercise, sets)
                updateEffect(WorkoutEntryEditEffect.GoBack)
            }.onFailure {
                updateState { it.copy(isSaving = false) }
                updateEffect(WorkoutEntryEditEffect.ShowMessage(SAVE_ERROR))
            }
        }
    }

    /** 판정이 실패해도 저장은 끝났으므로 조용히 넘어간다. 루틴은 기록이 아니라 견줄 것이 없다. */
    private suspend fun notifyPersonalRecord(
        target: WorkoutEntryEditTarget,
        exercise: Exercise,
        sets: List<WorkoutSet>,
    ) {
        val date = (target as? WorkoutEntryEditTarget.Log)?.date ?: return
        val isRecord = runCatching { isPersonalRecord(date, exercise.id, exercise.intensityType, sets) }
            .getOrDefault(false)
        if (isRecord) {
            updateEffect(
                WorkoutEntryEditEffect.ShowMessage("${exercise.name} ${exercise.intensityType.recordLabel} 갱신!"),
            )
        }
    }

    private suspend fun saveTo(
        target: WorkoutEntryEditTarget,
        entryId: Long?,
        exercise: Exercise,
        sets: List<WorkoutSet>,
    ) {
        when (target) {
            is WorkoutEntryEditTarget.Log ->
                if (entryId == null) {
                    workoutLogRepository.addEntry(target.date, exercise, sets)
                } else {
                    workoutLogRepository.updateEntry(entryId, exercise, sets)
                }

            is WorkoutEntryEditTarget.Routine ->
                if (entryId == null) {
                    routineRepository.addEntry(target.routineId, exercise, sets)
                } else {
                    routineRepository.updateEntry(entryId, exercise, sets)
                }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(target: WorkoutEntryEditTarget, editingEntryId: Long?): WorkoutEntryEditViewModel
    }
}

private const val LOAD_ERROR = "불러오지 못했습니다"
private const val SAVE_ERROR = "저장하지 못했습니다"
