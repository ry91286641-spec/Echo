package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = NeonPink,
    tertiary = ElectricCyan,
    background = MidnightBlack,
    surface = DarkGreySurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = HighDensityText,
    onSurface = HighDensityText
)

private val LightColorScheme = lightColorScheme(
    primary = NeonPurple,
    secondary = NeonPink,
    tertiary = ElectricCyan,
    background = HighDensityBg,
    surface = HighDensityCardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = HighDensityText,
    onSurface = HighDensityText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme for EchoLive as requested: premium dark mode standard
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
