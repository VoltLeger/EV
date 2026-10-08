package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorderBottom
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassWhiteHigh
import com.example.ui.theme.GlassWhiteLow
import com.example.ui.theme.GlassWhiteMid
import com.example.ui.theme.LocalCurrentTheme
import com.example.ui.theme.MintBgGradientBottom
import com.example.ui.theme.MintBgGradientMid
import com.example.ui.theme.MintBgGradientTop
import com.example.ui.theme.SoftBlue
import com.example.ui.theme.WrncAccent
import com.example.ui.theme.WrncBorder
import com.example.ui.theme.WrncBorderSpecular
import com.example.ui.theme.WrncBgGradientBottom
import com.example.ui.theme.WrncBgGradientMid
import com.example.ui.theme.WrncBgGradientTop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCurrency(amount: Double, currency: String): String {
    return if (amount <= 0.0001) {
        "0.00 $currency"
    } else {
        String.format(Locale.getDefault(), "%.2f %s", amount, currency)
    }
}

fun formatPricePerKwh(price: Double, currency: String): String {
    return if (price <= 0.0001) {
        "0 $currency"
    } else {
        String.format(Locale.getDefault(), "%.2f %s", price, currency)
    }
}

fun formatShortDate(timeMillis: Long): String {
    val sdf = SimpleDateFormat("dd.MM", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

fun formatDate(timeMillis: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

@Composable
fun StationTypeBadge(type: String, modifier: Modifier = Modifier) {
    val isDc = type.equals("DC", ignoreCase = true)
    val color = if (isDc) Color(0xFFC084FC) else Color(0xFF38BDF8)
    val label = if (isDc) "⚡ DC" else "🔌 AC"
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun TagBadge(name: String, color: Long = 0xFF38BDF8, modifier: Modifier = Modifier) {
    val badgeColor = Color(color)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.15f))
            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            color = badgeColor,
            maxLines = 1
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    accentColor: Color = ElectricCyan,
    modifier: Modifier = Modifier
) {
    VoltCard(
        modifier = modifier,
        borderColor = accentColor.copy(alpha = 0.35f),
        cornerRadius = 16.dp
    ) {
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Atmospheric background that emits soft ambient light beneath translucent liquid glass surfaces.
 * Dynamically reacts to current active theme: dark, light, or postcard color schemes (eco, sunset, cosmic).
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val currentThemeKey = LocalCurrentTheme.current
    val bgBase = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBase)
            .drawBehind {
                when (currentThemeKey) {
                    "light" -> {
                        // Soft elegant light gradient
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF8FAFC),
                                    Color(0xFFF1F5F9),
                                    Color(0xFFE2E8F0)
                                )
                            )
                        )
                        // Soft cyan ambient light top right
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.14f),
                                radius = size.width * 0.80f
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.14f),
                            radius = size.width * 0.80f
                        )
                        // Soft purple ambient light mid left
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(secondaryColor.copy(alpha = 0.09f), Color.Transparent),
                                center = Offset(size.width * 0.12f, size.height * 0.65f),
                                radius = size.width * 0.70f
                            ),
                            center = Offset(size.width * 0.12f, size.height * 0.65f),
                            radius = size.width * 0.70f
                        )
                    }
                    "eco" -> {
                        // Eco Emerald Postcard Background (#042017 -> #0B3828)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF042017),
                                    Color(0xFF082D20),
                                    Color(0xFF0B3828)
                                )
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF10B981).copy(alpha = 0.20f), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.14f),
                                radius = size.width * 0.80f
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.14f),
                            radius = size.width * 0.80f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF06D6A0).copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(size.width * 0.12f, size.height * 0.65f),
                                radius = size.width * 0.70f
                            ),
                            center = Offset(size.width * 0.12f, size.height * 0.65f),
                            radius = size.width * 0.70f
                        )
                    }
                    "sunset" -> {
                        // Sunset Drive Postcard Background (#1E0B24 -> #38112C)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E0B24),
                                    Color(0xFF2C0E28),
                                    Color(0xFF38112C)
                                )
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFF43F5E).copy(alpha = 0.22f), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.14f),
                                radius = size.width * 0.80f
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.14f),
                            radius = size.width * 0.80f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(size.width * 0.12f, size.height * 0.65f),
                                radius = size.width * 0.70f
                            ),
                            center = Offset(size.width * 0.12f, size.height * 0.65f),
                            radius = size.width * 0.70f
                        )
                    }
                    "cosmic" -> {
                        // Cosmic Deep Space Postcard Background (#070B1E -> #121A3D)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF070B1E),
                                    Color(0xFF0D132D),
                                    Color(0xFF121A3D)
                                )
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.20f), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.14f),
                                radius = size.width * 0.80f
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.14f),
                            radius = size.width * 0.80f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF818CF8).copy(alpha = 0.18f), Color.Transparent),
                                center = Offset(size.width * 0.12f, size.height * 0.65f),
                                radius = size.width * 0.70f
                            ),
                            center = Offset(size.width * 0.12f, size.height * 0.65f),
                            radius = size.width * 0.70f
                        )
                    }
                    else -> {
                        // Default Postcard Cyber Neon background vertical gradient (#0B0F19 to #131B2E)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0B0F19),
                                    Color(0xFF0E1524),
                                    Color(0xFF131B2E)
                                )
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.14f),
                                radius = size.width * 0.80f
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.14f),
                            radius = size.width * 0.80f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFA855F7).copy(alpha = 0.14f), Color.Transparent),
                                center = Offset(size.width * 0.12f, size.height * 0.65f),
                                radius = size.width * 0.70f
                            ),
                            center = Offset(size.width * 0.12f, size.height * 0.65f),
                            radius = size.width * 0.70f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF10B981).copy(alpha = 0.08f), Color.Transparent),
                                center = Offset(size.width * 0.75f, size.height * 0.95f),
                                radius = size.width * 0.65f
                            ),
                            center = Offset(size.width * 0.75f, size.height * 0.95f),
                            radius = size.width * 0.65f
                        )
                    }
                }
            }
    ) {
        content()
    }
}

/**
 * Modern Liquid Glass Card matching the Postcard styling:
 * Frosted container with specular gradient border.
 */
@Composable
fun VoltCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color? = null,
    cornerRadius: Dp = 22.dp,
    content: @Composable () -> Unit
) {
    val currentThemeKey = LocalCurrentTheme.current
    val isLight = currentThemeKey == "light"
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline

    // Specular neon/glass gradient border
    val glassBorderBrush = if (borderColor != null) {
        Brush.linearGradient(
            colors = listOf(
                if (isLight) Color.White else GlassBorderTop,
                borderColor,
                borderColor.copy(alpha = if (isLight) 0.6f else 0.4f),
                if (isLight) outlineColor else GlassBorderBottom
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    } else {
        Brush.linearGradient(
            colors = if (isLight) {
                listOf(
                    primary.copy(alpha = 0.45f),
                    secondary.copy(alpha = 0.35f),
                    outlineColor.copy(alpha = 0.5f)
                )
            } else {
                listOf(
                    primary.copy(alpha = 0.65f),
                    secondary.copy(alpha = 0.45f),
                    outlineColor
                )
            },
            start = Offset(0f, 0f),
            end = Offset(300f, 600f)
        )
    }

    val shape = RoundedCornerShape(cornerRadius)

    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLight) 1.dp else 0.dp),
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isLight) {
                        listOf(
                            surfaceColor,
                            surfaceVariant.copy(alpha = 0.65f)
                        )
                    } else {
                        listOf(
                            surfaceColor,
                            surfaceVariant.copy(alpha = 0.88f)
                        )
                    }
                )
            )
            .border(
                width = 1.2.dp,
                brush = glassBorderBrush,
                shape = shape
            )
            .drawBehind {
                // Top inner specular light glint (sheen)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isLight) Color.White.copy(alpha = 0.8f) else GlassWhiteHigh.copy(alpha = 0.45f),
                            primary.copy(alpha = if (isLight) 0.25f else 0.3f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(24f, 1.5f),
                    end = Offset(size.width - 24f, 1.5f),
                    strokeWidth = 2f
                )
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
