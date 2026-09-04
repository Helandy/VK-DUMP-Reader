package com.etozhesandy.redpanda.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PandaRed,
    onPrimary = PandaWhite,
    secondary = PandaRedDark,
    background = PandaSurfaceLight,
    surface = PandaSurfaceLight,
    onBackground = PandaBlack,
    onSurface = PandaBlack,
    surfaceVariant = PandaGrey,
)

private val DarkColors = darkColorScheme(
    primary = PandaRed,
    onPrimary = PandaBlack,
    secondary = PandaRedDark,
    background = PandaSurfaceDark,
    surface = PandaSurfaceDark,
    onBackground = PandaWhite,
    onSurface = PandaWhite,
    surfaceVariant = PandaGrey,
)

@Composable
fun RedPandaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RedPandaTypography,
        content = content,
    )
}
