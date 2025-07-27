package ru.dumch.spaced.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkMode()
    val colorScheme = if (isDark) DarkColors else LightColors

    MaterialTheme(colorScheme = colorScheme) {
        Surface( // ⬅️ This applies the dark background
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color.Black,
    surface = Color(0xFFFFFFFF),
    onSurface = Color.Black,
)