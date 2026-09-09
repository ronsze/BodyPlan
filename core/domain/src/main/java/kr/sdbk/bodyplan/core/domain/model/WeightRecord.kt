package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate

/**
 * 사용자가 손수 넣은 하루치 체중.
 *
 * 하루에 한 값만 남는다 — 같은 날짜에 다시 넣으면 덮어쓴다.
 * 인바디가 사진에서 읽은 추정값과 섞지 않는다. 추정값이 섞이면 하루 차이가 실제 변화인지
 * 읽기 오차인지 구분할 수 없다.
 */
data class WeightRecord(val date: LocalDate, val weightKg: Double)
