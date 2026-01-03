package com.example.kisskh.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    background = BackgroundColor,
    surface = BackgroundColor, // Cards often blend in
    onPrimary = White,
    onBackground = OnBackground,
    onSurface = OnSurface
)

@Composable
fun KissKhTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        // typography = Typography, // Defaulting for now
        content = content
    )
}
