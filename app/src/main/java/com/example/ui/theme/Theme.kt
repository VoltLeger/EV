package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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

// 1. Postcard Cyber Neon Dark Scheme (Фирменная тёмная)
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

// 2. Light Theme (Светлая тема: чистая, высококонтрастная, стеклоподобная)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F), // Deep Cyan
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF7C3AED), // Vibrant Violet
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = Color(0xFF059669), // Emerald Green
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9), // Slate 100
    onBackground = Color(0xFF0F172A), // Slate 900
    surface = Color(0xFFFFFFFF), // Pure White Card
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0), // Slate 200
    onSurfaceVariant = Color(0xFF475569), // Slate 600
    outline = Color(0xFFCBD5E1) // Slate 300
)

// 3. Postcard Color Themes (Цветные визитки после зарядки):

// 3a. Эко Изумруд (Eco Emerald - Визитка #1)
private val EcoEmeraldColorScheme = darkColorScheme(
    primary = Color(0xFF10B981), // Emerald Green
    onPrimary = Color(0xFF042017),
    primaryContainer = Color(0xFF0F3E30),
    onPrimaryContainer = Color(0xFFE6FFFA),
    secondary = Color(0xFF06D6A0), // Mint Accent
    onSecondary = Color(0xFF042017),
    secondaryContainer = Color(0xFF1A5D49),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF042017),
    background = Color(0xFF042017), // Postcard bgStart
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0A2E22), // Postcard card surface
    onSurface = Color(0xFFF0FDF4),
    surfaceVariant = Color(0xFF0F3E30), // Postcard cardBgColor
    onSurfaceVariant = Color(0xFF6EE7B7),
    outline = Color(0xFF1A5D49) // Postcard cardBorderColor
)

// 3b. Закатный Драйв (Sunset Drive - Визитка #2)
private val SunsetDriveColorScheme = darkColorScheme(
    primary = Color(0xFFF43F5E), // Coral Rose
    onPrimary = Color(0xFF1E0B24),
    primaryContainer = Color(0xFF311833),
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = Color(0xFFF59E0B), // Amber Gold
    onSecondary = Color(0xFF1E0B24),
    secondaryContainer = Color(0xFF502753),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFFFB7185),
    onTertiary = Color.White,
    background = Color(0xFF1E0B24), // Postcard bgStart
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF28112E),
    onSurface = Color(0xFFFFF1F2),
    surfaceVariant = Color(0xFF311833), // Postcard cardBgColor
    onSurfaceVariant = Color(0xFFFDA4AF),
    outline = Color(0xFF502753) // Postcard cardBorderColor
)

// 3c. Космос (Deep Space - Визитка #3)
private val CosmicColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8), // Sky Blue
    onPrimary = Color(0xFF070B1E),
    primaryContainer = Color(0xFF162044),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF818CF8), // Indigo
    onSecondary = Color(0xFF070B1E),
    secondaryContainer = Color(0xFF26376E),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color.White,
    background = Color(0xFF070B1E), // Postcard bgStart
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0F1735),
    onSurface = Color(0xFFF0F9FF),
    surfaceVariant = Color(0xFF162044), // Postcard cardBgColor
    onSurfaceVariant = Color(0xFF93C5FD),
    outline = Color(0xFF26376E) // Postcard cardBorderColor
)

@Composable
fun VoltLedgerTheme(
    themeSetting: String = "dark",
    languageSetting: String = "ru", // "ru", "en"
    currencySetting: String = "BYN",
    content: @Composable () -> Unit
) {
    val normalizedTheme = themeSetting.lowercase().trim()
    val isSystemDark = isSystemInDarkTheme()

    val effectiveThemeKey = when (normalizedTheme) {
        "light" -> "light"
        "dark", "cyber_neon" -> "dark"
        "eco", "emerald", "postcard_emerald" -> "eco"
        "sunset", "postcard_sunset" -> "sunset"
        "cosmic", "space", "postcard_space" -> "cosmic"
        "system" -> if (isSystemDark) "dark" else "light"
        else -> "dark"
    }

    val colorScheme: ColorScheme = when (effectiveThemeKey) {
        "light" -> LightColorScheme
        "eco" -> EcoEmeraldColorScheme
        "sunset" -> SunsetDriveColorScheme
        "cosmic" -> CosmicColorScheme
        else -> DarkColorScheme
    }

    val appStrings = AppStrings(isEn = (languageSetting == "en"))

    CompositionLocalProvider(
        LocalAppStrings provides appStrings,
        LocalCurrency provides currencySetting,
        LocalCurrentTheme provides effectiveThemeKey
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
