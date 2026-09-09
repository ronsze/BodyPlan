package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrendMetric
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrendPoint
import kr.sdbk.bodyplan.core.domain.model.Intensity
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.RecordedExercise
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SummarizeExerciseTrendUseCaseTest {
    private val useCase = SummarizeExerciseTrendUseCase()

    private val week1 = LocalDate.of(2026, 8, 17)
    private val week2 = LocalDate.of(2026, 8, 24)
    private val week3 = LocalDate.of(2026, 8, 31)
    private val week4 = LocalDate.of(2026, 9, 7)
    private val weekStarts = listOf(week1, week2, week3, week4)

    private fun entry(
        exerciseId: Long = 1L,
        exerciseName: String = "벤치프레스",
        bodyPart: BodyPart = BodyPart.CHEST,
        intensityType: IntensityType = IntensityType.WEIGHT,
        sets: List<WorkoutSet>,
    ): WorkoutEntry = WorkoutEntry(
        id = 1L,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        bodyPart = bodyPart,
        intensityType = intensityType,
        sets = sets,
    )

    @Test
    fun `무게 종목은 최고중량과 총볼륨을 함께 담는다`() {
        val entries = mapOf(
            week1 to listOf(
                entry(
                    sets = listOf(
                        WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20)),
                        WorkoutSet(repeatCount = 5, intensity = Intensity.Weight(30)),
                    ),
                ),
            ),
        )

        val result = useCase(entries, exerciseId = 1L, weekStarts = weekStarts)

        val point = result?.points?.first { it.weekStart == week1 }
        assertEquals(
            mapOf(
                ExerciseTrendMetric.MAX_WEIGHT to 30,
                ExerciseTrendMetric.TOTAL_VOLUME to (10 * 20 + 5 * 30),
            ),
            point?.values,
        )
    }

    @Test
    fun `각도 종목은 총 횟수만 담는다`() {
        val entries = mapOf(
            week1 to listOf(
                entry(
                    intensityType = IntensityType.ANGLE,
                    sets = listOf(
                        WorkoutSet(repeatCount = 10, intensity = Intensity.Angle(45)),
                        WorkoutSet(repeatCount = 8, intensity = Intensity.Angle(60)),
                    ),
                ),
            ),
        )

        val result = useCase(entries, exerciseId = 1L, weekStarts = weekStarts)

        val point = result?.points?.first { it.weekStart == week1 }
        assertEquals(mapOf(ExerciseTrendMetric.TOTAL_REPS to 18), point?.values)
    }

    @Test
    fun `시간 종목은 총 시간만 담고 횟수를 곱하지 않는다`() {
        val entries = mapOf(
            week1 to listOf(
                entry(
                    intensityType = IntensityType.DURATION,
                    sets = listOf(
                        WorkoutSet(repeatCount = 3, intensity = Intensity.Duration(30)),
                        WorkoutSet(repeatCount = 1, intensity = Intensity.Duration(20)),
                    ),
                ),
            ),
        )

        val result = useCase(entries, exerciseId = 1L, weekStarts = weekStarts)

        val point = result?.points?.first { it.weekStart == week1 }
        assertEquals(mapOf(ExerciseTrendMetric.TOTAL_MINUTES to 50), point?.values)
    }

    @Test
    fun `기록이 없는 주도 points에 빠짐없이 들어가고 values는 빈 맵이다`() {
        val entries = mapOf(
            week1 to listOf(entry(sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))))),
        )

        val result = useCase(entries, exerciseId = 1L, weekStarts = weekStarts)

        assertEquals(weekStarts, result?.points?.map { it.weekStart })
        assertEquals(emptyMap<ExerciseTrendMetric, Int>(), result?.points?.first { it.weekStart == week2 }?.values)
        assertEquals(emptyMap<ExerciseTrendMetric, Int>(), result?.points?.first { it.weekStart == week3 }?.values)
        assertEquals(emptyMap<ExerciseTrendMetric, Int>(), result?.points?.first { it.weekStart == week4 }?.values)
    }

    @Test
    fun `points의 길이와 순서는 weekStarts와 같다`() {
        val entries = mapOf(
            week1 to listOf(entry(sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))))),
        )

        val result = useCase(entries, exerciseId = 1L, weekStarts = weekStarts)

        assertEquals(weekStarts, result?.points?.map { it.weekStart })
    }

    @Test
    fun `weekStarts에 없는 주의 기록은 버린다`() {
        val outsideWeek = LocalDate.of(2026, 7, 20)
        val entries = mapOf(
            outsideWeek to listOf(entry(sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))))),
        )

        val result = useCase(entries, exerciseId = 1L, weekStarts = weekStarts)

        assertNull(result)
    }

    @Test
    fun `그 종목의 기록이 하나도 없으면 null이다`() {
        val entries = mapOf(
            week1 to
                listOf(
                    entry(
                        exerciseId = 2L,
                        sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))),
                    ),
                ),
        )

        val result = useCase(entries, exerciseId = 1L, weekStarts = weekStarts)

        assertNull(result)
    }

    @Test
    fun `같은 주에 기록이 여러 건이면 세트를 모두 모아 센다`() {
        val entries = mapOf(
            week1 to listOf(entry(sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))))),
            week1.plusDays(2) to
                listOf(entry(sets = listOf(WorkoutSet(repeatCount = 5, intensity = Intensity.Weight(40))))),
        )

        val result = useCase(entries, exerciseId = 1L, weekStarts = weekStarts)

        val point = result?.points?.first { it.weekStart == week1 }
        assertEquals(
            mapOf(
                ExerciseTrendMetric.MAX_WEIGHT to 40,
                ExerciseTrendMetric.TOTAL_VOLUME to (10 * 20 + 5 * 40),
            ),
            point?.values,
        )
    }

    @Test
    fun `다른 종목의 기록은 섞이지 않는다`() {
        val entries = mapOf(
            week1 to listOf(
                entry(exerciseId = 1L, sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20)))),
                entry(exerciseId = 2L, sets = listOf(WorkoutSet(repeatCount = 1, intensity = Intensity.Weight(999)))),
            ),
        )

        val result = useCase(entries, exerciseId = 1L, weekStarts = weekStarts)

        val point = result?.points?.first { it.weekStart == week1 }
        assertEquals(
            mapOf(
                ExerciseTrendMetric.MAX_WEIGHT to 20,
                ExerciseTrendMetric.TOTAL_VOLUME to 200,
            ),
            point?.values,
        )
    }

    @Test
    fun `종목 이름을 고친 경우 exercise는 가장 최근 날짜 기록의 스냅샷을 쓴다`() {
        val entries = mapOf(
            week1 to listOf(
                entry(
                    exerciseName = "옛 이름",
                    sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))),
                ),
            ),
            week2 to listOf(
                entry(
                    exerciseName = "새 이름",
                    sets = listOf(WorkoutSet(repeatCount = 10, intensity = Intensity.Weight(20))),
                ),
            ),
        )

        val result = useCase(entries, exerciseId = 1L, weekStarts = weekStarts)

        assertEquals(
            RecordedExercise(id = 1L, name = "새 이름", bodyPart = BodyPart.CHEST, intensityType = IntensityType.WEIGHT),
            result?.exercise,
        )
    }
}
