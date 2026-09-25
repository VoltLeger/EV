package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.util.AppStrings

val LocalAppStrings = staticCompositionLocalOf { AppStrings(isEn = false) }
val LocalCurrency = staticCompositionLocalOf { "BYN" }
val LocalCurrentTheme = staticCompositionLocalOf { "dark" }

// Postcard Cyber Neon Dark Scheme (The only active theme in VoltLedger)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF), // Cyber Neon Cyan (matching postcard primaryAccent)
    onPrimary = Color(0xFF0B0F19),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = Color(0xFFA855F7), // Electric Purple (matching postcard secondaryAccent)
    onSecondary = Color(0xFF0B0F19),
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = BatteryGreen, // 0xFF10B981 Emerald
    onTertiary = Color.White,
    background = Color(0xFF0B0F19), // matching postcard bgStartColor
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF162032), // matching postcard card surface
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B), // matching postcard cardBgColor
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155) // matching postcard cardBorderColor
)

@Composable
fun VoltLedgerTheme(
    themeSetting: String = "dark",
    languageSetting: String = "ru", // "ru", "en"
    currencySetting: String = "BYN",
    content: @Composable () -> Unit
) {
    // VoltLedger is locked to the dark cyber neon postcard theme
    val colorScheme = DarkColorScheme
    val appStrings = AppStrings(isEn = (languageSetting == "en"))

    CompositionLocalProvider(
        LocalAppStrings provides appStrings,
        LocalCurrency provides currencySetting,
        LocalCurrentTheme provides "dark"
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
