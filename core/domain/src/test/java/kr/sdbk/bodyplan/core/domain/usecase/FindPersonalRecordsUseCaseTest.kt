package kr.sdbk.bodyplan.core.domain.usecase

import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.ExerciseBest
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindPersonalRecordsUseCaseTest {
    private val useCase = FindPersonalRecordsUseCase()

    private fun entry(
        id: Long = 1L,
        exerciseId: Long = 1L,
        intensityType: IntensityType = IntensityType.WEIGHT,
        sets: List<WorkoutSet>,
    ) = WorkoutEntry(
        id = id,
        exerciseId = exerciseId,
        exerciseName = "종목",
        bodyPart = BodyPart.CHEST,
        intensityType = intensityType,
        sets = sets,
    )

    private fun best(
        exerciseId: Long = 1L,
        intensityType: IntensityType = IntensityType.WEIGHT,
        maxIntensityValue: Int = 0,
        maxRepeatCount: Int = 0,
    ) = ExerciseBest(
        exerciseId = exerciseId,
        intensityType = intensityType,
        maxIntensityValue = maxIntensityValue,
        maxRepeatCount = maxRepeatCount,
    )

    @Test
    fun `무게 종목은 세트 중 최고 무게로 이전 최고 무게를 넘겨야 PR이다`() {
        val entries = listOf(
            entry(sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60)))),
        )
        val bestBefore = listOf(best(maxIntensityValue = 50))

        val result = useCase(entries, bestBefore)

        assertEquals(setOf(1L), result)
    }

    @Test
    fun `각도 종목은 무게가 아니라 세트 중 최고 횟수로 견준다`() {
        val entries = listOf(
            entry(
                intensityType = IntensityType.ANGLE,
                sets = listOf(WorkoutSet(repeatCount = 20, intensity = Intensity.Angle(30))),
            ),
        )
        // 강도값(각도)은 낮아졌지만 횟수가 늘었으므로 PR이어야 한다.
        val bestBefore = listOf(best(intensityType = IntensityType.ANGLE, maxIntensityValue = 999, maxRepeatCount = 15))

        val result = useCase(entries, bestBefore)

        assertEquals(setOf(1L), result)
    }

    @Test
    fun `시간 종목은 세트 중 최장 시간으로 견준다`() {
        val entries = listOf(
            entry(
                intensityType = IntensityType.DURATION,
                sets = listOf(WorkoutSet(repeatCount = 1, intensity = Intensity.Duration(40))),
            ),
        )
        val bestBefore = listOf(best(intensityType = IntensityType.DURATION, maxIntensityValue = 30))

        val result = useCase(entries, bestBefore)

        assertEquals(setOf(1L), result)
    }

    @Test
    fun `이전 최고값보다 작으면 PR이 아니다`() {
        val entries = listOf(
            entry(sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(40)))),
        )
        val bestBefore = listOf(best(maxIntensityValue = 50))

        val result = useCase(entries, bestBefore)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `이전 최고값과 같으면 PR이 아니다`() {
        val entries = listOf(
            entry(sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(50)))),
        )
        val bestBefore = listOf(best(maxIntensityValue = 50))

        val result = useCase(entries, bestBefore)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `이전 기록이 없는 종목은 아무리 높아도 PR이 아니다`() {
        val entries = listOf(
            entry(sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(100)))),
        )

        val result = useCase(entries, emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `이전 기록이 다른 축뿐이면 PR이 아니다`() {
        val entries = listOf(
            entry(sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(100)))),
        )
        // 같은 종목이지만 각도 축으로만 기록이 있다 — 무게 축으로는 견줄 것이 없다.
        val bestBefore = listOf(best(intensityType = IntensityType.ANGLE, maxRepeatCount = 5))

        val result = useCase(entries, bestBefore)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `같은 종목에 축별 기록이 둘 있으면 지금 축의 기록만 견준다`() {
        val entries = listOf(
            entry(sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60)))),
        )
        val bestBefore = listOf(
            best(maxIntensityValue = 50),
            best(intensityType = IntensityType.ANGLE, maxRepeatCount = 999),
        )

        val result = useCase(entries, bestBefore)

        assertEquals(setOf(1L), result)
    }

    @Test
    fun `같은 종목을 하루에 여러 건 적으면 그날 최댓값으로 판정한다`() {
        val entries = listOf(
            entry(id = 1L, sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(45)))),
            entry(id = 2L, sets = listOf(WorkoutSet(repeatCount = 6, intensity = Intensity.Weight(60)))),
        )
        val bestBefore = listOf(best(maxIntensityValue = 50))

        val result = useCase(entries, bestBefore)

        assertEquals(setOf(1L), result)
    }

    @Test
    fun `여러 종목 중 넘긴 종목만 PR 집합에 담긴다`() {
        val entries = listOf(
            entry(
                id = 1L,
                exerciseId = 1L,
                sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(60))),
            ),
            entry(
                id = 2L,
                exerciseId = 2L,
                sets = listOf(WorkoutSet(repeatCount = 8, intensity = Intensity.Weight(20))),
            ),
        )
        val bestBefore = listOf(
            best(exerciseId = 1L, maxIntensityValue = 50),
            best(exerciseId = 2L, maxIntensityValue = 30),
        )

        val result = useCase(entries, bestBefore)

        assertEquals(setOf(1L), result)
    }
}
