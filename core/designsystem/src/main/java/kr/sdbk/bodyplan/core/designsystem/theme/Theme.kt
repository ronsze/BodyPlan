package kr.sdbk.bodyplan.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 디자인이 밝은 화면 하나만 정의하므로 색을 고정한다.
 * 시스템 테마나 기기 색을 따라가지 않는다 — 따라가면 카드와 바탕의 대비가 무너진다.
 */
private val ColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentSurface,
    onPrimaryContainer = Accent,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = BorderStrong,
    error = Danger,
    onError = OnAccent,
)

@Composable
fun BodyPlanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        content = content,
    )
}
