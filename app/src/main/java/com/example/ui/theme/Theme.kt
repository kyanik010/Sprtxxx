package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val SonixColorScheme = darkColorScheme(
    primary = CalmGold,
    onPrimary = NightNavy,
    primaryContainer = DarkSportsBlue,
    onPrimaryContainer = CalmGoldVariant,
    secondary = CalmGoldVariant,
    onSecondary = NightNavy,
    tertiary = LiveRed,
    onTertiary = SnowWhite,
    background = NightNavy,
    onBackground = SnowWhite,
    surface = CardBackground,
    onSurface = SnowWhite,
    surfaceVariant = DarkSportsBlue,
    onSurfaceVariant = TextMuted,
    outline = CardBorder,
    outlineVariant = CalmGold.copy(alpha = 0.55f)
)

@Composable
fun MatchNightTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = SonixColorScheme,
            typography = Typography,
            content = content
        )
    }
}
