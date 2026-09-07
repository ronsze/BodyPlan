package kr.sdbk.bodyplan.core.data.mapper

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.local.entity.DateBodyPart
import kr.sdbk.bodyplan.core.local.entity.ExerciseEntity
import kr.sdbk.bodyplan.core.local.entity.WorkoutEntryEntity
import kr.sdbk.bodyplan.core.local.entity.WorkoutEntryWithSets
import kr.sdbk.bodyplan.core.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutMapperTest {
    @Test
    fun `ExerciseEntity를 Exercise로 변환한다`() {
        val entity = ExerciseEntity(
            id = 1L,
            bodyPart = BodyPart.CHEST.name,
            name = "벤치프레스",
            intensityType = IntensityType.WEIGHT.name,
            isDeleted = true,
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals(BodyPart.CHEST, domain.bodyPart)
        assertEquals("벤치프레스", domain.name)
        assertEquals(IntensityType.WEIGHT, domain.intensityType)
        assertTrue(domain.isDeleted)
    }

    @Test
    fun `WorkoutEntryWithSets를 WorkoutEntry로 변환하며 세트를 setNumber 순으로 정렬한다`() {
        val entry = WorkoutEntryEntity(
            id = 10L,
            dateEpochDay = LocalDate.of(2026, 9, 7).toEpochDay(),
            exerciseId = 5L,
            exerciseName = "스쿼트",
            bodyPart = BodyPart.LEG.name,
            intensityType = IntensityType.WEIGHT.name,
            createdAtMillis = 0L,
        )
        // 저장 순서와 다르게 섞어 두어 정렬 로직을 검증한다.
        val sets = listOf(
            WorkoutSetEntity(id = 2L, entryId = 10L, setNumber = 2, repeatCount = 8, intensityValue = 60),
            WorkoutSetEntity(id = 1L, entryId = 10L, setNumber = 1, repeatCount = 12, intensityValue = 40),
        )

        val domain = WorkoutEntryWithSets(entry, sets).toDomain()

        assertEquals(10L, domain.id)
        assertEquals(5L, domain.exerciseId)
        assertEquals("스쿼트", domain.exerciseName)
        assertEquals(BodyPart.LEG, domain.bodyPart)
        assertEquals(IntensityType.WEIGHT, domain.intensityType)
        assertEquals(
            listOf(
                WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40)),
                WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60)),
            ),
            domain.sets,
        )
    }

    @Test
    fun `WorkoutSet 목록을 setNumber가 1부터 증가하는 WorkoutSetEntity 목록으로 변환한다`() {
        val sets = listOf(
            WorkoutSet(repeatCount = 12, intensity = Intensity.Weight(40)),
            WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60)),
        )

        val entities = sets.toEntities(entryId = 3L)

        assertEquals(
            listOf(
                WorkoutSetEntity(entryId = 3L, setNumber = 1, repeatCount = 12, intensityValue = 40),
                WorkoutSetEntity(entryId = 3L, setNumber = 2, repeatCount = 8, intensityValue = 60),
            ),
            entities,
        )
    }

    @Test
    fun `newEntryEntity는 종목 스냅샷을 담은 신규 WorkoutEntryEntity를 만든다`() {
        val date = LocalDate.of(2026, 9, 5)
        val exercise = Exercise(
            id = 7L,
            bodyPart = BodyPart.BACK,
            name = "랫풀다운",
            intensityType = IntensityType.WEIGHT,
        )

        val entity = newEntryEntity(date, exercise, createdAtMillis = 1234L)

        assertEquals(date.toEpochDay(), entity.dateEpochDay)
        assertEquals(7L, entity.exerciseId)
        assertEquals("랫풀다운", entity.exerciseName)
        assertEquals(BodyPart.BACK.name, entity.bodyPart)
        assertEquals(IntensityType.WEIGHT.name, entity.intensityType)
        assertEquals(1234L, entity.createdAtMillis)
    }

    @Test
    fun `기록이 없는 날짜는 결과 맵에서 빠진다`() {
        val recordedDay = LocalDate.of(2026, 9, 3)
        val rows = listOf(DateBodyPart(recordedDay.toEpochDay(), BodyPart.CHEST.name))

        val result = rows.toBodyPartsByDate()

        assertEquals(setOf(recordedDay), result.keys)
        assertFalse(result.containsKey(LocalDate.of(2026, 9, 4)))
    }

    @Test
    fun `같은 날짜의 여러 부위는 하나의 집합으로 모인다`() {
        val day = LocalDate.of(2026, 9, 3)
        val rows = listOf(
            DateBodyPart(day.toEpochDay(), BodyPart.CHEST.name),
            DateBodyPart(day.toEpochDay(), BodyPart.BACK.name),
        )

        val result = rows.toBodyPartsByDate()

        assertEquals(setOf(BodyPart.CHEST, BodyPart.BACK), result[day])
    }
}
