package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark mode colors
private val DarkObsidian = Color(0xFF0B1020)
private val DarkNavy = Color(0xFF121A2B)
private val DarkText = Color(0xFFE7EEF8)
private val DarkMint = Color(0xFF3DDC97)
private val DarkCyan = Color(0xFF7FE7F0)
private val DarkPurple = Color(0xFF8B7CFF)
private val DarkError = Color(0xFFFF6B6B)
private val DarkSuccess = Color(0xFF51CF66)

// Light mode colors
private val LightPearl = Color(0xFFF7F4EF)
private val LightInk = Color(0xFF1A1714)
private val LightMint = Color(0xFF1F8A62)
private val LightCyan = Color(0xFF0099CC)
private val LightPurple = Color(0xFF6E62C9)
private val LightError = Color(0xFFE63946)
private val LightSuccess = Color(0xFF06A77D)

private val DarkColorScheme = darkColorScheme(
    primary = DarkMint,
    secondary = DarkCyan,
    tertiary = DarkPurple,
    background = DarkObsidian,
    surface = DarkNavy,
    error = DarkError,
    onPrimary = DarkObsidian,
    onSecondary = DarkObsidian,
    onTertiary = DarkObsidian,
    onBackground = DarkText,
    onSurface = DarkText,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightMint,
    secondary = LightCyan,
    tertiary = LightPurple,
    background = LightPearl,
    surface = Color.White,
    error = LightError,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightInk,
    onSurface = LightInk,
    onError = Color.White
)

@Composable
fun DeproofTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
