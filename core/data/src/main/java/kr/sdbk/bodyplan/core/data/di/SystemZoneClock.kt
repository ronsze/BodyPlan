package kr.sdbk.bodyplan.core.data.di

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * 읽을 때마다 기기의 지금 시간대를 따르는 시계.
 *
 * `Clock.systemDefaultZone()`은 만들 때의 시간대를 붙들고 있어, 앱이 떠 있는 동안 사용자가 시간대를
 * 옮기면 "오늘"이 옛 시간대의 날짜로 남는다.
 */
internal object SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
