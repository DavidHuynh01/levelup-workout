package com.davidhuynh.levelup.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Gym-floor palette: near-black surfaces with a single bright accent for progress. */
private val Lime = Color(0xFF7AE582)
private val LimeDark = Color(0xFF34A853)
private val Ink = Color(0xFF12131A)
private val InkElevated = Color(0xFF1C1E27)
private val Smoke = Color(0xFF2A2D3A)
private val Cloud = Color(0xFFF5F6FA)
private val Slate = Color(0xFF6B7280)
private val Flame = Color(0xFFFF7043)
private val Crimson = Color(0xFFE5484D)

val StreakOrange = Flame
val AccentLime = Lime

private val DarkScheme = darkColorScheme(
    primary = Lime,
    onPrimary = Ink,
    primaryContainer = LimeDark,
    onPrimaryContainer = Cloud,
    secondary = Flame,
    onSecondary = Ink,
    background = Ink,
    onBackground = Cloud,
    surface = InkElevated,
    onSurface = Cloud,
    surfaceVariant = Smoke,
    onSurfaceVariant = Color(0xFFB6BAC7),
    outline = Color(0xFF3A3E4D),
    error = Crimson,
    onError = Color.White,
)

private val LightScheme = lightColorScheme(
    primary = LimeDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F5DA),
    onPrimaryContainer = Color(0xFF0B2E14),
    secondary = Flame,
    onSecondary = Color.White,
    background = Cloud,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EAF0),
    onSurfaceVariant = Slate,
    outline = Color(0xFFCBD0DB),
    error = Crimson,
    onError = Color.White,
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun LevelUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = scheme.background.toArgb()
            window.navigationBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content,
    )
}

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
