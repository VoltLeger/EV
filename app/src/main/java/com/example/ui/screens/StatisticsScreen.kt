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
import com.example.data.model.CarExpense
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
import com.example.ui.components.getCategoryEmoji
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
    carExpenses: List<CarExpense> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current

    // Navigation Tabs: 0 = Overview, 1 = Expenses, 2 = Operators, 3 = Top-3 & Efficiency
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
    val completedSessions = remember(allSessions, carId) {
        allSessions.filter {
            (carId == 0L || it.carId == carId) && it.status == "completed"
        }.sortedBy { it.startTime }
    }

    val currentPeriodSessions = remember(completedSessions, periodStartMillis) {
        completedSessions.filter { it.startTime >= periodStartMillis }
    }

    // Car Expenses in current period
    val currentPeriodExpenses = remember(carExpenses, carId, periodStartMillis) {
        carExpenses.filter {
            (carId == 0L || it.carId == carId) && it.timestamp >= periodStartMillis
        }
    }
    val totalExpensesCost = remember(currentPeriodExpenses) { currentPeriodExpenses.sumOf { it.amount } }

    // Prior period comparison
    val periodDuration = remember(periodStartMillis, now) { now - periodStartMillis }
    val priorPeriodSessions = remember(completedSessions, periodStartMillis, periodDuration) {
        completedSessions.filter {
            it.startTime in (periodStartMillis - periodDuration) until periodStartMillis
        }
    }

    // Totals
    val totalDeliveredKwh = remember(currentPeriodSessions) { currentPeriodSessions.sumOf { it.kwhDeliveredByStation } }
    val totalCost = remember(currentPeriodSessions) { currentPeriodSessions.sumOf { it.totalCost } }
    val totalEnergyCost = remember(currentPeriodSessions) { currentPeriodSessions.sumOf { it.energyCost } }
    val totalPenaltyCost = remember(currentPeriodSessions) { currentPeriodSessions.sumOf { it.penaltyCost } }

    // Total Car Spending (Charging + Maintenance & Other Expenses)
    val totalCarSpend = totalCost + totalExpensesCost

    // Distance in period
    val minOdo = remember(currentPeriodSessions) { currentPeriodSessions.minOfOrNull { it.startOdometer } ?: 0.0 }
    val maxOdo = remember(currentPeriodSessions) { currentPeriodSessions.maxOfOrNull { it.startOdometer } ?: 0.0 }
    val periodDistanceKm = (maxOdo - minOdo).coerceAtLeast(0.0)

    val costPerKm = if (periodDistanceKm > 0) totalCost / periodDistanceKm else 0.0
    val costPer100Km = costPerKm * 100.0

    // Total ownership cost per KM (includes charging + maintenance/insurance, but NOT counted in energy consumption per 100km!)
    val totalCostPerKm = if (periodDistanceKm > 0) totalCarSpend / periodDistanceKm else 0.0
    val totalCostPer100Km = totalCostPerKm * 100.0

    // Prior period total spent comparison
    val priorTotalCost = remember(priorPeriodSessions) { priorPeriodSessions.sumOf { it.totalCost } }
    val spentDiffPercent = if (priorTotalCost > 0) {
        ((totalCost - priorTotalCost) / priorTotalCost) * 100.0
    } else 0.0

    // Free charges savings
    val freeSessions = remember(currentPeriodSessions) { currentPeriodSessions.filter { it.isFreeCharge || it.pricePerKwh <= 0.0001 } }
    val freeChargesCount = freeSessions.size
    val moneySaved = remember(freeSessions) { freeSessions.sumOf { it.kwhDeliveredByStation * 0.73 } }

    // AC vs DC breakdown
    val acSessions = remember(currentPeriodSessions) { currentPeriodSessions.filter { it.stationType.equals("AC", ignoreCase = true) } }
    val dcSessions = remember(currentPeriodSessions) { currentPeriodSessions.filter { it.stationType.equals("DC", ignoreCase = true) } }
    val acCost = remember(acSessions) { acSessions.sumOf { it.totalCost } }
    val dcCost = remember(dcSessions) { dcSessions.sumOf { it.totalCost } }

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

                    // Tab Navigation Row (Overview, Expenses, Operators, Top-3 & Efficiency)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)
                        ) {
                            Text(strings.tabOverview, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)
                        ) {
                            Text("Затраты", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        SegmentedButton(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)
                        ) {
                            Text(strings.tabOperators, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        SegmentedButton(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)
                        ) {
                            Text(strings.tabTopEfficiency, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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

                        if (totalExpensesCost > 0.0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            VoltCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = SoftBlue.copy(alpha = 0.4f)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ВСЕ РАСХОДЫ НА АВТО ЗА ПЕРИОД",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SoftBlue,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            text = formatCurrency(totalCarSpend, currency),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Зарядка: ${formatCurrency(totalCost, currency)} • Прочие: ${formatCurrency(totalExpensesCost, currency)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (periodDistanceKm > 0) {
                                            Text(
                                                text = "${String.format(Locale.US, "%.2f", totalCostPerKm)} $currency/км",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = BatteryGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================== TAB 1: EXPENSES & CHARTS ==================
            if (selectedTab == 1) {
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
                                label = { Text(periodName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }
                }

                // 1. VISUAL DIAGRAM FIRST
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        VoltCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Диаграмма затрат на автомобиль",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Наглядная визуализация всех статей расходов",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                val washCost = currentPeriodExpenses.filter { it.category.contains("Мойка", ignoreCase = true) }.sumOf { it.amount }
                                val maintenanceCost = currentPeriodExpenses.filter { it.category.contains("ТО", ignoreCase = true) || it.category.contains("Ремонт", ignoreCase = true) }.sumOf { it.amount }
                                val insuranceCost = currentPeriodExpenses.filter { it.category.contains("Страховк", ignoreCase = true) }.sumOf { it.amount }
                                val tiresCost = currentPeriodExpenses.filter { it.category.contains("Шин", ignoreCase = true) }.sumOf { it.amount }
                                val parkingCost = currentPeriodExpenses.filter { it.category.contains("Парковк", ignoreCase = true) }.sumOf { it.amount }
                                val otherCost = currentPeriodExpenses.filter {
                                    !it.category.contains("Мойка", ignoreCase = true) &&
                                    !it.category.contains("ТО", ignoreCase = true) &&
                                    !it.category.contains("Ремонт", ignoreCase = true) &&
                                    !it.category.contains("Страховк", ignoreCase = true) &&
                                    !it.category.contains("Шин", ignoreCase = true) &&
                                    !it.category.contains("Парковк", ignoreCase = true)
                                }.sumOf { it.amount }

                                val slices = mutableListOf<DonutSlice>()
                                if (totalCost > 0.0) slices.add(DonutSlice("⚡ Зарядка", totalCost.toFloat(), ElectricCyan))
                                if (washCost > 0.0) slices.add(DonutSlice("🧼 Мойка", washCost.toFloat(), Color(0xFF38BDF8)))
                                if (maintenanceCost > 0.0) slices.add(DonutSlice("🔧 ТО / Сервис", maintenanceCost.toFloat(), Color(0xFFF59E0B)))
                                if (insuranceCost > 0.0) slices.add(DonutSlice("🛡️ Страховка", insuranceCost.toFloat(), Color(0xFF10B981)))
                                if (tiresCost > 0.0) slices.add(DonutSlice("🛞 Шиномонтаж", tiresCost.toFloat(), Color(0xFFA855F7)))
                                if (parkingCost > 0.0) slices.add(DonutSlice("🅿️ Парковка", parkingCost.toFloat(), Color(0xFFEC4899)))
                                if (otherCost > 0.0) slices.add(DonutSlice("💼 Прочее", otherCost.toFloat(), Color(0xFF94A3B8)))

                                if (slices.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "За выбранный период нет данных о расходах",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    DonutBreakdownChart(
                                        slices = slices,
                                        centerValue = formatCurrency(totalCarSpend, currency),
                                        centerTitle = "Всего затрат"
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. DRY NUMBERS & KEY METRICS SECOND
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Точные показатели и стоимость владения",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Metric Row 1: Total Spend & Total Cost per KM
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                title = "Все расходы на авто",
                                value = formatCurrency(totalCarSpend, currency),
                                subtitle = "Зарядка + ТО, мойка и т.д.",
                                accentColor = ElectricCyan,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Общая цена 1 км",
                                value = if (periodDistanceKm > 0) "${String.format(Locale.US, "%.2f", totalCostPerKm)} $currency" else "—",
                                subtitle = if (periodDistanceKm > 0) "Все расходы / ${periodDistanceKm.toInt()} км" else "Нет данных о пробеге",
                                accentColor = SoftBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metric Row 2: 1 km only charging & EV Energy Consumption (strictly excluding other costs)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                title = "1 км только зарядка",
                                value = if (periodDistanceKm > 0) "${String.format(Locale.US, "%.2f", costPerKm)} $currency" else "—",
                                subtitle = "Только электричество",
                                accentColor = BatteryGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Расход энергии (EV)",
                                value = if (periodConsumption != null) "${String.format(Locale.US, "%.1f", periodConsumption)} кВт·ч" else "—",
                                subtitle = "на 100 км (без ТО/страховки)",
                                accentColor = ElectricCyan,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Detailed Category Breakdown Table
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        VoltCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Детализация по статьям затрат",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val categoriesMap = currentPeriodExpenses.groupBy { it.category }

                                // Row for Charging
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚡", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Зарядка электромобиля", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${currentPeriodSessions.size} сессий", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(formatCurrency(totalCost, currency), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                                        val pct = if (totalCarSpend > 0) (totalCost / totalCarSpend * 100) else 0.0
                                        Text("${String.format(Locale.US, "%.1f", pct)}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                categoriesMap.forEach { (cat, items) ->
                                    val catSum = items.sumOf { it.amount }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(getCategoryEmoji(cat), fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(cat, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                Text("${items.size} записей", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(formatCurrency(catSum, currency), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            val pct = if (totalCarSpend > 0) (catSum / totalCarSpend * 100) else 0.0
                                            Text("${String.format(Locale.US, "%.1f", pct)}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================== TAB 2: OPERATORS GROUPED ==================
            if (selectedTab == 2) {
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

            // ================== TAB 3: TOP-3 & EFFICIENCY ==================
            if (selectedTab == 3) {
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
