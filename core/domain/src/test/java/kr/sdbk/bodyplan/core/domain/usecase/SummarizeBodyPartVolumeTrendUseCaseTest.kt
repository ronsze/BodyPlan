package kr.sdbk.bodyplan.core.domain.usecase

import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.ProgressDirection
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SummarizeBodyPartVolumeTrendUseCaseTest {
    private val useCase = SummarizeBodyPartVolumeTrendUseCase(SummarizeBodyPartVolumeUseCase())

    private fun weightEntry(bodyPart: BodyPart, volume: Int): WorkoutEntry = WorkoutEntry(
        id = 1L,
        exerciseId = 1L,
        exerciseName = "종목",
        bodyPart = bodyPart,
        intensityType = IntensityType.WEIGHT,
        sets = listOf(WorkoutSet(repeatCount = 1, intensity = Intensity.Weight(volume))),
    )

    @Test
    fun `두 구간 모두 기록이 있는 부위는 changeKg가 최근에서 그 앞을 뺀 값이고 IMPROVING이다`() {
        val recent = listOf(weightEntry(BodyPart.CHEST, 100))
        val previous = listOf(weightEntry(BodyPart.CHEST, 40))

        val trends = useCase(recent, previous)

        assertEquals(1, trends.size)
        assertEquals(BodyPart.CHEST, trends[0].bodyPart)
        assertEquals(100, trends[0].recentVolumeKg)
        assertEquals(60, trends[0].changeKg)
        assertEquals(ProgressDirection.IMPROVING, trends[0].direction)
    }

    @Test
    fun `최근이 그 앞보다 작으면 changeKg가 음수이고 WORSENING이다`() {
        val recent = listOf(weightEntry(BodyPart.LEG, 30))
        val previous = listOf(weightEntry(BodyPart.LEG, 80))

        val trends = useCase(recent, previous)

        assertEquals(-50, trends[0].changeKg)
        assertEquals(ProgressDirection.WORSENING, trends[0].direction)
    }

    @Test
    fun `최근과 그 앞이 같으면 changeKg가 0이고 STEADY다`() {
        val recent = listOf(weightEntry(BodyPart.BACK, 50))
        val previous = listOf(weightEntry(BodyPart.BACK, 50))

        val trends = useCase(recent, previous)

        assertEquals(0, trends[0].changeKg)
        assertEquals(ProgressDirection.STEADY, trends[0].direction)
    }

    @Test
    fun `최근에만 기록이 있으면 그 앞은 0으로 보고 changeKg는 recentVolumeKg와 같다`() {
        val recent = listOf(weightEntry(BodyPart.SHOULDER, 70))

        val trends = useCase(recent, emptyList())

        assertEquals(1, trends.size)
        assertEquals(70, trends[0].recentVolumeKg)
        assertEquals(70, trends[0].changeKg)
        assertEquals(ProgressDirection.IMPROVING, trends[0].direction)
    }

    @Test
    fun `그 앞에만 기록이 있으면 최근은 0이고 changeKg는 음수다`() {
        val previous = listOf(weightEntry(BodyPart.TRICEPS, 40))

        val trends = useCase(emptyList(), previous)

        assertEquals(1, trends.size)
        assertEquals(0, trends[0].recentVolumeKg)
        assertEquals(-40, trends[0].changeKg)
        assertEquals(ProgressDirection.WORSENING, trends[0].direction)
    }

    @Test
    fun `두 구간 모두 무게 기록이 없는 부위는 담지 않는다`() {
        val trends = useCase(emptyList(), emptyList())

        assertTrue(trends.isEmpty())
    }

    @Test
    fun `여러 부위가 있으면 BodyPart 선언 순서로 나온다`() {
        val recent = listOf(
            weightEntry(BodyPart.LEG, 10),
            weightEntry(BodyPart.CHEST, 10),
            weightEntry(BodyPart.BACK, 10),
        )

        val trends = useCase(recent, emptyList())

        assertEquals(listOf(BodyPart.CHEST, BodyPart.BACK, BodyPart.LEG), trends.map { it.bodyPart })
    }
}
