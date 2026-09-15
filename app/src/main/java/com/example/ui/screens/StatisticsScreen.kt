package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Car
import com.example.data.model.ChargingSession
import com.example.ui.components.BarChartItem
import com.example.ui.components.ChartPoint
import com.example.ui.components.ConsumptionLineChart
import com.example.ui.components.DonutBreakdownChart
import com.example.ui.components.DonutSlice
import com.example.ui.components.ExpensesBarChart
import com.example.ui.components.MetricCard
import com.example.ui.components.VoltCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatShortDate
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.BatteryOrange
import com.example.ui.theme.BatteryRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalCurrency
import com.example.ui.theme.SoftBlue
import java.util.Calendar
import java.util.Locale

enum class StatsPeriod {
    DAY, WEEK, MONTH, THREE_MONTHS, SIX_MONTHS, YEAR
}

@Composable
fun StatisticsScreen(
    activeCar: Car?,
    allSessions: List<ChargingSession>,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current

    var selectedPeriod by remember { mutableStateOf(StatsPeriod.MONTH) }
    var tripDistanceText by remember { mutableStateOf("300") }

    val now = System.currentTimeMillis()
    val periodStartMillis = remember(selectedPeriod) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            StatsPeriod.DAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.timeInMillis
            }
            StatsPeriod.WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            StatsPeriod.MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.timeInMillis
            }
            StatsPeriod.THREE_MONTHS -> {
                cal.add(Calendar.MONTH, -3)
                cal.timeInMillis
            }
            StatsPeriod.SIX_MONTHS -> {
                cal.add(Calendar.MONTH, -6)
                cal.timeInMillis
            }
            StatsPeriod.YEAR -> {
                cal.add(Calendar.YEAR, -1)
                cal.timeInMillis
            }
        }
    }

    val carId = activeCar?.id ?: 0L
    val completedSessions = allSessions.filter {
        (carId == 0L || it.carId == carId) && it.status == "completed"
    }.sortedBy { it.startTime }

    val currentPeriodSessions = completedSessions.filter { it.startTime >= periodStartMillis }

    // Prior period comparison
    val periodDuration = now - periodStartMillis
    val priorPeriodSessions = completedSessions.filter {
        it.startTime in (periodStartMillis - periodDuration) until periodStartMillis
    }

    // Totals
    val totalDeliveredKwh = currentPeriodSessions.sumOf { it.kwhDeliveredByStation }
    val totalCost = currentPeriodSessions.sumOf { it.totalCost }
    val totalEnergyCost = currentPeriodSessions.sumOf { it.energyCost }
    val totalPenaltyCost = currentPeriodSessions.sumOf { it.penaltyCost }
    val penaltySharePercent = if (totalCost > 0) (totalPenaltyCost / totalCost) * 100.0 else 0.0

    // Odometer & distance in period
    val minOdo = currentPeriodSessions.minOfOrNull { it.startOdometer } ?: 0.0
    val maxOdo = currentPeriodSessions.maxOfOrNull { it.startOdometer } ?: 0.0
    val periodDistanceKm = (maxOdo - minOdo).coerceAtLeast(0.0)

    val costPerKm = if (periodDistanceKm > 0) totalCost / periodDistanceKm else 0.0
    val costPer100Km = costPerKm * 100.0

    // Prior period total spent comparison
    val priorTotalCost = priorPeriodSessions.sumOf { it.totalCost }
    val spentDiffPercent = if (priorTotalCost > 0) {
        ((totalCost - priorTotalCost) / priorTotalCost) * 100.0
    } else 0.0

    // Free charges savings
    val freeSessions = currentPeriodSessions.filter { it.isFreeCharge || it.pricePerKwh <= 0.0001 }
    val freeChargesCount = freeSessions.size
    val moneySaved = freeSessions.sumOf { it.kwhDeliveredByStation * 0.73 } // based on typical DC price

    // Losses calculation
    val sessionsWithLosses = currentPeriodSessions.filter {
        it.kwhReceivedByCar != null && it.kwhDeliveredByStation > (it.kwhReceivedByCar ?: 0.0)
    }
    val totalLossKwh = sessionsWithLosses.sumOf { it.kwhDeliveredByStation - (it.kwhReceivedByCar ?: 0.0) }
    val totalDeliveredForLosses = sessionsWithLosses.sumOf { it.kwhDeliveredByStation }
    val lossPercent = if (totalDeliveredForLosses > 0) (totalLossKwh / totalDeliveredForLosses) * 100.0 else 0.0
    val overpaymentLosses = sessionsWithLosses.sumOf {
        val diff = it.kwhDeliveredByStation - (it.kwhReceivedByCar ?: 0.0)
        diff * it.pricePerKwh
    }

    // AC vs DC
    val acSpent = currentPeriodSessions.filter { it.stationType.equals("AC", true) }.sumOf { it.totalCost }
    val dcSpent = currentPeriodSessions.filter { it.stationType.equals("DC", true) }.sumOf { it.totalCost }

    // Operator breakdown
    val operatorTotals = currentPeriodSessions.groupBy { it.operatorName.ifBlank { "Other" } }
        .mapValues { entry -> entry.value.sumOf { it.totalCost } }

    // Penalty by operator
    val operatorPenalties = currentPeriodSessions.filter { it.penaltyCost > 0 }
        .groupBy { it.operatorName.ifBlank { "Other" } }
        .mapValues { entry -> entry.value.sumOf { it.penaltyCost } }
    val mostPenalizedOperator = operatorPenalties.maxByOrNull { it.value }

    // Trip cost calculator
    val lastSession = completedSessions.lastOrNull()
    val tripDistanceVal = tripDistanceText.toDoubleOrNull() ?: 100.0
    val lastPrice = lastSession?.pricePerKwh ?: 0.73
    val avgConsumptionForTrip = if (periodDistanceKm > 20 && totalDeliveredKwh > 0) {
        (totalDeliveredKwh / periodDistanceKm) * 100.0
    } else 18.5
    val estimatedTripCost = (tripDistanceVal / 100.0) * avgConsumptionForTrip * lastPrice

    // Chart points: consumption over sessions
    val consumptionPoints = currentPeriodSessions.mapIndexed { i, s ->
        val kwh100 = if (i > 0) {
            val prev = currentPeriodSessions[i - 1]
            val dist = s.startOdometer - prev.startOdometer
            if (dist > 5) (s.kwhDeliveredByStation / dist) * 100f else 18f
        } else 18f
        ChartPoint(label = formatShortDate(s.startTime), value = kwh100.toFloat())
    }

    // Chart items: expenses bar
    val barChartItems = currentPeriodSessions.takeLast(7).map { s ->
        BarChartItem(
            label = formatShortDate(s.startTime),
            energyCost = s.energyCost.toFloat(),
            penaltyCost = s.penaltyCost.toFloat()
        )
    }

    // Donut chart slices: AC vs DC
    val stationTypeSlices = listOf(
        DonutSlice("AC", acSpent.toFloat(), BatteryGreen),
        DonutSlice("DC", dcSpent.toFloat(), ElectricCyan)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period selector chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StatsPeriod.values()) { period ->
                    val label = when (period) {
                        StatsPeriod.DAY -> strings.day
                        StatsPeriod.WEEK -> strings.week
                        StatsPeriod.MONTH -> strings.month
                        StatsPeriod.THREE_MONTHS -> strings.threeMonths
                        StatsPeriod.SIX_MONTHS -> strings.sixMonths
                        StatsPeriod.YEAR -> strings.year
                    }
                    val isSelected = selectedPeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPeriod = period },
                        label = { Text(label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftBlue,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Period vs Previous Period comparison badge
        if (priorTotalCost > 0) {
            item {
                VoltCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (spentDiffPercent > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (spentDiffPercent > 0) BatteryOrange else BatteryGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.periodComparison,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val sign = if (spentDiffPercent > 0) "+" else ""
                        Text(
                            text = "$sign${String.format(Locale.getDefault(), "%.1f%%", spentDiffPercent)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (spentDiffPercent > 0) BatteryOrange else BatteryGreen
                        )
                    }
                }
            }
        }

        // Metric grid: Total Spent & Total Delivered
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = strings.totalSpent,
                    value = String.format(Locale.getDefault(), "%.2f", totalCost),
                    unit = currency,
                    subtitle = "Энергия: ${String.format(Locale.getDefault(), "%.2f", totalEnergyCost)}",
                    modifier = Modifier.weight(1f),
                    accentColor = SoftBlue
                )
                MetricCard(
                    title = strings.totalDelivered,
                    value = String.format(Locale.getDefault(), "%.1f", totalDeliveredKwh),
                    unit = "кВт·ч",
                    subtitle = "${currentPeriodSessions.size} сессий",
                    modifier = Modifier.weight(1f),
                    accentColor = ElectricCyan
                )
            }
        }

        // Metric grid: Cost per 1 km & Cost per 100 km
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = strings.pricePer1Km,
                    value = String.format(Locale.getDefault(), "%.2f", costPerKm),
                    unit = "$currency/км",
                    modifier = Modifier.weight(1f),
                    accentColor = BatteryGreen
                )
                MetricCard(
                    title = strings.pricePer100Km,
                    value = String.format(Locale.getDefault(), "%.2f", costPer100Km),
                    unit = "$currency/100км",
                    modifier = Modifier.weight(1f),
                    accentColor = BatteryGreen
                )
            }
        }

        // Chart 1: Consumption over time (Line)
        item {
            VoltCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Расход энергии во времени (кВт·ч/100 км)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                ConsumptionLineChart(
                    points = consumptionPoints,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Chart 2: Charging expenses over time (Bar)
        item {
            VoltCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Затраты на зарядку ($currency)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(SoftBlue))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Энергия", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(BatteryOrange))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Штрафы", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                ExpensesBarChart(
                    items = barChartItems,
                    currency = currency,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Penalties Analysis Card
        item {
            VoltCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (totalPenaltyCost > 0) BatteryOrange.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.penaltiesOnly,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(totalPenaltyCost, currency),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalPenaltyCost > 0) BatteryOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.penaltiesShare,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", penaltySharePercent),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (penaltySharePercent > 10.0) BatteryRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (mostPenalizedOperator != null && mostPenalizedOperator.value > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Самый штрафной оператор: ${mostPenalizedOperator.key} (${formatCurrency(mostPenalizedOperator.value, currency)})",
                        fontSize = 12.sp,
                        color = BatteryOrange
                    )
                }
            }
        }

        // Free Charges Card
        if (freeChargesCount > 0) {
            item {
                VoltCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BatteryGreen.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = BatteryGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${strings.freeCharges}: $freeChargesCount",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Бонусные или бесплатные киловатты",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = strings.moneySaved,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(moneySaved, currency),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BatteryGreen
                            )
                        }
                    }
                }
            }
        }

        // Losses and Overpayment
        if (lossPercent > 0.0) {
            item {
                VoltCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Потери и переплата при зарядке",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Средние потери", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", lossPercent),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = BatteryOrange
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Сумма переплаты", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatCurrency(overpaymentLosses, currency),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Breakdown AC vs DC (Donut)
        item {
            VoltCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = strings.stationBreakdown,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                DonutBreakdownChart(
                    slices = stationTypeSlices,
                    centerTitle = "Всего",
                    centerValue = formatCurrency(totalCost, currency),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Operators Breakdown
        if (operatorTotals.isNotEmpty()) {
            item {
                VoltCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.operatorBreakdown,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    operatorTotals.entries.sortedByDescending { it.value }.forEach { (name, sum) ->
                        val pct = if (totalCost > 0) (sum / totalCost) * 100.0 else 0.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "${formatCurrency(sum, currency)} (${String.format(Locale.getDefault(), "%.0f%%", pct)})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricCyan
                            )
                        }
                    }
                }
            }
        }

        // Trip Calculator Section
        item {
            VoltCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = SoftBlue.copy(alpha = 0.4f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = SoftBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.tripCalcTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tripDistanceText,
                    onValueChange = { tripDistanceText = it },
                    label = { Text(strings.tripDistance) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_calc_distance_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.tripCostResult,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(estimatedTripCost, currency),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElectricCyan
                    )
                }
            }
        }
    }
}
