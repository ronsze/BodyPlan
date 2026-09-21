package kr.sdbk.bodyplan.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// 화면 바탕과 면. 카드가 바탕 위에 떠 보이도록 바탕은 흰색보다 어둡다.
val Background = Color(0xFFF2F4F6)
val Surface = Color(0xFFFFFFFF)
val SurfaceMuted = Color(0xFFF9FAFC)

// 강조. AccentSurface는 강조색 위에 얹는 옅은 배경이다.
val Accent = Color(0xFF3182F6)
val AccentSurface = Color(0xFFE8F3FF)
val OnAccent = Color(0xFFFFFFFF)

// 글자. 중요도가 낮아질수록 옅어진다.
val TextPrimary = Color(0xFF191F28)
val TextSecondary = Color(0xFF4E5968)
val TextTertiary = Color(0xFF8B95A1)
val TextDisabled = Color(0xFF909AAB)

// 선과 비활성 면.
val Border = Color(0xFFE5E8EB)
val BorderStrong = Color(0xFFD1D6DB)
val Disabled = Color(0xFFD1D6DB)

/** 일요일 날짜와 위험한 동작. */
val Danger = Color(0xFFFF453A)

// 운동 부위 구분 색. 캘린더 셀의 점과 부위 라벨이 쓴다.
val PartChest = Color(0xFFE5736B)
val PartBack = Color(0xFF5B9BD5)
val PartShoulder = Color(0xFFE8A33D)
val PartLeg = Color(0xFF5FA86B)
val PartBiceps = Color(0xFF9B72C7)
val PartTriceps = Color(0xFF3FA9A0)
val PartCardio = Color(0xFFE8734A)

// AI 제공자를 가리키는 색. 부위 색과 같은 자리에 둔다.
val AiClaude = Color(0xFFD97757)
val AiGpt = Color(0xFF10A37F)
val AiGemini = Color(0xFF4285F4)
val AiOnDevice = Color(0xFF7E57C2)
