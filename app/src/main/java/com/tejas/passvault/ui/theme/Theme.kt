package com.tejas.passvault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultPurple = Color(0xFF7C4DFF)
private val VaultPurpleDark = Color(0xFF4527A0)
private val VaultBackground = Color(0xFF1B1035)

private val DarkColors = darkColorScheme(
    primary = VaultPurple,
    onPrimary = Color.White,
    primaryContainer = VaultPurpleDark,
    onPrimaryContainer = Color.White,
    secondary = VaultPurpleDark,
    onSecondary = Color.White,
    background = VaultBackground,
    onBackground = Color.White,
    surface = Color(0xFF241542),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF352454),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFE53935),
    onError = Color.White,
    errorContainer = Color(0xFF5C1A18),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColors = lightColorScheme(
    primary = VaultPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = VaultPurpleDark,
    onSecondary = Color.White,
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

@Composable
fun PassVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
