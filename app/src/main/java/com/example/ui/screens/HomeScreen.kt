package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
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
import com.example.ui.components.EditChargingSessionDialog
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.ShareSessionCardDialog
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
import com.example.ui.theme.LocalCurrentTheme
import com.example.ui.theme.SoftBlue
import com.example.util.EVCalculator
import java.util.Locale

@Composable
fun HomeScreen(
    activeCar: Car?,
    allCars: List<Car>,
    activeSession: ChargingSession?,
    monthConsumption: Double?,
    effectiveMonthConsumption: com.example.util.EVCalculator.EffectiveMonthConsumption? = null,
    recentSessions: List<ChargingSession>,
    onSelectCar: (Long) -> Unit,
    onAddChargeClick: () -> Unit,
    onCalculateRangeClick: () -> Unit,
    onQuickHomeCharge: ((Double, Double?, Double?, String?, Boolean?, Double?) -> Unit)? = null,
    onCompleteChargeClick: (ChargingSession) -> Unit,
    onCancelActiveCharge: (ChargingSession) -> Unit,
    onNavigateToHistory: (() -> Unit)? = null,
    onUpdateSession: ((ChargingSession) -> Unit)? = null,
    onDeleteSession: ((ChargingSession) -> Unit)? = null,
    userProfile: UserProfile? = null,
    onOpenProfile: (() -> Unit)? = null,
    onUpdateCar: ((Car) -> Unit)? = null,
    onDeleteCar: ((Car) -> Unit)? = null,
    onAddExpense: ((category: String, amount: Double, odometer: Double?, comment: String?, currency: String) -> Unit)? = null,
    topExpenseCategories: List<String> = emptyList(),
    appSettings: com.example.data.model.AppSettings? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    latestOdometer: Double? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    val isLightHomeCharge = LocalCurrentTheme.current == "light"
    var showCarPassportDialog by rememberSaveable { mutableStateOf(false) }
    var showAddExpenseDialog by rememberSaveable { mutableStateOf(false) }
    var showHomeNotConfiguredDialog by rememberSaveable { mutableStateOf(false) }

    // Dynamic Latest Odometer: calculated across car, sessions, and expenses
    val latestEffectiveOdometer = remember(latestOdometer, activeCar?.initialOdometer, recentSessions) {
        if (latestOdometer != null && latestOdometer > 0) {
            latestOdometer
        } else {
            val carOdo = activeCar?.initialOdometer ?: 0.0
            val maxSessionOdo = recentSessions.maxOfOrNull { maxOf(it.startOdometer, 0.0) } ?: 0.0
            maxOf(carOdo, maxSessionOdo)
        }
    }

    // Session Edit & Delete state persisted by ID across orientation changes
    var editingSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sessionToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sessionToShareId by rememberSaveable { mutableStateOf<Long?>(null) }

    val editingSession = remember(recentSessions, editingSessionId) {
        recentSessions.find { it.id == editingSessionId }
    }
    val sessionToDelete = remember(recentSessions, sessionToDeleteId) {
        recentSessions.find { it.id == sessionToDeleteId }
    }
    val sessionToShare = remember(recentSessions, sessionToShareId) {
        recentSessions.find { it.id == sessionToShareId }
    }

    // Quick Home Charge Dialog State (Persisted with rememberSaveable across orientation and lifecycle changes)
    var showHomeChargeDialog by rememberSaveable { mutableStateOf(false) }
    var homeChargeSocText by rememberSaveable { mutableStateOf("") }
    var homeChargeOdoText by rememberSaveable { mutableStateOf("") }
    var homeChargeMeterText by rememberSaveable { mutableStateOf("") }
    // Tariff mode selection in dialog: "auto" (default active), "standard", "two_tariff", "three_tariff"
    var selectedTariffMode by rememberSaveable { mutableStateOf("auto") }
    var homeChargeInitialized by rememberSaveable { mutableStateOf(false) }

    // When the dialog opens for the first time, populate initial values once. NEVER overwrite user-edited data on recompositions or rotation.
    LaunchedEffect(showHomeChargeDialog) {
        if (showHomeChargeDialog && !homeChargeInitialized) {
            if (homeChargeSocText.isBlank()) {
                homeChargeSocText = activeCar?.currentSoc?.toInt()?.toString() ?: "30"
            }
            if (homeChargeOdoText.isBlank() && latestEffectiveOdometer > 0) {
                homeChargeOdoText = latestEffectiveOdometer.toInt().toString()
            }
            if (homeChargeMeterText.isBlank() && appSettings?.homeLastMeterKwh != null && appSettings.homeLastMeterKwh > 0) {
                homeChargeMeterText = String.format(Locale.US, "%.1f", appSettings.homeLastMeterKwh)
            }
            homeChargeInitialized = true
        }
    }

    val usableCapacity = activeCar?.usableCapacityKwh ?: 57.0
    val carSoc = activeCar?.currentSoc ?: 80.0

    // Compute baseline consumption
    val completedSessions = remember(recentSessions) { recentSessions.filter { it.status == "completed" } }
    val effectiveCons = effectiveMonthConsumption?.consumption ?: monthConsumption
    val realAvg = remember(completedSessions, effectiveCons, usableCapacity, carSoc) {
        effectiveCons ?: run {
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

    // Calculate Recent Charges Consumption (Last 3 charges, or 2 if distance >= 200 km)
    val lastTwoConsumption = remember(completedSessions, usableCapacity) {
        EVCalculator.calculateLastTwoChargesConsumption(completedSessions, usableCapacity)
    }

    // Lower block metrics
    val lastSession = remember(completedSessions) { completedSessions.maxByOrNull { it.startTime } }
    val costPer100Km = remember(lastSession, realAvg, effectiveCons, currency) {
        if (lastSession != null && lastSession.kwhDeliveredByStation > 0 && lastSession.totalCost > 0) {
            val totalCostInMain = com.example.util.CurrencyConverter.convert(lastSession.totalCost, lastSession.currency, currency)
            val pricePerKwh = totalCostInMain / lastSession.kwhDeliveredByStation
            pricePerKwh * realAvg
        } else if (effectiveCons != null && lastSession != null) {
            val priceInMain = com.example.util.CurrencyConverter.convert(lastSession.pricePerKwh, lastSession.currency, currency)
            priceInMain * effectiveCons
        } else null
    }

    // Current month charges and metrics: distance for all charges, count, and cost per 100 km in current month
    val currentMonthData = remember(completedSessions, currency, realAvg, effectiveCons) {
        val now = java.util.Calendar.getInstance()
        val curYear = now.get(java.util.Calendar.YEAR)
        val curMonth = now.get(java.util.Calendar.MONTH)
        val cal = java.util.Calendar.getInstance()

        val curMonthSessions = completedSessions.filter { session ->
            cal.timeInMillis = session.startTime
            cal.get(java.util.Calendar.YEAR) == curYear && cal.get(java.util.Calendar.MONTH) == curMonth
        }.sortedBy { it.startTime }

        // Distance for all charges in current month
        val distanceKm: Double = if (curMonthSessions.size >= 2) {
            val minOdo = curMonthSessions.minOf { it.startOdometer }
            val maxOdo = curMonthSessions.maxOf { it.startOdometer }
            (maxOdo - minOdo).coerceAtLeast(0.0)
        } else if (curMonthSessions.size == 1) {
            val first = curMonthSessions.first()
            val prevSession = completedSessions
                .filter { it.startTime < first.startTime }
                .maxByOrNull { it.startTime }
            if (prevSession != null && first.startOdometer > prevSession.startOdometer) {
                first.startOdometer - prevSession.startOdometer
            } else 0.0
        } else 0.0

        val totalCostMonth = curMonthSessions.sumOf {
            com.example.util.CurrencyConverter.convert(it.totalCost, it.currency, currency)
        }
        val totalKwhMonth = curMonthSessions.sumOf { it.kwhDeliveredByStation }

        // Cost per 100 km this month
        val cost100Km: Double? = if (distanceKm >= 5.0 && totalCostMonth > 0.0) {
            (totalCostMonth / distanceKm) * 100.0
        } else if (totalCostMonth > 0.0 && totalKwhMonth > 0.0) {
            val pricePerKwh = totalCostMonth / totalKwhMonth
            val cons = effectiveCons ?: realAvg
            pricePerKwh * cons
        } else null

        Triple(distanceKm, curMonthSessions.size, cost100Km)
    }

    LiquidGlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. Top Vehicle Header Row
            item {
                val currentThemeKey = LocalCurrentTheme.current
                val isLight = currentThemeKey == "light"
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary

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
                                        if (isLight) SolidColor(Color.Black) else Brush.linearGradient(
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
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isLight) 0.95f else 0.85f))
                                .border(
                                    1.dp,
                                    if (isLight) SolidColor(Color.Black) else Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            primaryColor.copy(alpha = 0.35f),
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
                                    text = "${latestEffectiveOdometer.toInt()} км",
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
                                    width = 1.dp,
                                    brush = if (isLight) androidx.compose.ui.graphics.SolidColor(Color.Black) else Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.45f),
                                            if (activeSession != null) ElectricCyan.copy(alpha = 0.6f) else BatteryGreen.copy(alpha = 0.5f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
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
                    val currentThemeKey = LocalCurrentTheme.current
                    val isLight = currentThemeKey == "light"
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary

                    val centerpieceShape = RoundedCornerShape(26.dp)
                    val centerpieceBorder = if (isLight) {
                        BorderStroke(1.dp, Color.Black)
                    } else {
                        BorderStroke(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.45f),
                                    secondaryColor.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(centerpieceShape)
                            .background(
                                color = if (isLight) Color.White else MaterialTheme.colorScheme.surface,
                                shape = centerpieceShape
                            )
                            .then(
                                if (!isLight) {
                                    Modifier.background(
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        ),
                                        shape = centerpieceShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .then(
                                if (!isLight) {
                                    Modifier.background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = 0.20f),
                                                secondaryColor.copy(alpha = 0.08f),
                                                Color.Transparent
                                            ),
                                            radius = 420f
                                        ),
                                        shape = centerpieceShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .border(centerpieceBorder, centerpieceShape)
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
                                    .background(primaryColor.copy(alpha = if (isLight) 0.15f else 0.12f))
                                    .then(
                                        if (isLight) Modifier else Modifier.border(1.dp, primaryColor.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val cardHeader = if (lastTwoConsumption.hasEnoughData && lastTwoConsumption.avgConsumption != null) {
                                    if (lastTwoConsumption.chargesCount == 2 && lastTwoConsumption.distanceKm >= 200.0) {
                                        "СРЕДНИЙ РАСХОД (ЗА 2 ЗАРЯДКИ > 200 КМ)"
                                    } else {
                                        "СРЕДНИЙ РАСХОД (ЗА ${lastTwoConsumption.chargesCount} ЗАРЯДКИ)"
                                    }
                                } else {
                                    "СРЕДНИЙ РАСХОД"
                                }
                                Text(
                                    text = cardHeader,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryColor,
                                    letterSpacing = 0.6.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val displayConsumption = if (lastTwoConsumption.hasEnoughData && lastTwoConsumption.avgConsumption != null) {
                                lastTwoConsumption.avgConsumption
                            } else {
                                effectiveCons
                            }

                            // Large Glowing Number with luminous shadow aura (disabled in light theme for crispness)
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val numberGlow = if (isLight) null else Shadow(
                                    color = primaryColor.copy(alpha = 0.75f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 26f
                                )
                                val unitGlow = if (isLight) null else Shadow(
                                    color = primaryColor.copy(alpha = 0.45f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 14f
                                )

                                if (displayConsumption != null && displayConsumption > 0.0) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", displayConsumption),
                                        fontSize = 54.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isLight) MaterialTheme.colorScheme.onSurface else Color.White,
                                        style = TextStyle(shadow = numberGlow)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "кВт·ч / 100 км",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor,
                                        style = TextStyle(shadow = unitGlow),
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )
                                } else {
                                    Text(
                                        text = "—",
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isLight) MaterialTheme.colorScheme.onSurface else Color.White,
                                        style = TextStyle(shadow = numberGlow)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "кВт·ч / 100 км",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = secondaryColor,
                                        style = TextStyle(shadow = unitGlow),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Subtitle: clean and clear indication of calculation basis without confusing clutter
                            if (lastTwoConsumption.hasEnoughData && lastTwoConsumption.avgConsumption != null) {
                                val subtitleText = if (lastTwoConsumption.chargesCount == 2 && lastTwoConsumption.distanceKm >= 200.0) {
                                    "За последние 2 зарядки (пробег ${lastTwoConsumption.distanceKm.toInt()} км)"
                                } else if (lastTwoConsumption.chargesCount >= 3) {
                                    "За последние 3 зарядки"
                                } else {
                                    "За последние 2 зарядки"
                                }
                                Text(
                                    text = subtitleText,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (displayConsumption != null && displayConsumption > 0.0) {
                                Text(
                                    text = "Средний расход электромобиля",
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
                            // Column 1: ПРОБЕГ ЗА ВСЕ ЗАРЯДКИ В ТЕКУЩЕМ МЕСЯЦЕ
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
                                        text = "ПРОБЕГ / МЕСЯЦ",
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
                                    val (monthDist, monthCount, _) = currentMonthData
                                    Text(
                                        text = if (monthDist > 0.0) {
                                            "${monthDist.toInt()} км"
                                        } else if (monthCount > 0) {
                                            "0 км"
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
                                        text = "в этом месяце",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Divider 1
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            )

                            // Column 2: ПОСЛЕДНЯЯ ЗАРЯДКА
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
                                            val sessionCurr = lastSession.currency.trim().uppercase().ifBlank { currency }
                                            if (sessionCurr.equals(currency, ignoreCase = true)) {
                                                "${String.format(Locale.US, "%.2f", lastSession.totalCost)} $currency"
                                            } else {
                                                val converted = com.example.util.CurrencyConverter.convert(lastSession.totalCost, sessionCurr, currency)
                                                "${String.format(Locale.US, "%.1f", lastSession.totalCost)} $sessionCurr (~${String.format(Locale.US, "%.1f", converted)} $currency)"
                                            }
                                        } else "кВт·ч",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Divider 2
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            )

                            // Column 3: ЦЕНА 100 КМ В ЭТОМ МЕСЯЦЕ
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
                                    val monthCost100 = currentMonthData.third ?: costPer100Km
                                    Text(
                                        text = if (monthCost100 != null && monthCost100 > 0) {
                                            String.format(Locale.US, "%.2f", monthCost100)
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
                                        text = "$currency (в этом мес.)",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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
                val currentThemeKey = LocalCurrentTheme.current
                val isLight = currentThemeKey == "light"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp)
                ) {
                    // Button 1: "Добавить зарядку" (Full width, sleek glass matching theme with primary accent)
                    val primaryBtnColor = if (isLight) ElectricCyan else MaterialTheme.colorScheme.primary
                    val secondaryBtnColor = if (isLight) Color(0xFFA855F7) else MaterialTheme.colorScheme.secondary

                    OutlinedButton(
                        onClick = onAddChargeClick,
                        border = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        primaryBtnColor.copy(alpha = 0.25f),
                                        secondaryBtnColor.copy(alpha = 0.20f)
                                    )
                                )
                            )
                            .border(
                                width = 1.4.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        primaryBtnColor,
                                        secondaryBtnColor
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .testTag("add_charge_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = primaryBtnColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = primaryBtnColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.addCharge,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryBtnColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button 2: "Домашняя зарядка" (Full width, sleek dark glass with mint border)
                    OutlinedButton(
                        onClick = {
                            val isConfigured = appSettings?.homeChargeConfigured == true
                            if (homeChargeSocText.isBlank()) {
                                homeChargeSocText = activeCar?.currentSoc?.toInt()?.toString() ?: "30"
                            }
                            if (homeChargeOdoText.isBlank() && latestEffectiveOdometer > 0) {
                                homeChargeOdoText = latestEffectiveOdometer.toInt().toString()
                            }
                            if (homeChargeMeterText.isBlank() && appSettings?.homeLastMeterKwh != null && appSettings.homeLastMeterKwh > 0) {
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
                        border = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(BatteryGreen.copy(alpha = 0.25f))
                            .border(
                                width = 1.4.dp,
                                color = BatteryGreen,
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
                        border = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SoftBlue.copy(alpha = 0.22f))
                            .border(
                                width = 1.4.dp,
                                color = SoftBlue,
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
                            onClick = { editingSessionId = latest.id },
                            onEditClick = { editingSessionId = latest.id },
                            onShareClick = { sessionToShareId = latest.id }
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

        val isLightHomeCharge = LocalCurrentTheme.current == "light"
        AlertDialog(
            onDismissRequest = { /* Do not auto-dismiss on outside tap so user's data is never lost */ },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            ),
            modifier = if (isLightHomeCharge) Modifier.border(1.dp, Color.Black, RoundedCornerShape(26.dp)) else Modifier,
            shape = RoundedCornerShape(26.dp),
            tonalElevation = 0.dp,
            containerColor = if (isLightHomeCharge) Color.White else MaterialTheme.colorScheme.surface,
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
                        text = "Введите остаток батареи, текущий пробег и показания счётчика:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Input 1 & 2: SoC % and Odometer (km) pre-filled with last entered mileage
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = homeChargeSocText,
                            onValueChange = { homeChargeSocText = it },
                            label = { Text("Остаток (%)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.9f).testTag("home_charge_soc_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = homeChargeOdoText,
                            onValueChange = { homeChargeOdoText = it },
                            label = { Text("Пробег (км)") },
                            placeholder = { Text(if (latestEffectiveOdometer > 0) latestEffectiveOdometer.toInt().toString() else "0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.1f).testTag("home_charge_odometer_input"),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    if (latestEffectiveOdometer > 0) {
                        Text(
                            text = "Предыдущий пробег: ${latestEffectiveOdometer.toInt()} км",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Input 3: Current meter reading in kWh (pre-filled with the last entered reading)
                    OutlinedTextField(
                        value = homeChargeMeterText,
                        onValueChange = { homeChargeMeterText = it },
                        label = { Text("Показания счётчика (кВт·ч)") },
                        placeholder = { Text("необязательно") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("home_charge_meter_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (appSettings?.homeLastMeterKwh != null && appSettings.homeLastMeterKwh > 0) {
                        Text(
                            text = "Предыдущее: ${String.format(Locale.US, "%.1f", appSettings.homeLastMeterKwh)} кВт·ч (нажмите для ввода)",
                            fontSize = 11.sp,
                            color = ElectricCyan,
                            modifier = Modifier.clickable {
                                homeChargeMeterText = String.format(Locale.US, "%.1f", appSettings.homeLastMeterKwh)
                            }
                        )
                    }

                    val isLight = LocalCurrentTheme.current == "light"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isLight) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .then(
                                if (isLight) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            )
                            .padding(12.dp)
                    ) {
                        Column {
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
                        val parsedOdo = homeChargeOdoText.toDoubleOrNull() ?: latestEffectiveOdometer.takeIf { it > 0 }
                        onQuickHomeCharge?.invoke(
                            parsedSoc,
                            parsedMeter,
                            effectiveTariff.pricePerKwh,
                            effectiveTariff.tariffName,
                            effectiveTariff.isNight,
                            parsedOdo
                        )
                        showHomeChargeDialog = false
                        homeChargeInitialized = false
                        homeChargeSocText = ""
                        homeChargeOdoText = ""
                        homeChargeMeterText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BatteryGreen),
                    modifier = Modifier.testTag("confirm_quick_home_charge_button")
                ) {
                    Text(if (strings.isEn) "Start" else "Запустить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showHomeChargeDialog = false
                    homeChargeInitialized = false
                    homeChargeSocText = ""
                    homeChargeOdoText = ""
                    homeChargeMeterText = ""
                }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Dialog when Home Charging is not yet configured by the user
    if (showHomeNotConfiguredDialog) {
        AlertDialog(
            onDismissRequest = { /* user explicitly dismisses via buttons */ },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            ),
            modifier = if (isLightHomeCharge) Modifier.border(1.dp, Color.Black, RoundedCornerShape(26.dp)) else Modifier,
            shape = RoundedCornerShape(26.dp),
            tonalElevation = 0.dp,
            containerColor = if (isLightHomeCharge) Color.White else MaterialTheme.colorScheme.surface,
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
        EditChargingSessionDialog(
            session = s,
            currency = currency,
            onDismiss = { editingSessionId = null },
            onSave = { updated ->
                onUpdateSession?.invoke(updated)
                editingSessionId = null
            },
            onDelete = { toDelete ->
                editingSessionId = null
                sessionToDeleteId = toDelete.id
            },
            onShare = { toShare ->
                editingSessionId = null
                sessionToShareId = toShare.id
            }
        )
    }

    // Share Session Card Dialog from Home Screen
    if (sessionToShare != null) {
        ShareSessionCardDialog(
            session = sessionToShare!!,
            car = activeCar,
            currency = currency,
            onDismiss = { sessionToShareId = null }
        )
    }

    // Confirm Delete Dialog from Home Screen
    if (sessionToDelete != null) {
        val s = sessionToDelete!!
        AlertDialog(
            onDismissRequest = { sessionToDeleteId = null },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            ),
            modifier = if (isLightHomeCharge) Modifier.border(1.dp, Color.Black, RoundedCornerShape(26.dp)) else Modifier,
            shape = RoundedCornerShape(26.dp),
            tonalElevation = 0.dp,
            containerColor = if (isLightHomeCharge) Color.White else MaterialTheme.colorScheme.surface,
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить эту зарядку?") },
            text = {
                Text("Зарядка на ${s.operatorName.ifEmpty { "EV Station" }} (+${String.format(Locale.US, "%.1f", s.kwhDeliveredByStation)} кВт·ч, ${formatCurrency(s.totalCost, currency)}) будет удалена навсегда.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSession?.invoke(s)
                        sessionToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDeleteId = null }) {
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
            defaultOdometer = latestEffectiveOdometer,
            topCategories = topExpenseCategories,
            currency = currency,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { cat, amt, odo, comm, curr ->
                onAddExpense?.invoke(cat, amt, odo, comm, curr)
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
    onEditClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDc = session.stationType.equals("DC", ignoreCase = true)
    val isHome = session.operatorName.contains("Дом", ignoreCase = true)
    val badgeColor = when {
        isHome -> BatteryGreen
        isDc -> Color(0xFFC084FC) // Purple
        else -> Color(0xFF38BDF8) // Light Blue AC
    }

    val isLight = LocalCurrentTheme.current == "light"

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
                            .background(badgeColor.copy(alpha = if (isLight) 0.15f else 0.2f))
                            .then(
                                if (isLight) Modifier else Modifier.border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                            )
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
                val sessionCurr = session.currency.trim().uppercase().ifBlank { currency }
                val isForeign = !sessionCurr.equals(currency, ignoreCase = true)

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(session.totalCost, sessionCurr),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ElectricCyan
                    )
                    if (isForeign) {
                        val converted = com.example.util.CurrencyConverter.convert(session.totalCost, sessionCurr, currency)
                        Text(
                            text = "≈ ${String.format(Locale.US, "%.2f", converted)} $currency",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SoftBlue
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    Text(
                        text = "💬 ${session.operatorComment}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons Row: Postcard & Edit (Tight and clean without extra bottom padding)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onShareClick?.invoke() ?: onClick?.invoke() },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Открытка",
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Открытка", fontSize = 11.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                TextButton(
                    onClick = { onEditClick?.invoke() ?: onClick?.invoke() },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Редактировать", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
