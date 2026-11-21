package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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

private val DarkColorScheme = darkColorScheme(
    primary = FekrDarkPrimary,
    onPrimary = FekrDarkOnPrimary,
    primaryContainer = FekrDarkPrimaryContainer,
    onPrimaryContainer = FekrSageContainer,
    secondary = FekrDarkSecondary,
    onSecondary = FekrOnHeroRed,
    secondaryContainer = FekrDarkSecondaryContainer,
    onSecondaryContainer = FekrHeroRedContainer,
    tertiary = FekrAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5A3700),
    onTertiaryContainer = FekrAmberContainer,
    background = FekrDarkBackground,
    onBackground = Color(0xFFE3E4DC),
    surface = FekrDarkSurface,
    onSurface = Color(0xFFE3E4DC),
    surfaceVariant = FekrDarkSurfaceContainer,
    onSurfaceVariant = Color(0xFFC4C8BB),
    surfaceContainerLow = Color(0xFF1A1F16),
    surfaceContainer = FekrDarkSurfaceContainer,
    surfaceContainerHigh = Color(0xFF2B3125),
    outline = Color(0xFF8E9286),
    outlineVariant = Color(0xFF44483D)
)

private val LightColorScheme = lightColorScheme(
    primary = FekrForestGreen,
    onPrimary = Color.White,
    primaryContainer = FekrSageContainer,
    onPrimaryContainer = FekrOnSage,
    secondary = FekrHeroRed,
    onSecondary = Color.White,
    secondaryContainer = FekrHeroRedContainer,
    onSecondaryContainer = FekrOnHeroRed,
    tertiary = FekrAmber,
    onTertiary = Color.White,
    tertiaryContainer = FekrAmberContainer,
    onTertiaryContainer = FekrOnAmber,
    background = FekrCreamBackground,
    onBackground = FekrTextPrimary,
    surface = FekrCreamBackground,
    onSurface = FekrTextPrimary,
    surfaceVariant = FekrSurfaceContainer,
    onSurfaceVariant = FekrTextSecondary,
    surfaceContainerLow = FekrSurfaceLow,
    surfaceContainer = FekrSurfaceContainer,
    surfaceContainerHigh = FekrSurfaceHigh,
    outline = FekrOutline,
    outlineVariant = FekrOutlineVariant
)

val FekrShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FekrCenterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = FekrShapes,
            content = content
        )
    }
}
