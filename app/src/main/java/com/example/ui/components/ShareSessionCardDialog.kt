package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Car
import com.example.data.model.ChargingSession
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoftBlue
import java.util.Locale

@Composable
fun ShareSessionCardDialog(
    session: ChargingSession,
    car: Car?,
    currency: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDc = session.stationType.equals("DC", ignoreCase = true)
    val deliveredKwh = session.kwhDeliveredByStation
    val addedRangeKm = (deliveredKwh / (car?.passportConsumption ?: 16.0) * 100.0).toInt()
    val earnedXp = (deliveredKwh * 3 + if (session.stationType.equals("DC", true)) 30 else 15).toInt()

    fun shareCardText(context: Context) {
        val opName = session.operatorName.ifBlank { "Электрозарядка" }
        val carName = car?.name ?: "Мой Электромобиль"
        val shareText = """
            ⚡ Зарядил свой $carName в VoltLedger!
            
            📍 Станция: $opName (${session.stationType})
            🔋 Залито энергии: +${String.format(Locale.US, "%.1f", deliveredKwh)} кВт·ч
            📈 Заряд: ${session.startSoc.toInt()}% → ${session.endSoc.toInt()}%
            🛣️ Добавлено хода: ~+$addedRangeKm км
            💰 Стоимость: ${String.format(Locale.US, "%.2f", session.totalCost)} $currency
            🏆 Опыт: +$earnedXp XP
            
            #VoltLedger #EV #ElectricVehicle #ЗарядкаЭлектромобиля
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Поделиться зарядкой")
        context.startActivity(shareIntent)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Карточка сессии",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
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
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The Social Media Share Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF1E293B),
                                    Color(0xFF0F2027)
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    ElectricCyan.copy(alpha = 0.8f),
                                    BatteryGreen.copy(alpha = 0.6f),
                                    SoftBlue.copy(alpha = 0.4f)
                                )
                            ),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header row with App branding
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(ElectricCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VOLTLEDGER",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                    color = ElectricCyan
                                )
                            }

                            // Station badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDc) Color(0xFFC084FC).copy(alpha = 0.25f) else Color(0xFF38BDF8).copy(alpha = 0.25f))
                                    .border(
                                        1.dp,
                                        if (isDc) Color(0xFFC084FC) else Color(0xFF38BDF8),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = session.stationType,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDc) Color(0xFFC084FC) else Color(0xFF38BDF8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SoftBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "⚡", fontSize = 20.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = car?.name ?: "Электромобиль",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = session.operatorName.ifBlank { "Зарядная станция" },
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Massive Delivered kWh
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .padding(vertical = 12.dp, horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "+${String.format(Locale.US, "%.1f", deliveredKwh)} кВт·ч",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "${session.startSoc.toInt()}%  ➔  ${session.endSoc.toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BatteryGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Row: Added Range, Cost, XP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(text = "Запас хода", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                Text(
                                    text = "+$addedRangeKm км",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Стоимость", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", session.totalCost)} $currency",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Награда", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                Text(
                                    text = "+$earnedXp XP 🏆",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { shareCardText(context) },
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(ElectricCyan, SoftBlue)))
                    .testTag("share_card_action_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Поделиться", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Готово")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
