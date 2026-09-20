package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Car
import com.example.data.model.ChargingSession
import com.example.data.model.UserProfile
import com.example.ui.components.ActiveChargingCard
import com.example.ui.components.CarPassportDialog
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.StationTypeBadge
import com.example.ui.components.TagBadge
import com.example.ui.components.VoltAvatar
import com.example.ui.components.VoltCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalCurrency
import com.example.ui.theme.SoftBlue
import com.example.util.EVCalculator
import java.util.Locale

@Composable
fun HomeScreen(
    activeCar: Car?,
    allCars: List<Car>,
    activeSession: ChargingSession?,
    monthConsumption: Double?,
    recentSessions: List<ChargingSession>,
    onSelectCar: (Long) -> Unit,
    onAddChargeClick: () -> Unit,
    onCalculateRangeClick: () -> Unit,
    onQuickHomeCharge: ((Double, Double?) -> Unit)? = null,
    onCompleteChargeClick: (ChargingSession) -> Unit,
    onCancelActiveCharge: (ChargingSession) -> Unit,
    onNavigateToHistory: (() -> Unit)? = null,
    onUpdateSession: ((ChargingSession) -> Unit)? = null,
    onDeleteSession: ((ChargingSession) -> Unit)? = null,
    userProfile: UserProfile? = null,
    onOpenProfile: (() -> Unit)? = null,
    onUpdateCar: ((Car) -> Unit)? = null,
    onDeleteCar: ((Car) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    var showCarPassportDialog by remember { mutableStateOf(false) }

    // Session Edit & Delete state
    var editingSession by remember { mutableStateOf<ChargingSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<ChargingSession?>(null) }

    // Quick Home Charge Dialog State
    var showHomeChargeDialog by remember { mutableStateOf(false) }
    var homeChargeSocText by remember(activeCar?.currentSoc) {
        mutableStateOf(activeCar?.currentSoc?.toInt()?.toString() ?: "30")
    }
    var homeChargeMeterText by remember { mutableStateOf("") }

    val usableCapacity = activeCar?.usableCapacityKwh ?: 57.0
    val carSoc = activeCar?.currentSoc ?: 80.0

    // Compute baseline consumption
    val completedSessions = recentSessions.filter { it.status == "completed" }
    val realAvg = monthConsumption ?: run {
        val forecast = EVCalculator.calculateRangeForecast(completedSessions, usableCapacity, carSoc)
        forecast.realConsumptionPer100Km
    }

    // Range with current car SOC for top vehicle bar pill
    val currentSocRange = EVCalculator.calculateRangeForSoc(
        usableCapacityKwh = usableCapacity,
        socPercent = carSoc,
        consumptionPer100Km = realAvg
    )

    // Departure forecast range if active session exists
    val departureForecastRange = if (activeSession != null) {
        val targetSoc = if (activeSession.endSoc > carSoc) activeSession.endSoc else 90.0
        EVCalculator.calculateRangeForSoc(
            usableCapacityKwh = usableCapacity,
            socPercent = targetSoc,
            consumptionPer100Km = realAvg
        )
    } else null

    // Calculate Last Two Charges Consumption (Centerpiece 3D Card)
    val lastTwoConsumption = remember(completedSessions) {
        EVCalculator.calculateLastTwoChargesConsumption(completedSessions)
    }

    // Lower block metrics
    val lastSession = completedSessions.maxByOrNull { it.startTime }
    val costPer100Km = if (lastSession != null && lastSession.kwhDeliveredByStation > 0 && lastSession.totalCost > 0) {
        val pricePerKwh = lastSession.totalCost / lastSession.kwhDeliveredByStation
        pricePerKwh * realAvg
    } else if (monthConsumption != null && lastSession != null) {
        lastSession.pricePerKwh * monthConsumption
    } else null

    LiquidGlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. Top Vehicle Header Row
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Vehicle selector / Car Passport
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .border(
                                    1.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            SoftBlue.copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.08f)
                                        )
                                    ),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { showCarPassportDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("car_passport_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SoftBlue.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!activeCar?.photoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = activeCar.photoUri,
                                        contentDescription = "Фото ${activeCar.name}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = "Паспорт авто",
                                        tint = SoftBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeCar?.name ?: strings.currentVehicle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "📋",
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = "${activeCar?.initialOdometer?.toInt() ?: 0} км",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Top Right: Range Forecast Pill (clickable to open forecast dialog) + Avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (activeSession != null) ElectricCyan.copy(alpha = 0.18f) else BatteryGreen.copy(alpha = 0.16f))
                                    .border(
                                        1.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.45f),
                                                if (activeSession != null) ElectricCyan.copy(alpha = 0.6f) else BatteryGreen.copy(alpha = 0.5f),
                                                Color.Transparent
                                            )
                                        ),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onCalculateRangeClick() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("estimated_range_top_pill")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (activeSession != null) Icons.Default.ElectricBolt else Icons.Default.BatteryChargingFull,
                                        contentDescription = null,
                                        tint = if (activeSession != null) ElectricCyan else BatteryGreen,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (departureForecastRange != null) {
                                                "~${departureForecastRange.toInt()} км"
                                            } else {
                                                "~${currentSocRange.toInt()} км"
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (activeSession != null) ElectricCyan else BatteryGreen
                                        )
                                        Text(
                                            text = if (activeSession != null) "на выезде" else "${carSoc.toInt()}%",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (onOpenProfile != null) {
                                VoltAvatar(
                                    avatarEffect = userProfile?.avatarEffect ?: "neon_cyan",
                                    avatarIcon = userProfile?.avatarIcon ?: "bolt",
                                    imageUri = activeCar?.photoUri,
                                    size = 42.dp,
                                    onClick = onOpenProfile
                                )
                            }
                        }
                    }
                }
            }

            // 2. Centerpiece 3D Card: Average Consumption (Last 2 Charges)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Flat 3D Depth Backdrop Layer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFF003566).copy(alpha = 0.45f))
                    )

                    // Front Polished Glassmorphic 3D Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF0F172A).copy(alpha = 0.92f),
                                        Color(0xFF1E293B).copy(alpha = 0.95f),
                                        Color(0xFF0F172A).copy(alpha = 0.98f)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.55f),
                                        ElectricCyan,
                                        SoftBlue.copy(alpha = 0.5f),
                                        Color.White.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = ElectricCyan,
                                spotColor = ElectricCyan
                            )
                            .padding(vertical = 16.dp, horizontal = 18.dp)
                            .testTag("last_two_charges_card")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Top Tag: Centered Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricCyan.copy(alpha = 0.15f))
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "СРЕДНИЙ РАСХОД ЗА 2 ПОСЛЕДНИЕ ЗАРЯДКИ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ElectricCyan,
                                    letterSpacing = 0.6.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Large Glowing Number
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (lastTwoConsumption.hasEnoughData && lastTwoConsumption.avgConsumption != null) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", lastTwoConsumption.avgConsumption),
                                        fontSize = 54.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "кВт·ч / 100 км",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )
                                } else {
                                    Text(
                                        text = if (monthConsumption != null && monthConsumption > 0.0) {
                                            String.format(Locale.US, "%.1f", monthConsumption)
                                        } else "—",
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "кВт·ч / 100 км",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftBlue,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Subtitle with distance and energy info
                            if (lastTwoConsumption.hasEnoughData) {
                                Text(
                                    text = "Дистанция между зарядками: ${lastTwoConsumption.distanceKm.toInt()} км (+${String.format(Locale.US, "%.1f", lastTwoConsumption.totalKwh)} кВт·ч)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "Добавьте минимум 2 завершённые зарядки с пробегом",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Lower Block: (Last Charge | Month Consumption | Price per 100 km)
            // Perfectly aligned on fixed horizontal levels
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    VoltCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("summary_metrics_widget"),
                        borderColor = SoftBlue.copy(alpha = 0.28f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: ПОСЛЕДНЯЯ ЗАРЯДКА
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Fixed Header Slot
                                Box(
                                    modifier = Modifier.height(22.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "ПОСЛ. ЗАРЯДКА",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ElectricCyan,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Fixed Value Slot
                                Box(
                                    modifier = Modifier.height(26.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (lastSession != null && lastSession.kwhDeliveredByStation > 0) {
                                            "+${String.format(Locale.US, "%.1f", lastSession.kwhDeliveredByStation)}"
                                        } else "—",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Fixed Unit / Subtitle Slot
                                Box(
                                    modifier = Modifier.height(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (lastSession != null && lastSession.totalCost > 0) {
                                            "${String.format(Locale.US, "%.2f", lastSession.totalCost)} $currency"
                                        } else "кВт·ч",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Divider 1
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(Color.White.copy(alpha = 0.12f))
                            )

                            // Column 2: РАСХОД ЗА МЕСЯЦ (Aligned with Month up, kWh/100km down)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Fixed Header Slot (Month consumption title up)
                                Box(
                                    modifier = Modifier.height(22.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "РАСХОД / МЕСЯЦ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SoftBlue,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Fixed Value Slot
                                Box(
                                    modifier = Modifier.height(26.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (monthConsumption != null && monthConsumption > 0.0) {
                                            String.format(Locale.US, "%.1f", monthConsumption)
                                        } else "—",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Fixed Unit Slot (kWh/100km down)
                                Box(
                                    modifier = Modifier.height(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.kwhPer100Km,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Divider 2
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(Color.White.copy(alpha = 0.12f))
                            )

                            // Column 3: ЦЕНА ЗА 100 КМ
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Fixed Header Slot
                                Box(
                                    modifier = Modifier.height(22.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "ЦЕНА / 100 КМ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BatteryGreen,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Fixed Value Slot
                                Box(
                                    modifier = Modifier.height(26.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (costPer100Km != null && costPer100Km > 0) {
                                            String.format(Locale.US, "%.2f", costPer100Km)
                                        } else "—",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BatteryGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Fixed Unit Slot
                                Box(
                                    modifier = Modifier.height(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$currency / 100 км",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Floating Active Charging Card (shown when session is in progress)
            item {
                AnimatedVisibility(
                    visible = activeSession != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (activeSession != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            ActiveChargingCard(
                                session = activeSession,
                                onCompleteClick = { onCompleteChargeClick(activeSession) },
                                onCancelClick = { onCancelActiveCharge(activeSession) }
                            )
                        }
                    }
                }
            }

            // 5. Action Buttons (Full-Width Sleek Glass "Add Charge" & "Home Charge")
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp)
                ) {
                    // Button 1: "Добавить зарядку" (Full width, sleek dark glass matching Home Charge with Electric Cyan accent)
                    OutlinedButton(
                        onClick = onAddChargeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        ElectricCyan.copy(alpha = 0.8f),
                                        Color.White.copy(alpha = 0.35f),
                                        SoftBlue.copy(alpha = 0.6f)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .testTag("add_charge_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = ElectricCyan
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.addCharge,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button 2: "Домашняя зарядка (1 тап)" (Full width, sleek dark glass with mint border)
                    OutlinedButton(
                        onClick = {
                            if (onQuickHomeCharge != null) {
                                showHomeChargeDialog = true
                            } else {
                                onAddChargeClick()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        BatteryGreen.copy(alpha = 0.7f),
                                        Color.White.copy(alpha = 0.3f),
                                        BatteryGreen.copy(alpha = 0.4f)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .testTag("quick_home_charge_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = BatteryGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = BatteryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Домашняя зарядка (1 тап)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BatteryGreen
                        )
                    }
                }
            }

            // 6. Recent Sessions Header (Only 3-5 sessions, with "Вся история →" button)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentCharges,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (onNavigateToHistory != null) {
                        TextButton(
                            onClick = onNavigateToHistory,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Вся история",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SoftBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = SoftBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            if (recentSessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noRecentCharges,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(recentSessions.take(4)) { session ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                        RecentSessionCard(
                            session = session,
                            currency = currency,
                            onClick = { editingSession = session }
                        )
                    }
                }
            }
        }
    }

    // Quick Home Charge Dialog (Upgraded: Battery % remaining + current meter reading + 1-Tap start)
    if (showHomeChargeDialog) {
        val parsedSoc = homeChargeSocText.toDoubleOrNull() ?: carSoc
        val neededKwh = (usableCapacity * ((100.0 - parsedSoc).coerceAtLeast(0.0) / 100.0))
        val estPrice = 0.25 // Standard home rate
        val estCost = neededKwh * estPrice
        val parsedMeter = homeChargeMeterText.toDoubleOrNull()

        AlertDialog(
            onDismissRequest = { showHomeChargeDialog = false },
            icon = { Icon(Icons.Default.Home, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(28.dp)) },
            title = {
                Text(
                    text = "Домашняя зарядка",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Введите текущий остаток батареи и показания счётчика электроэнергии:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Input 1: Remaining battery %
                    OutlinedTextField(
                        value = homeChargeSocText,
                        onValueChange = { homeChargeSocText = it },
                        label = { Text("Остаток заряда (%)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Input 2: Current meter reading in kWh
                    OutlinedTextField(
                        value = homeChargeMeterText,
                        onValueChange = { homeChargeMeterText = it },
                        label = { Text("Показания счётчика (кВт·ч)") },
                        placeholder = { Text("необязательно") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    VoltCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("До 100% потребуется:", fontSize = 12.sp)
                                Text(
                                    text = "+${String.format(Locale.US, "%.1f", neededKwh)} кВт·ч",
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ориентир. стоимость:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "~${String.format(Locale.US, "%.2f", estCost)} $currency",
                                    fontWeight = FontWeight.Bold,
                                    color = BatteryGreen,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onQuickHomeCharge?.invoke(parsedSoc, parsedMeter)
                        showHomeChargeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BatteryGreen),
                    modifier = Modifier.testTag("confirm_quick_home_charge_button")
                ) {
                    Text("Запустить (1 тап)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHomeChargeDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Edit Session Dialog from Home Screen
    if (editingSession != null) {
        val s = editingSession!!
        var editOperatorName by remember(s.id) { mutableStateOf(s.operatorName) }
        var editStartSoc by remember(s.id) { mutableStateOf(s.startSoc.toInt().toString()) }
        var editEndSoc by remember(s.id) { mutableStateOf(s.endSoc.toInt().toString()) }
        var editKwh by remember(s.id) { mutableStateOf(String.format(Locale.US, "%.1f", s.kwhDeliveredByStation)) }
        var editCost by remember(s.id) { mutableStateOf(String.format(Locale.US, "%.2f", s.totalCost)) }
        var editOdometer by remember(s.id) { mutableStateOf(s.startOdometer.toInt().toString()) }
        var editStationType by remember(s.id) { mutableStateOf(s.stationType) }
        var editComment by remember(s.id) { mutableStateOf(s.operatorComment ?: "") }

        AlertDialog(
            onDismissRequest = { editingSession = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = ElectricCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Редактирование зарядки", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editOperatorName,
                        onValueChange = { editOperatorName = it },
                        label = { Text("Оператор / Станция") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editStartSoc,
                            onValueChange = { editStartSoc = it },
                            label = { Text("Начальный %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editEndSoc,
                            onValueChange = { editEndSoc = it },
                            label = { Text("Конечный %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editKwh,
                            onValueChange = { editKwh = it },
                            label = { Text("Заряжено кВт·ч") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editCost,
                            onValueChange = { editCost = it },
                            label = { Text("Сумма ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editOdometer,
                        onValueChange = { editOdometer = it },
                        label = { Text("Пробег (км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editComment,
                        onValueChange = { editComment = it },
                        label = { Text("Заметка / Тег") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = s.copy(
                            operatorName = editOperatorName.trim(),
                            startSoc = editStartSoc.toDoubleOrNull() ?: s.startSoc,
                            endSoc = editEndSoc.toDoubleOrNull() ?: s.endSoc,
                            kwhDeliveredByStation = editKwh.toDoubleOrNull() ?: s.kwhDeliveredByStation,
                            totalCost = editCost.toDoubleOrNull() ?: s.totalCost,
                            startOdometer = editOdometer.toDoubleOrNull() ?: s.startOdometer,
                            stationType = editStationType,
                            operatorComment = editComment.trim().ifEmpty { null }
                        )
                        onUpdateSession?.invoke(updated)
                        editingSession = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val toDelete = s
                            editingSession = null
                            sessionToDelete = toDelete
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить")
                    }
                    TextButton(onClick = { editingSession = null }) {
                        Text(strings.cancel)
                    }
                }
            }
        )
    }

    // Confirm Delete Dialog from Home Screen
    if (sessionToDelete != null) {
        val s = sessionToDelete!!
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить эту зарядку?") },
            text = {
                Text("Зарядка на ${s.operatorName.ifEmpty { "EV Station" }} (+${String.format(Locale.US, "%.1f", s.kwhDeliveredByStation)} кВт·ч, ${formatCurrency(s.totalCost, currency)}) будет удалена навсегда.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSession?.invoke(s)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Car Passport Dialog
    if (showCarPassportDialog && activeCar != null) {
        CarPassportDialog(
            car = activeCar,
            sessions = recentSessions,
            currency = currency,
            onDismiss = { showCarPassportDialog = false },
            onSaveCar = { updatedCar ->
                onUpdateCar?.invoke(updatedCar)
                showCarPassportDialog = false
            },
            onSellCar = { carToSell ->
                onDeleteCar?.invoke(carToSell)
                showCarPassportDialog = false
            }
        )
    }
}

@Composable
fun RecentSessionCard(
    session: ChargingSession,
    currency: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDc = session.stationType.equals("DC", ignoreCase = true)
    val isHome = session.operatorName.contains("Дом", ignoreCase = true)
    val badgeColor = when {
        isHome -> BatteryGreen
        isDc -> Color(0xFFC084FC) // Purple
        else -> Color(0xFF38BDF8) // Light Blue AC
    }

    VoltCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isHome) "HOME" else session.stationType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = session.operatorName.ifEmpty { "Станция зарядки" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatCurrency(session.totalCost, currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ElectricCyan
                    )
                    if (onClick != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit session",
                            tint = SoftBlue.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${session.startSoc.toInt()}% → ${session.endSoc.toInt()}% " +
                            if (session.kwhDeliveredByStation > 0) "(+${String.format(Locale.getDefault(), "%.1f", session.kwhDeliveredByStation)} кВт·ч)" else "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                )

                Text(
                    text = formatDate(session.startTime),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }

            if (!session.operatorComment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                TagBadge(name = session.operatorComment, color = 0xFF38BDF8)
            }
        }
    }
}
