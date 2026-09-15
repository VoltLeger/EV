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

private val DarkColorScheme = darkColorScheme(
    primary = SoftBlue,
    onPrimary = Color.White,
    primaryContainer = SoftBlueDark,
    onPrimaryContainer = Color.White,
    secondary = ElectricCyan,
    onSecondary = Color(0xFF001E2E),
    tertiary = BatteryGreen,
    onTertiary = Color.White,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

private val AmoledColorScheme = darkColorScheme(
    primary = SoftBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF162544),
    onPrimaryContainer = Color.White,
    secondary = ElectricCyan,
    onSecondary = Color.Black,
    tertiary = BatteryGreen,
    onTertiary = Color.White,
    background = AmoledBg,
    onBackground = Color(0xFFFFFFFF),
    surface = AmoledSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = AmoledBorder
)

private val LightColorScheme = lightColorScheme(
    primary = SoftBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FD),
    onPrimaryContainer = Color(0xFF0F3268),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    tertiary = BatteryGreen,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Composable
fun VoltLedgerTheme(
    themeSetting: String = "dark", // "dark", "light", "system", "amoled"
    languageSetting: String = "ru", // "ru", "en"
    currencySetting: String = "BYN",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeSetting) {
        "light" -> LightColorScheme
        "amoled" -> AmoledColorScheme
        "system" -> if (systemDark) DarkColorScheme else LightColorScheme
        else -> DarkColorScheme // default dark
    }

    val appStrings = AppStrings(isEn = (languageSetting == "en"))

    CompositionLocalProvider(
        LocalAppStrings provides appStrings,
        LocalCurrency provides currencySetting
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
