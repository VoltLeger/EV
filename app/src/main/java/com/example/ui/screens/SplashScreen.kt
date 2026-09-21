package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalAppStrings
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var progressTarget by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        progressTarget = 1.0f
        delay(2000L)
        onNavigateNext()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070C18))
    ) {
        // 1. Photographic Scenic Background (Scenic EV Road Wallpaper)
        Image(
            painter = painterResource(id = R.drawable.img_voltledger_splash),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Cinematic Gradient Scrim for text legibility & glowing ambiance
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF5060B16), // Deep dark at the top for logo & title
                            Color(0xD9070E1C),
                            Color(0x33060B16), // Clear middle to show the EV car & scenery
                            Color(0x88050914),
                            Color(0xFA050914)  // Dark at bottom for loading indicator
                        )
                    )
                )
        )

        // 3. Futuristic Speed-Light Streaks at the bottom corners
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Cyan energy trail left
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color(0x6600E5FF), Color(0xCC00E5FF)),
                    start = Offset(0f, h * 0.78f),
                    end = Offset(w * 0.35f, h * 0.84f)
                ),
                start = Offset(-20f, h * 0.77f),
                end = Offset(w * 0.35f, h * 0.84f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Cyan energy trail right
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xCC00E5FF), Color(0x4400E5FF), Color.Transparent),
                    start = Offset(w * 0.65f, h * 0.84f),
                    end = Offset(w + 20f, h * 0.79f)
                ),
                start = Offset(w * 0.65f, h * 0.84f),
                end = Offset(w + 20f, h * 0.79f),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 4. Foreground Content (Logo, Branding, Slogan, Loading)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Logo & Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp)
            ) {
                // Stylized V with lightning bolt icon mark
                VoltLogoMark(modifier = Modifier.size(80.dp))

                Spacer(modifier = Modifier.height(16.dp))

                // App Title: "Volt" in White + "Ledger" in Electric Cyan
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Volt",
                        fontSize = 35.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Ledger",
                        fontSize = 35.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElectricCyan,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle / Slogan
                Text(
                    text = strings.slogan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFB4C8E0),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    letterSpacing = 0.2.sp
                )
            }

            // Bottom Section: Progress bar & loading label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 44.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .width(170.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ElectricCyan,
                    trackColor = Color(0x3300E5FF)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.loading,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8EA4C2),
                    letterSpacing = 3.sp
                )
            }
        }
    }
}

/**
 * Geometric "V" brandmark with White left wing and glowing Electric Cyan lightning bolt right wing
 */
@Composable
private fun VoltLogoMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x3300E5FF),
                        Color(0x140B1A30),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val w = size.width
            val h = size.height

            // 1. Left wing of 'V' (Crisp White)
            val leftWing = Path().apply {
                moveTo(w * 0.16f, h * 0.18f)
                lineTo(w * 0.32f, h * 0.18f)
                lineTo(w * 0.50f, h * 0.78f)
                lineTo(w * 0.40f, h * 0.78f)
                close()
            }
            drawPath(
                path = leftWing,
                color = Color.White,
                style = Fill
            )

            // 2. Right wing of 'V': Electric Cyan Lightning Bolt
            val bolt = Path().apply {
                moveTo(w * 0.78f, h * 0.14f)
                lineTo(w * 0.46f, h * 0.52f)
                lineTo(w * 0.58f, h * 0.52f)
                lineTo(w * 0.38f, h * 0.86f)
                lineTo(w * 0.62f, h * 0.46f)
                lineTo(w * 0.50f, h * 0.46f)
                close()
            }

            // Glow stroke
            drawPath(
                path = bolt,
                color = Color(0x6600E5FF),
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Fill bolt
            drawPath(
                path = bolt,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF80F0FF), Color(0xFF00D2FF), Color(0xFF0091EA)),
                    start = Offset(w * 0.78f, h * 0.14f),
                    end = Offset(w * 0.38f, h * 0.86f)
                ),
                style = Fill
            )
        }
    }
}
