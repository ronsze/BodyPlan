package kr.sdbk.bodyplan.core.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutMemoTest {
    @Test
    fun `칩과 글이 모두 비어 있으면 비어 있다`() {
        assertTrue(WorkoutMemo().isEmpty)
    }

    @Test
    fun `컨디션만 있어도 비어 있지 않다`() {
        assertFalse(WorkoutMemo(condition = Condition.GOOD).isEmpty)
    }

    @Test
    fun `강도만 있어도 비어 있지 않다`() {
        assertFalse(WorkoutMemo(effort = Effort.LIGHT).isEmpty)
    }

    @Test
    fun `통증 부위만 있어도 비어 있지 않다`() {
        assertFalse(WorkoutMemo(painParts = setOf(BodyPart.LEG)).isEmpty)
    }

    @Test
    fun `글만 있어도 비어 있지 않다`() {
        assertFalse(WorkoutMemo(text = "메모").isEmpty)
    }

    @Test
    fun `글이 공백뿐이면 비어 있다`() {
        assertTrue(WorkoutMemo(text = "   ").isEmpty)
    }
}
