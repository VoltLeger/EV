package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.BatteryOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoftBlue

import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize
import coil.compose.AsyncImage

@Composable
fun VoltAvatar(
    avatarEffect: String,
    avatarIcon: String = "bolt",
    imageUri: String? = null,
    size: Dp = 56.dp,
    showGlow: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_fx")

    // Rotation angle for plasma / cyber matrix
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse scale/alpha for neon cyan / emerald
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Lightning flicker
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    val primaryColor = when (avatarEffect) {
        "plasma_purple" -> Color(0xFFC084FC) // Neon Purple
        "golden_ribbon" -> Color(0xFFFBBF24) // Warm Gold
        "emerald_glow" -> Color(0xFF10B981)  // Emerald Green
        "twilight_ring" -> Color(0xFF818CF8) // Twilight Indigo
        "lightning_spark" -> Color(0xFF38BDF8) // Lightning Sky
        "pulsar_diamond" -> Color(0xFF67E8F9) // Diamond Cyan
        else -> ElectricCyan
    }

    val secondaryColor = when (avatarEffect) {
        "plasma_purple" -> Color(0xFFE879F9)
        "golden_ribbon" -> Color(0xFFF59E0B)
        "emerald_glow" -> Color(0xFF34D399)
        "twilight_ring" -> Color(0xFFA78BFA)
        "lightning_spark" -> Color(0xFFFFFFFF)
        "pulsar_diamond" -> Color(0xFFF472B6)
        else -> SoftBlue
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        // Outer Glowing Aura Effect
        if (showGlow) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeW = (size.toPx() * 0.08f).coerceAtLeast(3f)
                val centerOffset = Offset(this.size.width / 2, this.size.height / 2)
                val radius = (this.size.minDimension / 2) - strokeW

                when (avatarEffect) {
                    "plasma_purple", "cyber_matrix", "twilight_ring" -> {
                        // Dual tone sweep rotating gradient
                        val sweepBrush = Brush.sweepGradient(
                            listOf(primaryColor, secondaryColor, primaryColor.copy(alpha = 0.2f), primaryColor)
                        )
                        drawCircle(
                            brush = sweepBrush,
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                    "golden_ribbon" -> {
                        // Golden Shimmer with orbiting highlights
                        drawCircle(
                            color = primaryColor.copy(alpha = pulseAlpha),
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = strokeW)
                        )
                    }
                    "lightning_spark" -> {
                        // Electric high-contrast pulse
                        drawCircle(
                            color = primaryColor.copy(alpha = flickerAlpha),
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = strokeW * 1.2f)
                        )
                    }
                    else -> {
                        // Neon Cyan / Emerald Breathing
                        drawCircle(
                            color = primaryColor.copy(alpha = pulseAlpha),
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = strokeW)
                        )
                    }
                }
            }
        }

        // Inner Circle Core
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size * 0.78f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f),
                            Color(0xFF0F172A).copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(primaryColor, secondaryColor)),
                    shape = CircleShape
                )
        ) {
            if (!imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Аватар Пилота",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                val iconVector = when (avatarIcon) {
                    "car" -> Icons.Default.DirectionsCar
                    "speed" -> Icons.Default.Speed
                    "leaf" -> Icons.Default.EnergySavingsLeaf
                    "star" -> Icons.Default.Star
                    "trophy" -> Icons.Default.EmojiEvents
                    "flash" -> Icons.Default.FlashOn
                    else -> Icons.Default.ElectricBolt
                }

                Icon(
                    imageVector = iconVector,
                    contentDescription = "Аватар Пилота",
                    tint = primaryColor,
                    modifier = Modifier.size(size * 0.42f)
                )
            }
        }
    }
}
