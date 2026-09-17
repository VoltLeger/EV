package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Car
import com.example.data.model.ChargingSession
import com.example.ui.components.ChartPoint
import com.example.ui.components.ConsumptionLineChart
import com.example.ui.components.DonutBreakdownChart
import com.example.ui.components.DonutSlice
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.MetricCard
import com.example.ui.components.StationTypeBadge
import com.example.ui.components.TagBadge
import com.example.ui.components.VoltCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatShortDate
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.BatteryOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalCurrency
import com.example.ui.theme.SoftBlue
import com.example.util.EVCalculator
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
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current

    // Navigation Tabs: 0 = Overview, 1 = Operators, 2 = Top-3 & Efficiency
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPeriod by remember { mutableStateOf(StatsPeriod.MONTH) }

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

    // Distance in period
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
    val moneySaved = freeSessions.sumOf { it.kwhDeliveredByStation * 0.73 }

    // AC vs DC breakdown
    val acSessions = currentPeriodSessions.filter { it.stationType.equals("AC", ignoreCase = true) }
    val dcSessions = currentPeriodSessions.filter { it.stationType.equals("DC", ignoreCase = true) }
    val acCost = acSessions.sumOf { it.totalCost }
    val dcCost = dcSessions.sumOf { it.totalCost }

    // Average consumption in period
    val periodConsumption = if (periodDistanceKm > 0 && totalDeliveredKwh > 0) {
        (totalDeliveredKwh / periodDistanceKm) * 100.0
    } else null

    // Passport consumption comparison
    val passportVal = activeCar?.passportConsumption ?: 16.0
    val realConsumption = periodConsumption ?: 17.5
    val (deltaKwh, deltaPercent) = EVCalculator.comparePassportConsumption(
        actualConsumption = realConsumption,
        passportConsumption = passportVal
    )

    // Month vs month comparative stats
    val monthComparison = remember(completedSessions) {
        EVCalculator.calculateMonthToMonthStats(completedSessions)
    }

    // Top-3 Stations calculations
    val topCheapest = remember(completedSessions, currency) {
        EVCalculator.getTopCheapestStations(completedSessions, currency)
    }
    val topFastest = remember(completedSessions) {
        EVCalculator.getTopFastestStations(completedSessions)
    }
    val topLeastLosses = remember(completedSessions) {
        EVCalculator.getTopLeastLossesStations(completedSessions)
    }

    // Operator grouped statistics
    val operatorStats = remember(completedSessions) {
        EVCalculator.getOperatorDetailedStats(completedSessions)
    }

    LiquidGlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Screen Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.statsNav,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Direct Export / Share Report Button
                        Button(
                            onClick = {
                                val reportText = buildString {
                                    append("⚡ Отчёт по зарядкам: ${activeCar?.name ?: "Мой EV"}\n")
                                    append("• Пробег за период: ${periodDistanceKm.toInt()} км\n")
                                    append("• Всего потрачено: ${String.format(Locale.US, "%.2f", totalCost)} $currency\n")
                                    append("• Стоимость 100 км: ${String.format(Locale.US, "%.2f", costPer100Km)} $currency\n")
                                    if (periodConsumption != null) {
                                        append("• Средний расход: ${String.format(Locale.US, "%.1f", periodConsumption)} кВт·ч/100 км (паспорт: $passportVal)\n")
                                    }
                                    append("• Зарядок: ${currentPeriodSessions.size} (Бесплатных: $freeChargesCount)\n")
                                    append("Создано в VoltLedger 🚀")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, reportText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, strings.exportStatsNow))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoftBlue.copy(alpha = 0.25f),
                                contentColor = SoftBlue
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.exportStatsNow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Navigation Row (Overview, Operators, Top-3 & Efficiency)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) {
                            Text(strings.tabOverview, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) {
                            Text(strings.tabOperators, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        SegmentedButton(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) {
                            Text(strings.tabTopEfficiency, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ================== TAB 0: OVERVIEW ==================
            if (selectedTab == 0) {
                // Period selector chips
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(StatsPeriod.values()) { period ->
                            val periodName = when (period) {
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
                                label = { Text(text = periodName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = ElectricCyan,
                                    containerColor = Color.Transparent,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }

                // 1. Passport vs Real Consumption Comparison Card
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        VoltCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (deltaPercent <= 0) BatteryGreen.copy(alpha = 0.4f) else BatteryOrange.copy(alpha = 0.4f)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.passportComparisonTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (deltaPercent <= 0) BatteryGreen.copy(alpha = 0.2f)
                                                else BatteryOrange.copy(alpha = 0.2f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (deltaPercent > 0) "+${String.format(Locale.US, "%.1f", deltaPercent)}%"
                                            else "${String.format(Locale.US, "%.1f", deltaPercent)}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (deltaPercent <= 0) BatteryGreen else BatteryOrange
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(strings.passportValue, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", passportVal)} кВт·ч",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(strings.realValue, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", realConsumption)} кВт·ч",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricCyan
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (deltaPercent > 0) "Выше нормы" else "Экономичнее",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (deltaPercent <= 0) BatteryGreen else BatteryOrange
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Month vs Month Comparison Card
                item {
                    val costDiff = monthComparison.costDiffPercent
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        VoltCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.monthVsMonth,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (costDiff != null) {
                                            if (costDiff <= 0) "Экономия ${String.format(Locale.US, "%.1f", -costDiff)}%"
                                            else "+${String.format(Locale.US, "%.1f", costDiff)}% трат"
                                        } else "—",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (costDiff != null && costDiff <= 0) BatteryGreen else BatteryOrange
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Metric 1: Spent
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Расходы", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", monthComparison.currentMonthCost)} $currency",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "пред.: ${String.format(Locale.US, "%.1f", monthComparison.prevMonthCost)}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Metric 2: Energy
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Энергия", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", monthComparison.currentMonthConsumption ?: 0.0)} кВт·ч",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "пред.: ${String.format(Locale.US, "%.1f", monthComparison.prevMonthConsumption ?: 0.0)}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Metric 3: Distance
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Пробег", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${monthComparison.currentMonthDistance.toInt()} км",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "пред.: ${monthComparison.prevMonthDistance.toInt()} км",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Grid of Key Metrics Cards
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                title = strings.totalCost,
                                value = formatCurrency(totalCost, currency),
                                subtitle = if (spentDiffPercent != 0.0) {
                                    "${if (spentDiffPercent > 0) "+" else ""}${String.format(Locale.getDefault(), "%.1f", spentDiffPercent)}% vs пред."
                                } else null,
                                accentColor = ElectricCyan,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = strings.odometer,
                                value = "${periodDistanceKm.toInt()} км",
                                subtitle = null,
                                accentColor = SoftBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                title = strings.pricePer100Km,
                                value = "${String.format(Locale.getDefault(), "%.2f", costPer100Km)} $currency",
                                subtitle = null,
                                accentColor = BatteryGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = strings.totalDelivered,
                                value = "${String.format(Locale.getDefault(), "%.1f", totalDeliveredKwh)} кВт·ч",
                                subtitle = null,
                                accentColor = SoftBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ================== TAB 1: OPERATORS GROUPED ==================
            if (selectedTab == 1) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Группировка по станциям и сетям",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Мощность, длительность сессий, суммарные расходы и объём энергии",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (operatorStats.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Нет данных по операторам. Завершите хотя бы одну зарядку.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(operatorStats) { stat ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            VoltCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Row 1: Operator Title + Cost
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(ElectricCyan)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stat.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", stat.totalCost)} $currency",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = ElectricCyan
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Row 2: Grid of Operator Telemetry
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Power
                                        Column {
                                            Text(strings.avgPowerHeader, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = stat.avgPowerKw?.let { "${String.format(Locale.US, "%.1f", it)} кВт" } ?: "—",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        // Duration
                                        Column {
                                            Text(strings.avgDurationHeader, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = stat.avgDurationMinutes?.let { "${it} мин" } ?: "—",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        // Total kWh
                                        Column {
                                            Text("Объём", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", stat.totalKwh)} кВт·ч",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        // Rate
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Ср. цена", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "${String.format(Locale.US, "%.2f", stat.avgPricePerKwh)} $currency",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = SoftBlue
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Сессий зарядки: ${stat.count}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ================== TAB 2: TOP-3 & EFFICIENCY ==================
            if (selectedTab == 2) {
                // Top-3 Cheapest Stations
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = strings.topCheapestTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (topCheapest.isEmpty()) {
                            Text("Недостаточно данных", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            VoltCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    topCheapest.forEachIndexed { index, rank ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = when (index) {
                                                        0 -> ElectricCyan
                                                        1 -> SoftBlue
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(text = rank.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                    if (rank.subtitle.isNotBlank()) {
                                                        Text(text = rank.subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = rank.formattedValue,
                                                fontWeight = FontWeight.Bold,
                                                color = BatteryGreen,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Top-3 Fastest Stations
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = strings.topFastestTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (topFastest.isEmpty()) {
                            Text("Недостаточно данных", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            VoltCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    topFastest.forEachIndexed { index, rank ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = when (index) {
                                                        0 -> ElectricCyan
                                                        1 -> SoftBlue
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(text = rank.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                    if (rank.subtitle.isNotBlank()) {
                                                        Text(text = rank.subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = rank.formattedValue,
                                                fontWeight = FontWeight.Bold,
                                                color = ElectricCyan,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Top-3 Least Losses Stations
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = strings.topLeastLossesTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (topLeastLosses.isEmpty()) {
                            Text("Недостаточно данных", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            VoltCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    topLeastLosses.forEachIndexed { index, rank ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = when (index) {
                                                        0 -> ElectricCyan
                                                        1 -> SoftBlue
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(text = rank.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                    if (rank.subtitle.isNotBlank()) {
                                                        Text(text = rank.subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = rank.formattedValue,
                                                fontWeight = FontWeight.Bold,
                                                color = SoftBlue,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // AC vs DC Donut breakdown
                if (totalCost > 0.0) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            VoltCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Распределение AC vs DC расходов",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    DonutBreakdownChart(
                                        slices = listOf(
                                            DonutSlice("AC", acCost.toFloat(), Color(0xFF38BDF8)),
                                            DonutSlice("DC", dcCost.toFloat(), Color(0xFFC084FC))
                                        ),
                                        centerValue = formatCurrency(totalCost, currency),
                                        centerTitle = strings.totalCost
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
