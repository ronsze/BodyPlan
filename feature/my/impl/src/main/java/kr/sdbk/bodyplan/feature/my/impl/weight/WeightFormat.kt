package kr.sdbk.bodyplan.feature.my.impl.weight

import java.util.Locale

/** 저울이 0.1kg 단위로 찍으므로 소수 한 자리로 맞춘다. */
internal fun weightText(value: Double): String = String.format(Locale.US, "%.1f", value)

/**
 * 변동 값의 표기. 늘었는지 줄었는지가 크기보다 먼저 읽혀야 해 부호를 늘 붙인다.
 * 견줄 것이 없으면 0이 아니라 `-`다 — 0으로 적으면 "변화가 없었다"로 읽힌다.
 */
internal fun weightChangeText(value: Double?): String = value?.let { String.format(Locale.US, "%+.1fkg", it) } ?: "-"

/**
 * 입력칸이 받아들이는 글자만 남긴다. 정수 세 자리에 소수 한 자리까지다.
 *
 * 걸러 내지 않고 어긋난 입력을 통째로 물리는 것은, 한 글자를 지우는 도중의 문자열까지
 * 유효해야 하기 때문이다 — `72.`는 그 자체로는 값이 아니지만 지나가는 상태다.
 */
internal fun isAcceptableWeightInput(text: String): Boolean = WEIGHT_INPUT.matches(text)

private val WEIGHT_INPUT = Regex("""^\d{0,3}(\.\d?)?$""")
