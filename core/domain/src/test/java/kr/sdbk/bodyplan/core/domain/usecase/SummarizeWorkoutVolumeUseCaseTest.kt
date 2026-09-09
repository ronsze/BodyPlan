package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Test

class SummarizeWorkoutVolumeUseCaseTest {
    private val useCase = SummarizeWorkoutVolumeUseCase()

    private fun entry(intensityType: IntensityType, sets: List<WorkoutSet>): WorkoutEntry = WorkoutEntry(
        id = 1L,
        exerciseId = 1L,
        exerciseName = "종목",
        bodyPart = BodyPart.CHEST,
        intensityType = intensityType,
        sets = sets,
    )

    @Test
    fun `무게 종목 세트는 intensity 값과 반복 횟수를 곱해 합산한다`() {
        val entries = mapOf(
            LocalDate.of(2026, 9, 1) to listOf(
                entry(
                    IntensityType.WEIGHT,
                    listOf(
                        WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20)),
                        WorkoutSet(repeatCount = 5, intensity = Intensity.Weight(30)),
                    ),
                ),
            ),
        )

        val result = useCase(entries)

        assertEquals(10 * 20 + 5 * 30, result.weightVolumeKg)
    }

    @Test
    fun `각도 종목은 볼륨에 넣지 않는다`() {
        val entries = mapOf(
            LocalDate.of(2026, 9, 1) to listOf(
                entry(
                    IntensityType.ANGLE,
                    listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Angle(45))),
                ),
            ),
        )

        val result = useCase(entries)

        assertEquals(0, result.weightVolumeKg)
    }

    @Test
    fun `시간 종목은 볼륨에 넣지 않는다`() {
        val entries = mapOf(
            LocalDate.of(2026, 9, 1) to listOf(
                entry(
                    IntensityType.DURATION,
                    listOf(WorkoutSet(repeatCount = 1, intensity = Intensity.Duration(30))),
                ),
            ),
        )

        val result = useCase(entries)

        assertEquals(0, result.weightVolumeKg)
    }

    @Test
    fun `기록이 빈 목록인 날짜는 운동한 날로 세지 않는다`() {
        val entries = mapOf(
            LocalDate.of(2026, 9, 1) to listOf(
                entry(IntensityType.WEIGHT, listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20)))),
            ),
            LocalDate.of(2026, 9, 2) to emptyList(),
        )

        val result = useCase(entries)

        assertEquals(1, result.workoutDays)
    }

    @Test
    fun `기록이 있는 날짜만 운동한 날로 센다`() {
        val entries = mapOf(
            LocalDate.of(2026, 9, 1) to listOf(
                entry(IntensityType.ANGLE, listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Angle(45)))),
            ),
            LocalDate.of(2026, 9, 2) to listOf(
                entry(IntensityType.WEIGHT, listOf(WorkoutSet(repeatCount = 5, intensity = Intensity.Weight(10)))),
            ),
        )

        val result = useCase(entries)

        assertEquals(2, result.workoutDays)
    }

    @Test
    fun `기록이 전혀 없으면 볼륨과 날 수 모두 0이다`() {
        val result = useCase(emptyMap())

        assertEquals(0, result.weightVolumeKg)
        assertEquals(0, result.workoutDays)
    }
}
