package kr.sdbk.bodyplan.feature.home.impl.home

import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolumeTrend
import kr.sdbk.bodyplan.core.domain.model.ProgressDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeTrendFormatTest {
    private fun trend(
        bodyPart: BodyPart,
        recentVolumeKg: Int,
        changeKg: Int,
        direction: ProgressDirection,
    ): BodyPartVolumeTrend = BodyPartVolumeTrend(
        bodyPart = bodyPart,
        recentVolumeKg = recentVolumeKg,
        changeKg = changeKg,
        direction = direction,
    )

    @Test
    fun `changeKg가 0이면 변화 없음이다`() {
        val text = trendChangeText(trend(BodyPart.CHEST, 100, 0, ProgressDirection.STEADY))

        assertEquals("변화 없음", text)
    }

    @Test
    fun `changeKg가 양수면 쉼표와 부호가 붙는다`() {
        val text = trendChangeText(trend(BodyPart.CHEST, 1240, 1240, ProgressDirection.IMPROVING))

        assertEquals("+1,240kg", text)
    }

    @Test
    fun `changeKg가 음수면 부호가 붙는다`() {
        val text = trendChangeText(trend(BodyPart.LEG, 0, -300, ProgressDirection.WORSENING))

        assertEquals("-300kg", text)
    }

    @Test
    fun `늘어난 부위만 있으면 늘었어요 메시지다`() {
        val trends = listOf(
            trend(BodyPart.CHEST, 100, 100, ProgressDirection.IMPROVING),
            trend(BodyPart.BACK, 100, 100, ProgressDirection.IMPROVING),
        )

        val message = volumeTrendMessage(trends)

        assertEquals("가슴·등 볼륨이 늘었어요", message)
    }

    @Test
    fun `줄어든 부위만 있으면 챙겨보세요 메시지다`() {
        val trends = listOf(trend(BodyPart.LEG, 0, -100, ProgressDirection.WORSENING))

        val message = volumeTrendMessage(trends)

        assertEquals("하체 볼륨이 줄었어요. 이번 주에 챙겨보세요", message)
    }

    @Test
    fun `늘어난 부위와 줄어든 부위가 모두 있으면 늘고 줄었어요로 묶인다`() {
        val trends = listOf(
            trend(BodyPart.CHEST, 100, 100, ProgressDirection.IMPROVING),
            trend(BodyPart.BACK, 100, 100, ProgressDirection.IMPROVING),
            trend(BodyPart.LEG, 0, -100, ProgressDirection.WORSENING),
        )

        val message = volumeTrendMessage(trends)

        assertEquals("가슴·등 볼륨이 늘고 하체 볼륨이 줄었어요", message)
    }

    @Test
    fun `전부 STEADY면 모든 부위가 지난주와 비슷해요다`() {
        val trends = listOf(
            trend(BodyPart.CHEST, 100, 0, ProgressDirection.STEADY),
            trend(BodyPart.BACK, 100, 0, ProgressDirection.STEADY),
        )

        val message = volumeTrendMessage(trends)

        assertEquals("모든 부위가 지난주와 비슷해요", message)
    }
}
