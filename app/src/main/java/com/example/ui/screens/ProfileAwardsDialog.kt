package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Award
import com.example.ui.components.VoltAvatar
import com.example.ui.components.VoltCard
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.BatteryOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoftBlue
import com.example.ui.viewmodel.VoltViewModel
import java.util.Locale

@Composable
fun ProfileAwardsDialog(
    viewModel: VoltViewModel,
    onDismiss: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val awards by viewModel.awards.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()
    val rankTier by viewModel.rankTier.collectAsState()
    val activeCar by viewModel.activeCar.collectAsState()
    val context = LocalContext.current

    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember(profile.displayName) { mutableStateOf(profile.displayName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Профиль Пилота и Награды",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Pilot Hero Card
                    item {
                        VoltCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = ElectricCyan.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    VoltAvatar(
                                        avatarEffect = profile.avatarEffect,
                                        avatarIcon = profile.avatarIcon,
                                        imageUri = activeCar?.photoUri,
                                        size = 72.dp,
                                        showGlow = true
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = profile.callsign,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ElectricCyan
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(SoftBlue.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Lvl ${rankTier.second}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SoftBlue
                                                )
                                            }
                                        }

                                        if (isEditingName) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                OutlinedTextField(
                                                    value = tempName,
                                                    onValueChange = { tempName = it },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = ElectricCyan,
                                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                                    )
                                                )
                                                IconButton(onClick = {
                                                    viewModel.updateUserProfile(displayName = tempName)
                                                    isEditingName = false
                                                }) {
                                                    Icon(Icons.Default.Check, contentDescription = "Сохранить", tint = BatteryGreen)
                                                }
                                            }
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable { isEditingName = true }
                                            ) {
                                                Text(
                                                    text = profile.displayName,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Редактировать имя",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = rankTier.first,
                                            fontSize = 12.sp,
                                            color = BatteryOrange,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // XP Bar
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Опыт пилота", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "$totalXp XP",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val nextLevelXp = when (rankTier.second) {
                                        1 -> 500f
                                        2 -> 1500f
                                        3 -> 5000f
                                        else -> 10000f
                                    }
                                    LinearProgressIndicator(
                                        progress = { (totalXp.toFloat() / nextLevelXp).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = ElectricCyan,
                                        trackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Pilot ID / Global Rating Sync Token
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Pilot ID", profile.id)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Pilot ID скопирован в буфер", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ID Пилота (для глобального рейтинга):",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = profile.id,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = ElectricCyan
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Скопировать",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 1.5. Telegram Community & News
                    item {
                        VoltCard(
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://t.me/VoltLeger")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = Color(0xFF229ED9).copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF229ED9).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Telegram",
                                            tint = Color(0xFF229ED9),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Telegram-канал проекта",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Новости, обновления и сообщество: @VoltLeger",
                                            fontSize = 11.sp,
                                            color = Color(0xFF229ED9)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Перейти",
                                    tint = Color(0xFF229ED9),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // 2. Global Leaderboard Opt-In
                    item {
                        VoltCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = BatteryGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Глобальный рейтинг EV",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (profile.isLeaderboardOptIn) "Готов к синхронизации с облаком" else "Выключено",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = profile.isLeaderboardOptIn,
                                    onCheckedChange = { viewModel.updateUserProfile(isLeaderboardOptIn = it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BatteryGreen,
                                        checkedTrackColor = BatteryGreen.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }

                    // 3. Customize Avatar Visual Effect
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Эффект Аватара (из полученных наград)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val availableEffects = listOf(
                                Triple("neon_cyan", "Неоновый Циан", true),
                                Triple("plasma_purple", "Плазменная Аура", awards.any { it.avatarEffectReward == "plasma_purple" && it.isUnlocked }),
                                Triple("emerald_glow", "Изумрудный Ореол", awards.any { it.avatarEffectReward == "emerald_glow" && it.isUnlocked }),
                                Triple("golden_ribbon", "Золотой Ранг", awards.any { it.avatarEffectReward == "golden_ribbon" && it.isUnlocked }),
                                Triple("twilight_ring", "Сумеречное Кольцо", awards.any { it.avatarEffectReward == "twilight_ring" && it.isUnlocked }),
                                Triple("lightning_spark", "Кибер-Молния", awards.any { it.avatarEffectReward == "lightning_spark" && it.isUnlocked }),
                                Triple("cyber_matrix", "Кибер-Матрица", awards.any { it.avatarEffectReward == "cyber_matrix" && it.isUnlocked }),
                                Triple("pulsar_diamond", "Алмазный Пульсар", awards.any { it.avatarEffectReward == "pulsar_diamond" && it.isUnlocked })
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(availableEffects) { (effectId, effectTitle, isAvailable) ->
                                    val isSelected = profile.avatarEffect == effectId
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) ElectricCyan.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable(enabled = isAvailable) {
                                                viewModel.updateUserProfile(avatarEffect = effectId)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (!isAvailable) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Заблокировано",
                                                    tint = Color.White.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = effectTitle,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isAvailable) {
                                                    if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurface
                                                } else Color.White.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Customize Avatar Icon
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Символ Аватара",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val icons = listOf(
                                "bolt" to Icons.Default.ElectricBolt,
                                "car" to Icons.Default.DirectionsCar,
                                "speed" to Icons.Default.Speed,
                                "leaf" to Icons.Default.EnergySavingsLeaf,
                                "star" to Icons.Default.Star,
                                "trophy" to Icons.Default.EmojiEvents,
                                "flash" to Icons.Default.FlashOn
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                icons.forEach { (iconKey, vector) ->
                                    val isSelected = profile.avatarIcon == iconKey
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) ElectricCyan.copy(alpha = 0.25f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.1f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                viewModel.updateUserProfile(avatarIcon = iconKey)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = vector,
                                            contentDescription = iconKey,
                                            tint = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Awards Showcase Section Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Зал Славы и Награды",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val unlockedCount = awards.count { it.isUnlocked }
                            Text(
                                text = "$unlockedCount из ${awards.size} открыто",
                                fontSize = 12.sp,
                                color = if (unlockedCount > 0) BatteryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 6. Awards List
                    items(awards) { award ->
                        AwardItemCard(award = award)
                    }
                }
            }
        }
    }
}

@Composable
fun AwardItemCard(award: Award) {
    val isUnlocked = award.isUnlocked

    VoltCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isUnlocked) BatteryGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isUnlocked) BatteryGreen.copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isUnlocked) BatteryGreen.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (award.iconKey) {
                        "speed" -> Icons.Default.Speed
                        "moon" -> Icons.Default.NightsStay
                        "flash" -> Icons.Default.FlashOn
                        "leaf" -> Icons.Default.EnergySavingsLeaf
                        "trophy" -> Icons.Default.EmojiEvents
                        "station" -> Icons.Default.Stars
                        else -> Icons.Default.ElectricBolt
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isUnlocked) BatteryGreen else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = award.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.7f)
                        )

                        if (isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BatteryGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Получено",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BatteryGreen
                                )
                            }
                        } else {
                            Text(
                                text = "+${award.xpReward} XP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SoftBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = award.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar & Unlocked reward note
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Прогресс: ${String.format(Locale.US, "%.1f", award.currentProgress)} / ${String.format(Locale.US, "%.0f", award.maxProgress)} ${award.unit}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(award.progressPercent * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) BatteryGreen else ElectricCyan
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { award.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isUnlocked) BatteryGreen else ElectricCyan,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = if (isUnlocked) ElectricCyan else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Награда: Эффект аватара «${award.avatarEffectName}»",
                        fontSize = 10.sp,
                        color = if (isUnlocked) ElectricCyan else Color.White.copy(alpha = 0.4f),
                        fontWeight = if (isUnlocked) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
