package kr.sdbk.bodyplan.core.data.mapper

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.ExerciseBest
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.local.entity.DateBodyPart
import kr.sdbk.bodyplan.core.local.entity.ExerciseBestRow
import kr.sdbk.bodyplan.core.local.entity.ExerciseEntity
import kr.sdbk.bodyplan.core.local.entity.WorkoutEntryEntity
import kr.sdbk.bodyplan.core.local.entity.WorkoutEntryWithSets
import kr.sdbk.bodyplan.core.local.entity.WorkoutMemoEntity
import kr.sdbk.bodyplan.core.local.entity.WorkoutSetEntity

internal fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    bodyPart = BodyPart.valueOf(bodyPart),
    name = name,
    intensityType = IntensityType.valueOf(intensityType),
    isDeleted = isDeleted,
)

internal fun WorkoutEntryWithSets.toDomain(): WorkoutEntry {
    val intensityType = IntensityType.valueOf(entry.intensityType)
    return WorkoutEntry(
        id = entry.id,
        exerciseId = entry.exerciseId,
        exerciseName = entry.exerciseName,
        bodyPart = BodyPart.valueOf(entry.bodyPart),
        intensityType = intensityType,
        // @Relation은 순서를 보장하지 않는다.
        sets = sets.sortedBy { it.setNumber }.map { it.toDomain(intensityType) },
    )
}

internal fun ExerciseBestRow.toDomain(): ExerciseBest = ExerciseBest(
    exerciseId = exerciseId,
    intensityType = IntensityType.valueOf(intensityType),
    maxIntensityValue = maxIntensityValue,
    maxRepeatCount = maxRepeatCount,
)

internal fun WorkoutSetEntity.toDomain(intensityType: IntensityType): WorkoutSet = WorkoutSet(
    repeatCount = repeatCount,
    intensity = Intensity.of(intensityType, intensityValue),
)

internal fun List<WorkoutSet>.toEntities(entryId: Long): List<WorkoutSetEntity> = mapIndexed { index, set ->
    WorkoutSetEntity(
        entryId = entryId,
        setNumber = index + 1,
        repeatCount = set.repeatCount,
        intensityValue = set.intensity.value,
    )
}

internal fun newEntryEntity(date: LocalDate, exercise: Exercise, createdAtMillis: Long): WorkoutEntryEntity =
    WorkoutEntryEntity(
        dateEpochDay = date.toEpochDay(),
        exerciseId = exercise.id,
        exerciseName = exercise.name,
        bodyPart = exercise.bodyPart.name,
        intensityType = exercise.intensityType.name,
        createdAtMillis = createdAtMillis,
    )

/** 루틴 항목을 그날 기록으로 옮긴다. `id`는 비워 새 행으로 들어가게 한다. */
internal fun WorkoutEntry.toEntityWithSets(date: LocalDate, createdAtMillis: Long): WorkoutEntryWithSets =
    WorkoutEntryWithSets(
        entry = WorkoutEntryEntity(
            dateEpochDay = date.toEpochDay(),
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            bodyPart = bodyPart.name,
            intensityType = intensityType.name,
            createdAtMillis = createdAtMillis,
        ),
        sets = sets.toEntities(entryId = 0L),
    )

/** 기록이 없는 날짜는 결과에 넣지 않는다 — 빈 목록과 기록 없음을 호출부가 구분할 이유가 없다. */
internal fun List<WorkoutEntryWithSets>.toEntriesByDate(): Map<LocalDate, List<WorkoutEntry>> =
    groupBy { LocalDate.ofEpochDay(it.entry.dateEpochDay) }
        .mapValues { (_, rows) -> rows.map { it.toDomain() } }

/** 기록이 없는 날짜는 결과에 넣지 않는다. */
internal fun List<DateBodyPart>.toBodyPartsByDate(): Map<LocalDate, Set<BodyPart>> =
    groupBy { LocalDate.ofEpochDay(it.dateEpochDay) }
        .mapValues { (_, rows) -> rows.map { BodyPart.valueOf(it.bodyPart) }.toSet() }

internal fun newMemoEntity(date: LocalDate, text: String, updatedAtMillis: Long): WorkoutMemoEntity = WorkoutMemoEntity(
    dateEpochDay = date.toEpochDay(),
    text = text,
    updatedAtMillis = updatedAtMillis,
)
