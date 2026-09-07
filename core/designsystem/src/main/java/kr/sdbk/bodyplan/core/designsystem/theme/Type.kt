package kr.sdbk.bodyplan.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(size: Int, weight: FontWeight, lineHeight: Int = size + 6) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

/**
 * 디자인의 크기·굵기 단계를 그대로 옮긴 것이다. 화면은 여기 없는 크기를 직접 쓰지 않는다.
 *
 * - `titleLarge` 화면 제목, `titleMedium` 월 표기, `titleSmall` 카드 제목
 * - `labelLarge` 상단 오른쪽 동작, `bodyLarge` 강조 값, `bodyMedium` 본문
 * - `bodySmall` 보조 동작, `labelMedium` 캡션, `labelSmall` 탭·배지
 */
val Typography = Typography(
    titleLarge = style(20, FontWeight.Bold),
    titleMedium = style(18, FontWeight.Bold),
    titleSmall = style(17, FontWeight.Bold),
    bodyLarge = style(16, FontWeight.Bold),
    labelLarge = style(16, FontWeight.SemiBold),
    bodyMedium = style(15, FontWeight.SemiBold),
    bodySmall = style(14, FontWeight.Medium),
    labelMedium = style(13, FontWeight.SemiBold),
    labelSmall = style(12, FontWeight.Medium),
)
