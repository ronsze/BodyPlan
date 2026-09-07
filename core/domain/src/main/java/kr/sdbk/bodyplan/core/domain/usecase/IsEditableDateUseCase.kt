package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * 그 날짜의 기록을 아직 쓰거나 고칠 수 있는지 판정한다. 오늘과 어제만 허용한다.
 *
 * 자정을 넘기면 결과가 바뀌므로 호출부는 화면 진입 시점에 한 번만 부른다.
 */
class IsEditableDateUseCase
@Inject
constructor(private val clock: Clock) {
    operator fun invoke(date: LocalDate): Boolean = invoke(date, LocalDate.now(clock))

    /**
     * 기준일을 직접 받는 판정.
     * 한 번 읽은 오늘을 여러 날짜에 걸쳐 적용해야 하는 호출부가 쓴다 — 판정 도중 자정을 넘겨도 기준이 흔들리지 않는다.
     */
    operator fun invoke(date: LocalDate, today: LocalDate): Boolean = date == today || date == today.minusDays(1)
}
