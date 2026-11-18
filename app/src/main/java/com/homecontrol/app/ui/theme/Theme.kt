package com.homecontrol.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7C9BFF),
    onPrimary = Color(0xFF0B1030),
    secondary = Color(0xFF38E1C6),
    onSecondary = Color(0xFF00201B),
    tertiary = Color(0xFFFF7AC6),
    background = Color(0xFF0A0E1A),
    onBackground = Color(0xFFEAF0FF),
    surface = Color(0xFF121829),
    onSurface = Color(0xFFEAF0FF),
    surfaceVariant = Color(0xFF1B2440),
    onSurfaceVariant = Color(0xFFA9B4D6),
    outline = Color(0xFF3A4668),
    error = Color(0xFFFF6B81)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F5BFF),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0E9F8E),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFD6338F),
    background = Color(0xFFF3F5FC),
    onBackground = Color(0xFF0E1330),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1330),
    surfaceVariant = Color(0xFFE6EAF8),
    onSurfaceVariant = Color(0xFF4A5478),
    outline = Color(0xFFB4BCD8),
    error = Color(0xFFD93251)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

object AppColors {
    val success = Color(0xFF34D399)
    val warning = Color(0xFFFFC857)
}

@Composable
fun HomeControlTheme(dark: Boolean, content: @Composable () -> Unit) {
    // The whole UI is Persian, so the layout direction is always right-to-left.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            shapes = AppShapes,
            content = content
        )
    }
}
