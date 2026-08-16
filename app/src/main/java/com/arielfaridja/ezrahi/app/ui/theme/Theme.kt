package com.arielfaridja.ezrahi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80D8FF),
    secondary = Color(0xFFFFB74D),
    error = Color(0xFFFF5252),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006590),
    secondary = Color(0xFFE65100),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFBFDFE),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun EzrahiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
