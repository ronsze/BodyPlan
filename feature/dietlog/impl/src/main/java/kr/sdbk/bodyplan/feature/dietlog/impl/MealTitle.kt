package kr.sdbk.bodyplan.feature.dietlog.impl

/**
 * 항목 순번을 제목으로 바꾼다. 끼니 구분을 두지 않으므로 순서만 드러낸다.
 * 우리말 서수는 열까지만 따로 있고 그 뒤는 숫자로 읽는다.
 */
internal fun mealTitle(order: Int): String = ORDINALS.getOrNull(order - 1)?.let { "$it 끼니" } ?: "${order}번째 끼니"

private val ORDINALS = listOf(
    "첫째",
    "둘째",
    "셋째",
    "넷째",
    "다섯째",
    "여섯째",
    "일곱째",
    "여덟째",
    "아홉째",
    "열째",
)
