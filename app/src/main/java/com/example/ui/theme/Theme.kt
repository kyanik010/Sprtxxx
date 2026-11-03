package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val MatchNightColorScheme = darkColorScheme(
    primary = CalmGold,
    onPrimary = NightNavy,
    primaryContainer = DarkSportsBlue,
    onPrimaryContainer = CalmGoldVariant,
    secondary = SportsGreen,
    onSecondary = NightNavy,
    tertiary = LiveRed,
    onTertiary = SnowWhite,
    background = NightNavy,
    onBackground = SnowWhite,
    surface = DarkSportsBlue,
    onSurface = SnowWhite,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextMuted,
    outline = CardBorder,
    outlineVariant = CalmGold
)

@Composable
fun MatchNightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force RTL layout direction for pristine native Arabic IPTV experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = MatchNightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
