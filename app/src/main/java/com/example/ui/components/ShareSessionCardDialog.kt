package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Car
import com.example.data.model.ChargingSession
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoftBlue
import com.example.util.PostcardGenerator
import java.util.Locale

@Composable
fun ShareSessionCardDialog(
    session: ChargingSession,
    car: Car?,
    currency: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedThemeIndex by remember { mutableIntStateOf(0) }
    var selectedQuote by remember { mutableStateOf(PostcardGenerator.PRESET_QUOTES[0]) }

    val currentTheme = PostcardGenerator.THEMES[selectedThemeIndex]
    val isDc = session.stationType.equals("DC", ignoreCase = true)
    val isHome = session.operatorName.contains("Дом", ignoreCase = true)
    val deliveredKwh = session.kwhDeliveredByStation
    val passportConsumption = car?.passportConsumption?.takeIf { it > 5.0 } ?: 16.0
    val addedRangeKm = ((deliveredKwh / passportConsumption) * 100.0).toInt().coerceAtLeast(0)
    val co2SavedKg = String.format(Locale.US, "%.1f", deliveredKwh * 0.52)
    val earnedXp = (deliveredKwh * 3 + if (isDc) 30 else 15).toInt()

    val primaryAccentCompose = Color(currentTheme.primaryAccent)
    val secondaryAccentCompose = Color(currentTheme.secondaryAccent)
    val bgStartCompose = Color(currentTheme.bgStartColor)
    val bgEndCompose = Color(currentTheme.bgEndColor)
    val cardBgCompose = Color(currentTheme.cardBgColor)
    val cardBorderCompose = Color(currentTheme.cardBorderColor)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Открытка для соцсетей",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Theme Selector Chips
                Text(
                    text = "Выберите стиль открытки:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(PostcardGenerator.THEMES) { index, theme ->
                        val isSelected = selectedThemeIndex == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedThemeIndex = index },
                            label = { Text(theme.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(theme.primaryAccent).copy(alpha = 0.25f),
                                selectedLabelColor = Color(theme.primaryAccent)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // The Social Media Card (Live Preview matching the chosen theme)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(bgStartCompose, bgEndCompose)
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    primaryAccentCompose.copy(alpha = 0.85f),
                                    secondaryAccentCompose.copy(alpha = 0.65f),
                                    cardBorderCompose
                                )
                            ),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header row with App branding & station badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(primaryAccentCompose.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = primaryAccentCompose,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VOLTLEDGER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp,
                                    color = primaryAccentCompose
                                )
                            }

                            // Station badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(secondaryAccentCompose.copy(alpha = 0.22f))
                                    .border(1.dp, secondaryAccentCompose, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isHome) "HOME" else session.stationType,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = secondaryAccentCompose
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Car Info with photo / avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!car?.photoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = car?.photoUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBgCompose)
                                        .border(1.dp, cardBorderCompose, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "⚡", fontSize = 18.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = car?.name ?: "Мой Электромобиль",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = session.operatorName.ifBlank { "Зарядная станция" },
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Highlight Energy Box & Battery Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBgCompose.copy(alpha = 0.75f))
                                .border(1.dp, cardBorderCompose, RoundedCornerShape(16.dp))
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "+${String.format(Locale.US, "%.1f", deliveredKwh)} кВт·ч",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = primaryAccentCompose
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Battery progress bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                ) {
                                    val startRatio = (session.startSoc / 100.0).coerceIn(0.0, 1.0).toFloat()
                                    val endRatio = (session.endSoc / 100.0).coerceIn(0.0, 1.0).toFloat()
                                    val fillRatio = (endRatio - startRatio).coerceAtLeast(0.05f)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = endRatio)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(primaryAccentCompose, secondaryAccentCompose)
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${session.startSoc.toInt()}%  ➔  ${session.endSoc.toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4 Key Stats (2x2 Grid)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Stat 1: Added Range
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBgCompose.copy(alpha = 0.7f))
                                    .border(1.dp, cardBorderCompose, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("🛣️ ЗАПАС ХОДА", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                                    Text("+$addedRangeKm км", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = primaryAccentCompose)
                                }
                            }

                            // Stat 2: Total Cost
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBgCompose.copy(alpha = 0.7f))
                                    .border(1.dp, cardBorderCompose, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("💰 СТОИМОСТЬ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                                    Text("${String.format(Locale.US, "%.2f", session.totalCost)} $currency", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Stat 3: CO2 Saved
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBgCompose.copy(alpha = 0.7f))
                                    .border(1.dp, cardBorderCompose, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("🌱 СБЕРЕЖЕНО", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                                    Text("-$co2SavedKg кг CO₂", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = secondaryAccentCompose)
                                }
                            }

                            // Stat 4: XP Earned
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBgCompose.copy(alpha = 0.7f))
                                    .border(1.dp, cardBorderCompose, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("🏆 НАГРАДА", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                                    Text("+$earnedXp XP", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFBBF24))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Motto quote box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(primaryAccentCompose.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "\"$selectedQuote\"",
                                fontSize = 10.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quote selector
                Text(
                    text = "Цитата / Девиз на открытке:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(PostcardGenerator.PRESET_QUOTES) { index, quote ->
                        val isQuoteSelected = selectedQuote == quote
                        FilterChip(
                            selected = isQuoteSelected,
                            onClick = { selectedQuote = quote },
                            label = { Text("Цитата #${index + 1}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                selectedLabelColor = ElectricCyan
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    PostcardGenerator.shareCardImage(
                        context = context,
                        session = session,
                        car = car,
                        currency = currency,
                        themeIndex = selectedThemeIndex,
                        quoteText = selectedQuote
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(primaryAccentCompose, secondaryAccentCompose)))
                    .testTag("share_card_image_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Открытка (Картинка)", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    PostcardGenerator.shareCardText(
                        context = context,
                        session = session,
                        car = car,
                        currency = currency,
                        quoteText = selectedQuote
                    )
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Текстом", fontSize = 12.sp)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
