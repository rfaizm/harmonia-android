package com.rfaizm.harmoniamusic.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// GUIDELINE §2 tokens mapped onto the M3 color scheme.
private val LightColors = lightColorScheme(
    background = Color(0xFFF5F4F0),
    onBackground = Color(0xFF2C2C2C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2C2C2C),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    primary = Color(0xFF5A7A6A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFC9C3D8),
    onSecondary = Color(0xFF2C2C2C),
    surfaceVariant = Color(0xFFE8E6E0),
    onSurfaceVariant = Color(0xFF7A7870),
    tertiary = Color(0xFF3D6B7A),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFC0392B),
    outline = Color(0x265A7A6A),
    outlineVariant = Color(0x265A7A6A),
)

private val DarkColors = darkColorScheme(
    background = Color(0xFF1E2022),
    onBackground = Color(0xFFE8E6E0),
    surface = Color(0xFF272B2D),
    onSurface = Color(0xFFE8E6E0),
    surfaceContainer = Color(0xFF272B2D),
    surfaceContainerLow = Color(0xFF272B2D),
    primary = Color(0xFF7AAB8A),
    onPrimary = Color(0xFF1A2620),
    secondary = Color(0xFF4A4462),
    onSecondary = Color(0xFFE8E6E0),
    surfaceVariant = Color(0xFF2E3235),
    onSurfaceVariant = Color(0xFF9A9890),
    tertiary = Color(0xFF5A8FA0),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFE57373),
    outline = Color(0x267AAB8A),
    outlineVariant = Color(0x267AAB8A),
)

// Guideline vocabulary aliases.
val ColorScheme.card get() = surface
val ColorScheme.muted get() = surfaceVariant
val ColorScheme.mutedForeground get() = onSurfaceVariant
val ColorScheme.border get() = outline
val ColorScheme.destructive get() = error

val LikedRed = Color(0xFFE05C5C)
val PlayerBottom = Color(0xFF141618)

@Composable
fun HarmoniaTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val scheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = scheme, typography = Typography) {
        // No Surface at the root, so set the default text/icon color here (otherwise it's black).
        CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
    }
}
