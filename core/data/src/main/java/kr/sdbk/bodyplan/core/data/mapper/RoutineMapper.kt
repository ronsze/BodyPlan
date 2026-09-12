package kr.sdbk.bodyplan.core.data.mapper

import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.local.entity.RoutineEntity
import kr.sdbk.bodyplan.core.local.entity.RoutineEntryEntity
import kr.sdbk.bodyplan.core.local.entity.RoutineEntryWithSets
import kr.sdbk.bodyplan.core.local.entity.RoutineSetEntity
import kr.sdbk.bodyplan.core.local.entity.RoutineWithEntries

internal fun RoutineWithEntries.toDomain(): Routine = Routine(
    id = routine.id,
    name = routine.name,
    bodyPart = BodyPart.valueOf(routine.bodyPart),
    // @Relation은 순서를 보장하지 않는다.
    entries = entries
        .sortedWith(compareBy({ it.entry.createdAtMillis }, { it.entry.id }))
        .map { it.toDomain() },
)

internal fun RoutineEntryWithSets.toDomain(): WorkoutEntry {
    val intensityType = IntensityType.valueOf(entry.intensityType)
    return WorkoutEntry(
        id = entry.id,
        exerciseId = entry.exerciseId,
        exerciseName = entry.exerciseName,
        bodyPart = BodyPart.valueOf(entry.bodyPart),
        intensityType = intensityType,
        sets = sets.sortedBy { it.setNumber }.map { it.toDomain(intensityType) },
    )
}

internal fun RoutineSetEntity.toDomain(intensityType: IntensityType): WorkoutSet = WorkoutSet(
    repeatCount = repeatCount,
    intensity = Intensity.of(intensityType, intensityValue),
)

internal fun List<WorkoutSet>.toRoutineSetEntities(entryId: Long): List<RoutineSetEntity> = mapIndexed { index, set ->
    RoutineSetEntity(
        entryId = entryId,
        setNumber = index + 1,
        repeatCount = set.repeatCount,
        intensityValue = set.intensity.value,
    )
}

internal fun newRoutineEntity(name: String, bodyPart: BodyPart, createdAtMillis: Long): RoutineEntity =
    RoutineEntity(name = name, bodyPart = bodyPart.name, createdAtMillis = createdAtMillis)

internal fun newRoutineEntryEntity(routineId: Long, exercise: Exercise, createdAtMillis: Long): RoutineEntryEntity =
    RoutineEntryEntity(
        routineId = routineId,
        exerciseId = exercise.id,
        exerciseName = exercise.name,
        bodyPart = exercise.bodyPart.name,
        intensityType = exercise.intensityType.name,
        createdAtMillis = createdAtMillis,
    )
