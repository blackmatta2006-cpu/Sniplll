package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = OrangeDark,
    onPrimaryContainer = Color.White,
    secondary = OrangeAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF4E2600),
    onSecondaryContainer = Color(0xFFFFD180),
    tertiary = NetworkCyan,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = SurfaceCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF2C2420),
    onSurfaceVariant = TextSecondaryDark,
    error = CutRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = OrangeContainerLight,
    onPrimaryContainer = OrangeDark,
    secondary = OrangeDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFECB3),
    onSecondaryContainer = Color(0xFF5D4037),
    tertiary = ElectricBlue,
    onTertiary = Color.White,
    background = WarmBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceCardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFFFF0E6),
    onSurfaceVariant = TextSecondaryLight,
    error = CutRedDark,
    onError = Color.White
)

@Composable
fun ShreyNetworkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
