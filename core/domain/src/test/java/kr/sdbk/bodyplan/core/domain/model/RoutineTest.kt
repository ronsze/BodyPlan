package kr.sdbk.bodyplan.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineTest {
    private fun routine(id: Long, bodyPart: BodyPart) =
        Routine(id = id, name = "루틴$id", bodyPart = bodyPart, entries = emptyList())

    @Test
    fun `루틴이 없는 부위는 묶음이 나오지 않는다`() {
        val routines = listOf(routine(1L, BodyPart.CHEST))

        val sections = routines.groupedByBodyPart()

        assertEquals(listOf(BodyPart.CHEST), sections.map { it.first })
    }

    @Test
    fun `묶음은 BodyPart 선언 순서로 나온다`() {
        val routines = listOf(routine(1L, BodyPart.LEG), routine(2L, BodyPart.CHEST), routine(3L, BodyPart.BACK))

        val sections = routines.groupedByBodyPart()

        assertEquals(
            BodyPart.entries.filter { it in setOf(BodyPart.LEG, BodyPart.CHEST, BodyPart.BACK) },
            sections.map { it.first },
        )
    }

    @Test
    fun `같은 부위의 루틴은 한 묶음에 모두 들어간다`() {
        val chestA = routine(1L, BodyPart.CHEST)
        val chestB = routine(2L, BodyPart.CHEST)

        val sections = routines(chestA, chestB).groupedByBodyPart()

        assertEquals(listOf(chestA, chestB), sections.single().second)
    }

    @Test
    fun `루틴이 하나도 없으면 묶음도 비어 있다`() {
        val sections = emptyList<Routine>().groupedByBodyPart()

        assertEquals(emptyList<Pair<BodyPart, List<Routine>>>(), sections)
    }

    private fun routines(vararg routines: Routine) = routines.toList()
}
