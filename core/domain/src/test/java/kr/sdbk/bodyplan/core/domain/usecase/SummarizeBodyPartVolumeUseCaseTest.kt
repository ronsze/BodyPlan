package kr.sdbk.bodyplan.core.domain.usecase

import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolume
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SummarizeBodyPartVolumeUseCaseTest {
    private val useCase = SummarizeBodyPartVolumeUseCase()

    private fun entry(
        bodyPart: BodyPart,
        intensityType: IntensityType = IntensityType.WEIGHT,
        sets: List<WorkoutSet> = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))),
    ): WorkoutEntry = WorkoutEntry(
        id = 1L,
        exerciseId = 1L,
        exerciseName = "종목",
        bodyPart = bodyPart,
        intensityType = intensityType,
        sets = sets,
    )

    @Test
    fun `무게 종목 기록이 있는 부위는 BodyPart 선언 순서로 나온다`() {
        val entries = listOf(
            entry(BodyPart.LEG),
            entry(BodyPart.CHEST),
            entry(BodyPart.BACK),
        )

        val result = useCase(entries)

        assertEquals(listOf(BodyPart.CHEST, BodyPart.BACK, BodyPart.LEG), result.map { it.bodyPart })
    }

    @Test
    fun `무게 종목 기록이 없는 부위는 담지 않는다`() {
        val entries = listOf(
            entry(
                BodyPart.SHOULDER,
                intensityType = IntensityType.ANGLE,
                sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Angle(45))),
            ),
            entry(
                BodyPart.CARDIO,
                intensityType = IntensityType.DURATION,
                sets = listOf(WorkoutSet(repeatCount = 1, intensity = Intensity.Duration(30))),
            ),
        )

        val result = useCase(entries)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `무게 종목이 있으면 합이 0이어도 담는다`() {
        val entries = listOf(
            entry(BodyPart.CHEST, sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(0)))),
        )

        val result = useCase(entries)

        assertEquals(listOf(BodyPartVolume(BodyPart.CHEST, 0)), result)
    }

    @Test
    fun `한 부위의 여러 무게 기록은 합산된다`() {
        val entries = listOf(
            entry(BodyPart.CHEST, sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20)))),
            entry(BodyPart.CHEST, sets = listOf(WorkoutSet(repeatCount = 5, intensity = Intensity.Weight(30)))),
        )

        val result = useCase(entries)

        assertEquals(listOf(BodyPartVolume(BodyPart.CHEST, 10 * 20 + 5 * 30)), result)
    }

    @Test
    fun `한 부위에 무게와 각도 기록이 섞이면 각도는 셈에서 빠진다`() {
        val entries = listOf(
            entry(BodyPart.SHOULDER, sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20)))),
            entry(
                BodyPart.SHOULDER,
                intensityType = IntensityType.ANGLE,
                sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Angle(45))),
            ),
        )

        val result = useCase(entries)

        assertEquals(listOf(BodyPartVolume(BodyPart.SHOULDER, 200)), result)
    }

    @Test
    fun `빈 입력이면 빈 목록을 낸다`() {
        val result = useCase(emptyList())

        assertTrue(result.isEmpty())
    }
}
