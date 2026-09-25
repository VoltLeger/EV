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

fun formatDate(timeMillis: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

fun formatShortDate(timeMillis: Long): String {
    val sdf = SimpleDateFormat("dd.MM", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

/**
 * Atmospheric background that emits soft ambient light beneath translucent liquid glass surfaces.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgBase = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBase)
            .drawBehind {
                // Postcard Cyber Neon background vertical gradient (#0B0F19 to #131B2E)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B0F19),
                            Color(0xFF0E1524),
                            Color(0xFF131B2E)
                        )
                    )
                )

                // Atmospheric glowing neon cyan circle (top right, matching postcard)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.14f),
                        radius = size.width * 0.80f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.14f),
                    radius = size.width * 0.80f
                )

                // Atmospheric glowing electric purple circle (mid-left, matching postcard)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFA855F7).copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.65f),
                        radius = size.width * 0.70f
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.65f),
                    radius = size.width * 0.70f
                )

                // Subtle emerald green / cyan ambient glow at bottom
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
    ) {
        content()
    }
}

/**
 * Modern Liquid Glass Card matching the Postcard styling:
 * Frosted dark obsidian container with a subtle neon specular gradient border.
 */
@Composable
fun VoltCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color? = null,
    cornerRadius: Dp = 22.dp,
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    // Specular neon glass gradient border matching the postcard
    val glassBorderBrush = if (borderColor != null) {
        Brush.linearGradient(
            colors = listOf(
                GlassBorderTop,
                borderColor,
                borderColor.copy(alpha = 0.4f),
                GlassBorderBottom
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF00E5FF).copy(alpha = 0.65f),
                Color(0xFFA855F7).copy(alpha = 0.45f),
                Color(0xFF334155)
            ),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor,
                        surfaceVariant.copy(alpha = 0.88f)
                    )
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
                            GlassWhiteHigh.copy(alpha = 0.45f),
                            GlassBorderCyan.copy(alpha = 0.3f),
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

@Composable
fun StationTypeBadge(type: String) {
    val isDc = type.equals("DC", ignoreCase = true)
    val accentColor = if (isDc) ElectricCyan else BatteryGreen
    val bgColor = accentColor.copy(alpha = 0.16f)
    val borderColor = accentColor.copy(alpha = 0.45f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(GlassBorderTop, borderColor, GlassBorderBottom)
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 9.dp, vertical = 3.5.dp)
    ) {
        Text(
            text = type.uppercase(),
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun TagBadge(name: String, color: Long) {
    if (name.isBlank()) return
    val tagColor = Color(color)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tagColor.copy(alpha = 0.16f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(GlassBorderTop, tagColor.copy(alpha = 0.5f), GlassBorderBottom)
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 9.dp, vertical = 3.5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(tagColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                color = tagColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String = "",
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = SoftBlue
) {
    VoltCard(
        modifier = modifier,
        borderColor = accentColor.copy(alpha = 0.45f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = unit,
                    fontSize = 14.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Sleek liquid glass primary action button with specular rim glint.
 */
@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentGradient: List<Color> = listOf(ElectricCyan, SoftBlue)
) {
    val shape = RoundedCornerShape(16.dp)

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Gray.copy(alpha = 0.2f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 1.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(accentGradient)
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.15f)
                    )
                ),
                shape = shape
            )
            .drawBehind {
                // Top rim specular highlight
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(20f, 1f),
                    end = Offset(size.width - 20f, 1f),
                    strokeWidth = 2f
                )
            }
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 0.3.sp
        )
    }
}
