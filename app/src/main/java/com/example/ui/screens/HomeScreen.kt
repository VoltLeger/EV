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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Car
import com.example.data.model.ChargingSession
import com.example.data.model.UserProfile
import com.example.ui.components.ActiveChargingCard
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
    onQuickHomeCharge: ((Double) -> Unit)? = null,
    onCompleteChargeClick: (ChargingSession) -> Unit,
    onCancelActiveCharge: (ChargingSession) -> Unit,
    userProfile: UserProfile? = null,
    onOpenProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    var carMenuExpanded by remember { mutableStateOf(false) }
    var showHomeChargeDialog by remember { mutableStateOf(false) }
    var homeChargeTargetSoc by remember { mutableFloatStateOf(100f) }

    // Driving style and interactive battery forecast state
    val usableCapacity = activeCar?.usableCapacityKwh ?: 57.0
    val carSoc = activeCar?.currentSoc?.toFloat() ?: 80f

    // Driving style: 0 = "Мой стиль", 1 = "Город/Эко", 2 = "Трасса", 3 = "Зима"
    var selectedStyleIndex by remember { mutableIntStateOf(0) }
    var sliderSoc by remember { mutableFloatStateOf(carSoc) }

    // Sync slider with carSoc when car changes
    LaunchedEffect(activeCar?.id, carSoc) {
        sliderSoc = carSoc
    }

    // Determine baseline consumption
    val realAvg = monthConsumption ?: run {
        val completed = recentSessions.filter { it.status == "completed" }
        val forecast = EVCalculator.calculateRangeForecast(completed, usableCapacity, carSoc.toDouble())
        forecast.realConsumptionPer100Km
    }

    val selectedConsumption = when (selectedStyleIndex) {
        0 -> realAvg
        1 -> 14.0
        2 -> 19.5
        3 -> 23.0
        else -> realAvg
    }

    // Calculated range for interactive slider
    val interactiveRange = EVCalculator.calculateRangeForSoc(
        usableCapacityKwh = usableCapacity,
        socPercent = sliderSoc.toDouble(),
        consumptionPer100Km = selectedConsumption
    )

    // Range with current car SOC for vehicle top bar pill
    val currentSocRange = EVCalculator.calculateRangeForSoc(
        usableCapacityKwh = usableCapacity,
        socPercent = carSoc.toDouble(),
        consumptionPer100Km = realAvg
    )

    // Range after departure from active charge
    val departureForecastRange = if (activeSession != null) {
        val targetSoc = if (activeSession.endSoc > carSoc) activeSession.endSoc else 90.0
        EVCalculator.calculateRangeForSoc(
            usableCapacityKwh = usableCapacity,
            socPercent = targetSoc,
            consumptionPer100Km = realAvg
        )
    } else null

    // Compute widget metrics (last session, month consumption, cost per 100 km)
    val completedSessions = recentSessions.filter { it.status == "completed" }
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
            // Vehicle header with predicted range
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
                        // Vehicle selector
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
                                .clickable { if (allCars.size > 1) carMenuExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SoftBlue.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = SoftBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeCar?.name ?: strings.currentVehicle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (allCars.size > 1) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Switch Car",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${activeCar?.initialOdometer?.toInt() ?: 0} км",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Top Right: Estimated Range pill + Pilot Avatar
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
                                    size = 42.dp,
                                    onClick = onOpenProfile
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = carMenuExpanded,
                        onDismissRequest = { carMenuExpanded = false }
                    ) {
                        allCars.forEach { car ->
                            DropdownMenuItem(
                                text = { Text(car.name + if (car.id == activeCar?.id) " (✓)" else "") },
                                onClick = {
                                    onSelectCar(car.id)
                                    carMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Enlarged Centerpiece: Range Forecast with Interactive Battery Slider
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VoltCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("interactive_range_card"),
                        borderColor = ElectricCyan.copy(alpha = 0.45f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 12.dp)
                        ) {
                            // Top row: Title and current consumption badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (activeSession != null) strings.forecastAfterCharge else strings.estimatedRangeTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SoftBlue.copy(alpha = 0.18f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", selectedConsumption)} ${strings.kwhPer100Km}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftBlue
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Glowing Large Forecast Number
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${interactiveRange.toInt()}",
                                    fontSize = 54.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ElectricCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "км",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }

                            val availableKwh = usableCapacity * (sliderSoc / 100.0)
                            Text(
                                text = "При ${sliderSoc.toInt()}% батареи (~${String.format(Locale.US, "%.1f", availableKwh)} кВт·ч)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Interactive Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "5%",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = sliderSoc,
                                    onValueChange = { sliderSoc = it },
                                    valueRange = 5f..100f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                        .testTag("battery_soc_slider"),
                                    colors = SliderDefaults.colors(
                                        thumbColor = ElectricCyan,
                                        activeTrackColor = ElectricCyan,
                                        inactiveTrackColor = ElectricCyan.copy(alpha = 0.2f)
                                    )
                                )
                                Text(
                                    text = "100%",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Driving Style Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val styles = listOf(
                                    strings.drivingStyleMy,
                                    strings.drivingStyleCity,
                                    strings.drivingStyleHighway,
                                    strings.drivingStyleWinter
                                )
                                styles.forEachIndexed { index, styleName ->
                                    val isSelected = selectedStyleIndex == index
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedStyleIndex = index },
                                        label = {
                                            Text(
                                                text = styleName,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                            selectedLabelColor = ElectricCyan,
                                            containerColor = Color.Transparent,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.12f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // High-Impact Home Screen Widget: (Last charge / Month consumption / Cost per 100 km)
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
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Last Charge
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = strings.lastChargeWidget,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (lastSession != null && lastSession.kwhDeliveredByStation > 0) {
                                        "+${String.format(Locale.US, "%.1f", lastSession.kwhDeliveredByStation)}"
                                    } else "—",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (lastSession != null && lastSession.totalCost > 0) {
                                        "${String.format(Locale.US, "%.2f", lastSession.totalCost)} $currency"
                                    } else "кВт·ч",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(Color.White.copy(alpha = 0.12f))
                            )

                            // 2. Month Avg Consumption
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = SoftBlue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = strings.monthConsumptionWidget,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (monthConsumption != null && monthConsumption > 0.0) {
                                        String.format(Locale.US, "%.1f", monthConsumption)
                                    } else "—",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.kwhPer100Km,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(Color.White.copy(alpha = 0.12f))
                            )

                            // 3. Cost per 100 km
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timeline,
                                        contentDescription = null,
                                        tint = BatteryGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = strings.costPer100KmWidget,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (costPer100Km != null && costPer100Km > 0) {
                                        String.format(Locale.US, "%.2f", costPer100Km)
                                    } else "—",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BatteryGreen
                                )
                                Text(
                                    text = "$currency / 100 км",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Floating Active Charging Card (appears if session is active)
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

            // Action Buttons: Primary "Add Charge" & Quick "Home Charge (1 Tap)"
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Button 1: "Добавить зарядку"
                        Button(
                            onClick = onAddChargeClick,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(ElectricCyan, SoftBlue)
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f))
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .testTag("add_charge_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.addCharge,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Button 2: "Домашняя (1 тап)"
                        OutlinedButton(
                            onClick = {
                                if (onQuickHomeCharge != null) {
                                    showHomeChargeDialog = true
                                } else {
                                    onAddChargeClick()
                                }
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            BatteryGreen.copy(alpha = 0.5f),
                                            Color.White.copy(alpha = 0.08f)
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = strings.homeChargeQuick,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BatteryGreen
                                )
                                Text(
                                    text = strings.homeChargeSubtitle,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Button: "Рассчитать пробег"
                    OutlinedButton(
                        onClick = onCalculateRangeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        ElectricCyan.copy(alpha = 0.3f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("calculate_range_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = ElectricCyan
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.calculateRange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Recent Sessions Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp)
                ) {
                    Text(
                        text = strings.recentCharges,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (recentSessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
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
                items(recentSessions.take(5)) { session ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                        RecentSessionCard(session = session, currency = currency)
                    }
                }
            }
        }
    }

    // Quick Home Charge Dialog (1-Tap confirmation)
    if (showHomeChargeDialog) {
        val neededKwh = (usableCapacity * ((homeChargeTargetSoc - carSoc).coerceAtLeast(0f) / 100.0))
        val estPrice = 0.25 // Standard home rate
        val estCost = neededKwh * estPrice

        AlertDialog(
            onDismissRequest = { showHomeChargeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = BatteryGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Быстрая домашняя зарядка",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Автоматический расчёт для ${activeCar?.name ?: "автомобиля"} от ${carSoc.toInt()}% до ${homeChargeTargetSoc.toInt()}%:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    VoltCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Потребуется энергии:", fontSize = 13.sp)
                                Text(
                                    text = "+${String.format(Locale.US, "%.1f", neededKwh)} кВт·ч",
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Домашний тариф:", fontSize = 13.sp)
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", estPrice)} $currency/кВт·ч",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ориентир. стоимость:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "~${String.format(Locale.US, "%.2f", estCost)} $currency",
                                    fontWeight = FontWeight.Bold,
                                    color = BatteryGreen
                                )
                            }
                        }
                    }

                    Text(
                        text = "Целевой заряд: ${homeChargeTargetSoc.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = homeChargeTargetSoc,
                        onValueChange = { homeChargeTargetSoc = it },
                        valueRange = (carSoc.coerceAtLeast(10f))..100f,
                        steps = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onQuickHomeCharge?.invoke(homeChargeTargetSoc.toDouble())
                        showHomeChargeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BatteryGreen)
                ) {
                    Text("Запустить (1 тап)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHomeChargeDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun RecentSessionCard(
    session: ChargingSession,
    currency: String,
    modifier: Modifier = Modifier
) {
    VoltCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StationTypeBadge(type = session.stationType)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = session.operatorName.ifEmpty { "Станция зарядки" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = formatCurrency(session.totalCost, currency),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ElectricCyan
                )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatDate(session.startTime),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!session.operatorComment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                TagBadge(name = session.operatorComment, color = 0xFF38BDF8)
            }
        }
    }
}
