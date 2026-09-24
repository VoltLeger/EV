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
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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
import com.example.ui.components.AddEditExpenseDialog
import com.example.ui.components.CarPassportDialog
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.StationTypeBadge
import com.example.ui.components.TagBadge
import com.example.ui.components.VoltAvatar
import com.example.ui.components.VoltCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.BatteryOrange
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
    onQuickHomeCharge: ((Double, Double?, Double?, String?, Boolean?) -> Unit)? = null,
    onCompleteChargeClick: (ChargingSession) -> Unit,
    onCancelActiveCharge: (ChargingSession) -> Unit,
    onNavigateToHistory: (() -> Unit)? = null,
    onUpdateSession: ((ChargingSession) -> Unit)? = null,
    onDeleteSession: ((ChargingSession) -> Unit)? = null,
    userProfile: UserProfile? = null,
    onOpenProfile: (() -> Unit)? = null,
    onUpdateCar: ((Car) -> Unit)? = null,
    onDeleteCar: ((Car) -> Unit)? = null,
    onAddExpense: ((category: String, amount: Double, odometer: Double?, comment: String?) -> Unit)? = null,
    topExpenseCategories: List<String> = emptyList(),
    appSettings: com.example.data.model.AppSettings? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    var showCarPassportDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showHomeNotConfiguredDialog by remember { mutableStateOf(false) }

    // Dynamic Latest Odometer: calculated across car, sessions, and expenses
    val latestOdometer = remember(activeCar?.initialOdometer, recentSessions) {
        val carOdo = activeCar?.initialOdometer ?: 0.0
        val maxSessionOdo = recentSessions.maxOfOrNull { maxOf(it.startOdometer, 0.0) } ?: 0.0
        maxOf(carOdo, maxSessionOdo)
    }

    // Session Edit & Delete state
    var editingSession by remember { mutableStateOf<ChargingSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<ChargingSession?>(null) }

    // Quick Home Charge Dialog State
    var showHomeChargeDialog by remember { mutableStateOf(false) }
    var homeChargeSocText by remember(activeCar?.currentSoc) {
        mutableStateOf(activeCar?.currentSoc?.toInt()?.toString() ?: "30")
    }
    var homeChargeMeterText by remember { mutableStateOf("") }
    // Tariff mode selection in dialog: "auto" (default active), "standard", "two_tariff", "three_tariff"
    var selectedTariffMode by remember { mutableStateOf("auto") }

    val usableCapacity = activeCar?.usableCapacityKwh ?: 57.0
    val carSoc = activeCar?.currentSoc ?: 80.0

    // Compute baseline consumption
    val completedSessions = remember(recentSessions) { recentSessions.filter { it.status == "completed" } }
    val realAvg = remember(completedSessions, monthConsumption, usableCapacity, carSoc) {
        monthConsumption ?: run {
            val forecast = EVCalculator.calculateRangeForecast(completedSessions, usableCapacity, carSoc)
            forecast.realConsumptionPer100Km
        }
    }

    // Range with current car SOC for top vehicle bar pill
    val currentSocRange = remember(usableCapacity, carSoc, realAvg) {
        EVCalculator.calculateRangeForSoc(
            usableCapacityKwh = usableCapacity,
            socPercent = carSoc,
            consumptionPer100Km = realAvg
        )
    }

    // Departure forecast range if active session exists
    val departureForecastRange = remember(activeSession, carSoc, usableCapacity, realAvg) {
        if (activeSession != null) {
            val targetSoc = if (activeSession.endSoc > carSoc) activeSession.endSoc else 90.0
            EVCalculator.calculateRangeForSoc(
                usableCapacityKwh = usableCapacity,
                socPercent = targetSoc,
                consumptionPer100Km = realAvg
            )
        } else null
    }

    // Calculate Last Two Charges Consumption (Centerpiece 3D Card)
    val lastTwoConsumption = remember(completedSessions, usableCapacity) {
        EVCalculator.calculateLastTwoChargesConsumption(completedSessions, usableCapacity)
    }

    // Lower block metrics
    val lastSession = remember(completedSessions) { completedSessions.maxByOrNull { it.startTime } }
    val costPer100Km = remember(lastSession, realAvg, monthConsumption) {
        if (lastSession != null && lastSession.kwhDeliveredByStation > 0 && lastSession.totalCost > 0) {
            val pricePerKwh = lastSession.totalCost / lastSession.kwhDeliveredByStation
            pricePerKwh * realAvg
        } else if (monthConsumption != null && lastSession != null) {
            lastSession.pricePerKwh * monthConsumption
        } else null
    }

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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Top-Left: Hall of Fame / Awards Cup Button
                        if (onOpenProfile != null) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFFF59E0B).copy(alpha = 0.25f),
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFFF59E0B).copy(alpha = 0.6f),
                                                ElectricCyan.copy(alpha = 0.3f),
                                                Color.White.copy(alpha = 0.1f)
                                            )
                                        ),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onOpenProfile() }
                                    .testTag("awards_cup_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Зал славы и награды",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Center: Vehicle selector / Car Passport
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
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
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("car_passport_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
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
                                            .clip(RoundedCornerShape(9.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = "Паспорт авто",
                                        tint = SoftBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeCar?.name ?: strings.currentVehicle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "📋",
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = "${latestOdometer.toInt()} км",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Top Right: Range Forecast Pill (clickable to open forecast dialog)
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
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("estimated_range_top_pill")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (activeSession != null) Icons.Default.ElectricBolt else Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = if (activeSession != null) ElectricCyan else BatteryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (departureForecastRange != null) {
                                            "~${departureForecastRange.toInt()} км"
                                        } else {
                                            "~${currentSocRange.toInt()} км"
                                        },
                                        fontSize = 13.sp,
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
                    }
                }
            }

            // 2. Centerpiece Card: Average Consumption (Last 2 Charges)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            // Base gradient background smoothly transitioning into ambient theme
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF131D31).copy(alpha = 0.85f),
                                        Color(0xFF0F172A).copy(alpha = 0.90f),
                                        Color(0xFF0B1120).copy(alpha = 0.85f)
                                    )
                                )
                            )
                            // Soft radial glow in the center that smoothly dissolves toward borders
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        ElectricCyan.copy(alpha = 0.20f),
                                        SoftBlue.copy(alpha = 0.07f),
                                        Color.Transparent
                                    ),
                                    radius = 420f
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        ElectricCyan.copy(alpha = 0.45f),
                                        SoftBlue.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .padding(vertical = 18.dp, horizontal = 20.dp)
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
                                    .background(ElectricCyan.copy(alpha = 0.12f))
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
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

                            Spacer(modifier = Modifier.height(12.dp))

                            // Large Glowing Number with luminous shadow aura
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val numberGlow = Shadow(
                                    color = ElectricCyan.copy(alpha = 0.75f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 26f
                                )
                                val unitGlow = Shadow(
                                    color = ElectricCyan.copy(alpha = 0.45f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 14f
                                )

                                if (lastTwoConsumption.hasEnoughData && lastTwoConsumption.avgConsumption != null) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", lastTwoConsumption.avgConsumption),
                                        fontSize = 54.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        style = TextStyle(shadow = numberGlow)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "кВт·ч / 100 км",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyan,
                                        style = TextStyle(shadow = unitGlow),
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )
                                } else {
                                    val isVal = monthConsumption != null && monthConsumption > 0.0
                                    Text(
                                        text = if (isVal) String.format(Locale.US, "%.1f", monthConsumption) else "—",
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        style = TextStyle(shadow = numberGlow)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "кВт·ч / 100 км",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftBlue,
                                        style = TextStyle(shadow = unitGlow),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Subtitle with distance and energy info
                            if (lastTwoConsumption.hasEnoughData) {
                                Text(
                                    text = "Поездка: ${lastTwoConsumption.distanceKm.toInt()} км • Расход: ${String.format(Locale.US, "%.1f", lastTwoConsumption.totalKwh)} кВт·ч",
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

                    // Button 2: "Домашняя зарядка" (Full width, sleek dark glass with mint border)
                    OutlinedButton(
                        onClick = {
                            val isConfigured = appSettings?.homeChargeConfigured == true
                            if (appSettings?.homeLastMeterKwh != null && appSettings.homeLastMeterKwh > 0) {
                                homeChargeMeterText = String.format(Locale.US, "%.1f", appSettings.homeLastMeterKwh)
                            }
                            if (!isConfigured) {
                                showHomeNotConfiguredDialog = true
                            } else if (onQuickHomeCharge != null) {
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
                            text = strings.homeChargeQuick,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BatteryGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Button 3: "Бортовой журнал • Добавить расход" (Sleek dark glass with soft blue border)
                    OutlinedButton(
                        onClick = { showAddExpenseDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        SoftBlue.copy(alpha = 0.7f),
                                        Color.White.copy(alpha = 0.3f),
                                        ElectricCyan.copy(alpha = 0.4f)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .testTag("add_expense_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = SoftBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = SoftBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Бортовой журнал • Добавить расход",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftBlue
                        )
                    }
                }
            }

            // 6. Recent Session Header (Compact: strictly only 1 latest charge)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Последняя зарядка",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (onNavigateToHistory != null) {
                        TextButton(
                            onClick = onNavigateToHistory,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Вся история",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SoftBlue
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = SoftBlue,
                                modifier = Modifier.size(13.dp)
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
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noRecentCharges,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                item {
                    val latest = recentSessions.first()
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        RecentSessionCard(
                            session = latest,
                            currency = currency,
                            onClick = { editingSession = latest }
                        )
                    }
                }
            }
        }
    }

    // Quick Home Charge Dialog (Upgraded: 3 Quick Tariff Switch Buttons, Battery % remaining, Last Meter Reading + 1-Tap start)
    if (showHomeChargeDialog) {
        val parsedSoc = homeChargeSocText.toDoubleOrNull() ?: carSoc
        val neededKwh = (usableCapacity * ((100.0 - parsedSoc).coerceAtLeast(0.0) / 100.0))
        val parsedMeter = homeChargeMeterText.toDoubleOrNull()

        // Calculate active tariff based on user's quick switch selection ("standard", "two_tariff", "three_tariff", or "auto")
        val effectiveTariff = remember(selectedTariffMode, appSettings) {
            val settings = appSettings ?: com.example.data.model.AppSettings()
            val nowHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when (selectedTariffMode) {
                "standard" -> {
                    EVCalculator.HomeTariffEstimate(
                        pricePerKwh = settings.homeStandardPrice,
                        tariffName = "Стандартный",
                        isNight = false
                    )
                }
                "two_tariff" -> {
                    val isNight = EVCalculator.isHourInRange(nowHour, settings.homeNightStartHour, settings.homeNightEndHour)
                    val price = if (isNight) settings.homeNightPrice else settings.homeStandardPrice
                    val name = if (isNight) "Ночной (23:00-06:00)" else "Дневной (стандартный)"
                    EVCalculator.HomeTariffEstimate(
                        pricePerKwh = price,
                        tariffName = name,
                        isNight = isNight
                    )
                }
                "three_tariff" -> {
                    val isNight = EVCalculator.isHourInRange(nowHour, settings.homeNightStartHour, settings.homeNightEndHour)
                    val isPeak = EVCalculator.isHourInRange(nowHour, settings.homePeakStartHour, settings.homePeakEndHour)
                    when {
                        isNight -> EVCalculator.HomeTariffEstimate(settings.homeNightPrice, "Ночной (23:00-06:00)", true)
                        isPeak -> EVCalculator.HomeTariffEstimate(settings.homePeakPrice, "Пиковый (17:00-23:00)", false)
                        else -> EVCalculator.HomeTariffEstimate(settings.homeSemiPeakPrice, "Полупиковый (06:00-17:00)", false)
                    }
                }
                else -> {
                    // Default behavior (configured preference or standard)
                    EVCalculator.determineHomeTariff(settings)
                }
            }
        }

        val estPrice = effectiveTariff.pricePerKwh
        val estCost = neededKwh * estPrice

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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Tariff Switcher: 3 buttons at top
                    Text(
                        text = "Выбор тарифа:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isStd = selectedTariffMode == "standard" || (selectedTariffMode == "auto" && appSettings?.homeThreeTariffEnabled != true && appSettings?.homeNightTariffEnabled != true)
                        val isTwo = selectedTariffMode == "two_tariff" || (selectedTariffMode == "auto" && appSettings?.homeNightTariffEnabled == true && appSettings.homeThreeTariffEnabled != true)
                        val isThree = selectedTariffMode == "three_tariff" || (selectedTariffMode == "auto" && appSettings?.homeThreeTariffEnabled == true)

                        // Button 1: Стандарт
                        FilterChip(
                            selected = isStd,
                            onClick = { selectedTariffMode = "standard" },
                            label = { Text("Стандарт", fontSize = 11.sp, fontWeight = if (isStd) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BatteryGreen,
                                selectedLabelColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("quick_tariff_standard")
                        )

                        // Button 2: 2-зонный
                        FilterChip(
                            selected = isTwo,
                            onClick = { selectedTariffMode = "two_tariff" },
                            label = { Text("2-зонный", fontSize = 11.sp, fontWeight = if (isTwo) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("quick_tariff_two_zones")
                        )

                        // Button 3: 3-зонный
                        FilterChip(
                            selected = isThree,
                            onClick = { selectedTariffMode = "three_tariff" },
                            label = { Text("3-зонный", fontSize = 11.sp, fontWeight = if (isThree) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BatteryOrange,
                                selectedLabelColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("quick_tariff_three_zones")
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Введите текущий остаток батареи и показания счётчика электроэнергии:",
                        fontSize = 12.sp,
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

                    // Input 2: Current meter reading in kWh (with prompt button for last reading if available)
                    OutlinedTextField(
                        value = homeChargeMeterText,
                        onValueChange = { homeChargeMeterText = it },
                        label = { Text("Показания счётчика (кВт·ч)") },
                        placeholder = { Text("необязательно") },
                        supportingText = {
                            if (appSettings?.homeLastMeterKwh != null && appSettings.homeLastMeterKwh > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        homeChargeMeterText = String.format(Locale.US, "%.1f", appSettings.homeLastMeterKwh)
                                    },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Предыдущее: ${String.format(Locale.US, "%.1f", appSettings.homeLastMeterKwh)} кВт·ч (нажмите, чтобы подставить)",
                                        fontSize = 10.sp,
                                        color = ElectricCyan
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("home_charge_meter_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    VoltCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Выбранный тариф:", fontSize = 12.sp)
                                Text(
                                    text = "${effectiveTariff.tariffName} (${String.format(Locale.US, "%.4f", effectiveTariff.pricePerKwh)} $currency)",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (effectiveTariff.isNight) BatteryGreen else ElectricCyan,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
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
                        onQuickHomeCharge?.invoke(
                            parsedSoc,
                            parsedMeter,
                            effectiveTariff.pricePerKwh,
                            effectiveTariff.tariffName,
                            effectiveTariff.isNight
                        )
                        showHomeChargeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BatteryGreen),
                    modifier = Modifier.testTag("confirm_quick_home_charge_button")
                ) {
                    Text(if (strings.isEn) "Start" else "Запустить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHomeChargeDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Dialog when Home Charging is not yet configured by the user
    if (showHomeNotConfiguredDialog) {
        AlertDialog(
            onDismissRequest = { showHomeNotConfiguredDialog = false },
            icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = SoftBlue, modifier = Modifier.size(28.dp)) },
            title = {
                Text(
                    text = "Настройка домашней зарядки",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Параметры домашней зарядки ещё не настроены. У каждого пользователя свои тарифы электроэнергии (одноставочный, двухзонный или трёхзонный). Пожалуйста, настройте тарифы в Настройках, чтобы расчёт стоимости был точным.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHomeNotConfiguredDialog = false
                        onNavigateToSettings?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftBlue),
                    modifier = Modifier.testTag("go_to_settings_button")
                ) {
                    Text("Настроить сейчас", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showHomeNotConfiguredDialog = false
                    // Allow quick start with default standard tariff even if not yet customized
                    showHomeChargeDialog = true
                }) {
                    Text("Продолжить со станд.")
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

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        AddEditExpenseDialog(
            defaultOdometer = activeCar?.initialOdometer,
            topCategories = topExpenseCategories,
            currency = currency,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { cat, amt, odo, comm ->
                onAddExpense?.invoke(cat, amt, odo, comm)
                showAddExpenseDialog = false
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
