package kr.sdbk.bodyplan.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutineTest {
    private fun weightEntry(bodyPart: BodyPart, vararg sets: Pair<Int, Int>) = WorkoutEntry(
        id = 0L,
        exerciseId = 0L,
        exerciseName = "종목",
        bodyPart = bodyPart,
        intensityType = IntensityType.WEIGHT,
        sets = sets.map { (repeat, kg) -> WorkoutSet(repeatCount = repeat, intensity = Intensity.Weight(kg)) },
    )

    private fun angleEntry(bodyPart: BodyPart) = WorkoutEntry(
        id = 0L,
        exerciseId = 0L,
        exerciseName = "종목",
        bodyPart = bodyPart,
        intensityType = IntensityType.ANGLE,
        sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Angle(0))),
    )

    @Test
    fun `총 세트수는 모든 항목의 세트를 더한다`() {
        val routine = Routine(
            id = 1L,
            name = "루틴",
            entries = listOf(weightEntry(BodyPart.CHEST, 10 to 20, 8 to 25), angleEntry(BodyPart.BACK)),
        )

        assertEquals(3, routine.totalSetCount)
    }

    @Test
    fun `부위별로 종목수 세트수 무게를 BodyPart 선언 순서로 센다`() {
        val entries = listOf(
            weightEntry(BodyPart.BACK, 10 to 30),
            weightEntry(BodyPart.CHEST, 10 to 20, 5 to 40),
            weightEntry(BodyPart.CHEST, 8 to 10),
        )

        val workloads = entries.summarizedByBodyPart()

        assertEquals(
            listOf(
                BodyPartWorkload(BodyPart.CHEST, entryCount = 2, setCount = 3, weightVolumeKg = 480),
                BodyPartWorkload(BodyPart.BACK, entryCount = 1, setCount = 1, weightVolumeKg = 300),
            ),
            workloads,
        )
    }

    @Test
    fun `무게 종목이 없는 부위의 무게는 null이다`() {
        val workloads = listOf(angleEntry(BodyPart.CHEST)).summarizedByBodyPart()

        assertNull(workloads.single().weightVolumeKg)
    }

    @Test
    fun `항목이 없으면 빈 목록이다`() {
        assertEquals(emptyList<BodyPartWorkload>(), emptyList<WorkoutEntry>().summarizedByBodyPart())
    }
}
